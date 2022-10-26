#include <stdio.h>
#include <string.h>

#include "common.h"
#include "scanner.h"

typedef struct {
  const char* start;
  const char* current;
  int line;
} Scanner;

Scanner scanner;

static bool isAtEnd() {
  return *scanner.current == '\0';
}

// Consumes the current character and returns it
static char advance() {
  scanner.current++;
  return scanner.current[-1];
}

// Returns current char but doesn't consume it
static char peek() {
  return *scanner.current;
}

// Returns the next character past current but doesn't consume it
static char peekNext() {
  if (isAtEnd()) return '\0';
  return scanner.current[1];
}

// If current char is the expected one, we advance and return true
// Otherwise, we return false to indicate it wasn't matched
static bool match(char expected) {
  if (isAtEnd()) return false;
  if (*scanner.current != expected) return false;
  scanner.current++;
  return true;
}

static Token makeToken(TokenType type) {
  Token token;
  token.type = type;
  token.start = scanner.start;
  token.length = (int)(scanner.current - scanner.start);
  token.line = scanner.line;
  return token;
}

static Token errorToken(const char* message) {
  Token token;
  token.type = TOKEN_ERROR;
  token.start = message;
  token.length = (int)strlen(message);
  token.line = scanner.line;
  return token;
}

static void skipWhitespace() {
  for (;;) {
    char c = peek();
    switch (c)
    {
    case ' ':
    case '\r':
    case '\t':
      advance();
      break;
    case '\n':
      // when we consume a newline, we bump the line number
      scanner.line++;
      advance();
      break;
    case '/':
      if (peekNext() == '/') {
        // a comment goes till the end of the line
        while (peek() != '\n' && !isAtEnd()) advance();
      }
      else {
        return;
      }
      break;
    default:
      return;
    }
  }
}

void initScanner(const char* source) {
  scanner.start = source;
  scanner.current = source;
  scanner.line = 1;
}

Token scanToken() {
  skipWhitespace();
  scanner.start = scanner.current;

  if (isAtEnd()) return makeToken(TOKEN_EOF);

  char c = advance();

  switch (c) {
    // single character lexemes
  case '(': return makeToken(TOKEN_LEFT_PAREN);
  case ')': return makeToken(TOKEN_RIGHT_PAREN);
  case '{': return makeToken(TOKEN_LEFT_BRACE);
  case '}': return makeToken(TOKEN_RIGHT_BRACE);
  case ';': return makeToken(TOKEN_SEMICOLON);
  case ',': return makeToken(TOKEN_COMMA);
  case '.': return makeToken(TOKEN_DOT);
  case '-': return makeToken(TOKEN_MINUS);
  case '+': return makeToken(TOKEN_PLUS);
  case '/': return makeToken(TOKEN_SLASH);
  case '*': return makeToken(TOKEN_STAR);
    // two-character punctuation tokens
  case '!':
    return makeToken(
      match('=') ? TOKEN_BANG_EQUAL : TOKEN_BANG
    );
  case '=':
    return makeToken(
      match('=') ? TOKEN_EQUAL_EQUAL : TOKEN_EQUAL
    );
  case '<':
    return makeToken(
      match('=') ? TOKEN_LESS_EQUAL : TOKEN_LESS
    );
  case '>':
    return makeToken(
      match('=') ? TOKEN_GREATER_EQUAL : TOKEN_GREATER
    );
  }

  return errorToken("Unexpected character.");
}