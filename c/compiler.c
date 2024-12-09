#include <stdio.h>
#include <stdlib.h>

#include "chunk.h"
#include "common.h"
#include "compiler.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE
#include "debug.h"

#endif

// global variable of this struct type so we don’t need to pass the state around
// from function to function in the compiler.
typedef struct {
  Token current;
  Token previous;
  bool hadError;

  // Helps avoid error cascades
  bool panicMode;
} Parser;

// Lox Precedence Levels from lowest to highest
// C gives enums successively larger numbers for enums,
// PREC_CALL is numerically larger than PREC_UNARY
typedef enum {
  PREC_NONE,
  PREC_ASSIGNMENT, // =
  PREC_OR,         // or
  PREC_AND,        // and
  PREC_EQUALITY,   // == !=
  PREC_COMPARISON, // < > <= >=
  PREC_TERM,       // + -
  PREC_FACTOR,     // * /
  PREC_UNARY,      // ! -
  PREC_CALL,       // . ()
  PREC_PRIMARY
} Precedence;

// Function type that takes no args and returns nothing
typedef void (*ParseFn)();

// Table that given a token type lets us find
//
// 1. The fn to compile a prefix expr starting with a token of that type
// 2. The fn to compile an infix expr whose left operand is followed by a token
// of that type
// 3. The precedence of an infix expr that uses that token as an operator
typedef struct {
  ParseFn prefix;
  ParseFn infix;
  Precedence precedence;
} ParseRule;

Parser parser;
Chunk *compilingChunk;

static Chunk *currentChunk() { return compilingChunk; }

static void errorAt(Token *token, const char *message) {
  // While panic mode flag is set, we simply suppress any other errors that get
  // detected
  if (parser.panicMode)
    return;
  parser.panicMode = true;
  fprintf(stderr, "[line %d] Error", token->line);

  if (token->type == TOKEN_EOF) {
    fprintf(stderr, " at end");
  } else if (token->type == TOKEN_ERROR) {
    // Nothing.
  } else {
    fprintf(stderr, " at '%.*s'", token->length, token->start);
  }

  fprintf(stderr, ": %s\n", message);
  parser.hadError = true;
}

static void error(const char *message) { errorAt(&parser.previous, message); }

static void errorAtCurrent(const char *message) {
  errorAt(&parser.current, message);
}

static void advance() {
  parser.previous = parser.current;

  for (;;) {
    parser.current = scanToken();
    if (parser.current.type != TOKEN_ERROR)
      break;

    // The scanner doesn't report lexical errors
    // Instead, it creates special error tokens and leaves it up to the parser
    // to report them. We do that here
    errorAtCurrent(parser.current.start);
  }
}

// Similar to advance() and it reads the next token
// Also validates that the token has an expected type. If not it reports an
// error
// This is where most syntax errors are surfaced in the compiler
static void consume(TokenType type, const char *message) {
  if (parser.current.type == type) {
    advance();
    return;
  }

  errorAtCurrent(message);
}

// Append the given byte (opcode or operand) to the chunk
//
// Also, sends in the previous token's line info so that runtime errors are
// associated with that line
static void emitByte(uint8_t byte) {
  writeChunk(currentChunk(), byte, parser.previous.line);
}

// Convenience function for writing an opcode followed by a one-byte operand
static void emitBytes(uint8_t byte1, uint8_t byte2) {
  emitByte(byte1);
  emitByte(byte2);
}

static void emitReturn() {
  emitByte(OP_RETURN);
#ifdef DEBUG_PRINT_CODE

  if (!parser.hadError) {
    disassembleChunk(currentChunk(), "code");
  }

#endif
}

static uint8_t makeConstant(Value value) {
  // TODO: handle OP_CONSTANT_LONG
  int constant = addConstant(currentChunk(), value);
  if (constant > UINT8_MAX) {
    error("Too many constants in one chunk");
    return 0;
  }

  return (uint8_t)constant;
}

static void emitConstant(Value value) {
  emitBytes(OP_CONSTANT, makeConstant(value));
}

static void endCompiler() { emitReturn(); }

// Forward declarations to handle the fact that our grammar is recursive
static void expression();
static ParseRule *getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

static void binary() {
  TokenType operatorType = parser.previous.type;
  ParseRule *rule = getRule(operatorType);

  // Each binary operator's right-hand operand precedence is one level higher
  // than its own
  parsePrecedence((Precedence)(rule->precedence + 1));

  switch (operatorType) {
  case TOKEN_PLUS:
    emitByte(OP_ADD);
    break;
  case TOKEN_MINUS:
    emitByte(OP_SUBTRACT);
    break;
  case TOKEN_STAR:
    emitByte(OP_MULTIPLY);
    break;
  case TOKEN_SLASH:
    emitByte(OP_DIVIDE);
    break;
  default:
    return; // unreachable
  }
}

static void grouping() {
  // Assumes that the initial ( has already been consumed
  // Recursively call back into expression() to compile the expression between
  // the (), then parse the closing ) at the end
  //
  // As far as the back end is concerned, there’s literally nothing to a
  // grouping expression. Its sole function is syntactic—it lets you insert a
  // lower-precedence expression where a higher precedence is expected. Thus,
  // it has no runtime semantics on its own and therefore doesn’t emit any
  // bytecode. The inner call to expression() takes care of generating
  // bytecode for the expression inside the parentheses.
  expression();
  consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
}

static void number() {
  double value = strtod(parser.previous.start, NULL);
  emitConstant(value);
}

static void unary() {
  TokenType operatorType = parser.previous.type;

  // Compile the operand
  parsePrecedence(PREC_UNARY);

  // Emit the operator instruction
  switch (operatorType) {
  case TOKEN_MINUS:
    emitByte(OP_NEGATE);
    break;
  default:
    return; // unreachable
  }
}

ParseRule rules[] = {
    [TOKEN_LEFT_PAREN] = {grouping, NULL, PREC_NONE},
    [TOKEN_RIGHT_PAREN] = {NULL, NULL, PREC_NONE},
    [TOKEN_LEFT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_RIGHT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_COMMA] = {NULL, NULL, PREC_NONE},
    [TOKEN_DOT] = {NULL, NULL, PREC_NONE},
    [TOKEN_MINUS] = {unary, binary, PREC_TERM},
    [TOKEN_PLUS] = {NULL, binary, PREC_TERM},
    [TOKEN_SEMICOLON] = {NULL, NULL, PREC_NONE},
    [TOKEN_SLASH] = {NULL, binary, PREC_FACTOR},
    [TOKEN_STAR] = {NULL, binary, PREC_FACTOR},
    [TOKEN_BANG] = {NULL, NULL, PREC_NONE},
    [TOKEN_BANG_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_GREATER] = {NULL, NULL, PREC_NONE},
    [TOKEN_GREATER_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_LESS] = {NULL, NULL, PREC_NONE},
    [TOKEN_LESS_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_IDENTIFIER] = {NULL, NULL, PREC_NONE},
    [TOKEN_STRING] = {NULL, NULL, PREC_NONE},
    [TOKEN_NUMBER] = {number, NULL, PREC_NONE},
    [TOKEN_AND] = {NULL, NULL, PREC_NONE},
    [TOKEN_CLASS] = {NULL, NULL, PREC_NONE},
    [TOKEN_ELSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FALSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FOR] = {NULL, NULL, PREC_NONE},
    [TOKEN_FUN] = {NULL, NULL, PREC_NONE},
    [TOKEN_IF] = {NULL, NULL, PREC_NONE},
    [TOKEN_NIL] = {NULL, NULL, PREC_NONE},
    [TOKEN_OR] = {NULL, NULL, PREC_NONE},
    [TOKEN_PRINT] = {NULL, NULL, PREC_NONE},
    [TOKEN_RETURN] = {NULL, NULL, PREC_NONE},
    [TOKEN_SUPER] = {NULL, NULL, PREC_NONE},
    [TOKEN_THIS] = {NULL, NULL, PREC_NONE},
    [TOKEN_TRUE] = {NULL, NULL, PREC_NONE},
    [TOKEN_VAR] = {NULL, NULL, PREC_NONE},
    [TOKEN_WHILE] = {NULL, NULL, PREC_NONE},
    [TOKEN_ERROR] = {NULL, NULL, PREC_NONE},
    [TOKEN_EOF] = {NULL, NULL, PREC_NONE},
};

// Starts at the current token and parses any expression at the given
// precedence level or higher
static void parsePrecedence(Precedence precedence) {
  // Maestro that orchestrates all of the parsing functions
  /* At the beginning of parsePrecedence(), we look up a prefix parser for the
   * current token. The first token is always going to belong to some kind of
   * prefix expression, by definition. It may turn out to be nested as an
   * operand inside one or more infix expressions, but as you read the code from
   * left to right, the first token you hit always belongs to a prefix
   * expression. */

  advance();
  ParseFn prefixRule = getRule(parser.previous.type)->prefix;
  if (prefixRule == NULL) {
    error("Expect expression.");
    return;
  }

  prefixRule();

  /* After parsing that, which may consume more tokens, the prefix expression is
   * done. Now we look for an infix parser for the next token. If we find one,
   * it means the prefix expression we already compiled might be an operand for
   * it. But only if the call to parsePrecedence() has a precedence that is low
   * enough to permit that infix operator. */

  /* If the next token is too low precedence, or isn’t an infix operator at all,
   * we’re done. We’ve parsed as much expression as we can. Otherwise, we
   * consume the operator and hand off control to the infix parser we found. It
   * consumes whatever other tokens it needs (usually the right operand) and
   * returns back to parsePrecedence(). Then we loop back around and see if the
   * next token is also a valid infix operator that can take the entire
   * preceding expression as its operand. We keep looping like that, crunching
   * through infix operators and their operands until we hit a token that isn’t
   * an infix operator or is too low precedence and stop. */

  // Parsing Infix expressions
  while (precedence <= getRule(parser.current.type)->precedence) {
    advance();
    ParseFn infixRule = getRule(parser.previous.type)->infix;
    infixRule();
  }
}

static ParseRule *getRule(TokenType type) { return &rules[type]; }

static void expression() { parsePrecedence(PREC_ASSIGNMENT); }

/*
Single-pass Compilation

Generally languages two separate passes -
1. A parser produces an AST
2. Code Generator traverses the AST and outputs the target code

SInce Lox is a small, dynamically typed language, we utilize a single-pass
*/
bool compile(const char *source, Chunk *chunk) {
  initScanner(source);
  compilingChunk = chunk;

  advance();
  expression();
  consume(TOKEN_EOF, "Expect end of expression");
  endCompiler();
  return !parser.hadError;
}
