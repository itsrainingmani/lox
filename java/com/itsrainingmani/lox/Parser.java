package com.itsrainingmani.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.itsrainingmani.lox.TokenType.*;

/*
program        → declaration* EOF ;

declaration    → classDecl
               | funDecl
               | varDecl
               | statement;

classDecl      → "class" IDENTIFIER "{" function* "}" ;
funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | block
               | break ;

exprStmt       → expression ";" ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
ifStmt         → "if" "(" expression ")" statement
               ( "else" statement)? ;
printStmt      → "print" expression ";" ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement;
block          → "{" declaration* "}" ;
break          → "break" ;

expression     → assignment ;
assignment     → ( call "." )? IDENTIFIER "=" assignment
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality)* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | call ;
call           → primary ( ."(" arguments? ")" | "." IDENTIFIER )* ; 
arguments      → expression ( ", " expression )* ;
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER 
               | lambdaFunctions ;
*/
// A recursive descent parser is a literal translation of the grammar’s rules straight into imperative code. 
// Each grammar rules becomes a method inside the Parser class
class Parser {
  private boolean allowExpression;
  private boolean foundExpression = false;

  private final List<Token> tokens;
  private int current = 0;

  // should be a syntax error to use "break" outside a loop
  // we can do this by maintaining a field in the parser that
  // tracks how many enclosing loops there currently are
  private int loopDepth = 0;

  private static class ParseError extends RuntimeException {
  }

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Object parseRepl() {
    allowExpression = true;
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());

      if (foundExpression) {
        Stmt last = statements.get(statements.size() - 1);
        return ((Stmt.Expression) last).expression;
      }

      allowExpression = false;
    }

    return statements;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(CLASS))
        return classDeclaration();

      // Only parse a function declaration if the current token is FUN
      // and the next token is an identifier
      if (check(FUN) && checkNext(IDENTIFIER)) {
        consume(FUN, null);
        return function("function");
      }
      if (match(VAR))
        return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");
    consume(LEFT_BRACE, "Expect '{' before classs body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' before classs body.");

    return new Stmt.Class(name, methods);
  }

  private Stmt statement() {
    if (match(FOR))
      return forStatement();
    if (match(IF))
      return ifStatement();
    if (match(PRINT))
      return printStatement();
    if (match(RETURN))
      return returnStatement();
    if (match(WHILE))
      return whileStatement();
    if (match(BREAK))
      return breakStatement();
    if (match(LEFT_BRACE))
      return new Stmt.Block(block());

    return expressionStatement();
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    // it is possible in the grammar to have a place
    // where we can have an expression and a statement
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    // we look for a semicolon to see if the clause has been omitted
    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();

    try {
      loopDepth++;
      if (increment != null) {
        body = new Stmt.Block(
            Arrays.asList(body, new Stmt.Expression(increment)));
      }

      if (condition == null)
        condition = new Expr.Literal(true);
      body = new Stmt.While(condition, body);

      if (initializer != null) {
        body = new Stmt.Block(Arrays.asList(initializer, body));
      }

      return body;
    } finally {
      loopDepth--;
    }

  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after 'if'.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after 'condition'.");

    try {
      loopDepth++;
      Stmt body = statement();

      return new Stmt.While(condition, body);
    } finally {
      loopDepth--;
    }
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt breakStatement() {
    if (loopDepth == 0) {
      error(previous(), "Must be inside a loop to use 'break'.");
    }

    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Break();
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration");
    return new Stmt.Var(name, initializer);
  }

  // Lets you place an expression where a statement is expected
  // They exist to evaluate expressions that have side effects
  // Any time you see a function or method call followed by a ';'
  private Stmt expressionStatement() {
    Expr expr = expression();

    if (allowExpression && isAtEnd()) {
      foundExpression = true;
    } else {
      consume(SEMICOLON, "Expect ';' after value.");
    }
    return new Stmt.Expression(expr);
  }

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
    return new Stmt.Function(name, functionBody(kind));
  }

  private Expr.Function functionBody(String kind) {
    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();

    return new Expr.Function(parameters, body);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      // parse the left-hand side as a normal expression
      // when we stumble onto the equal sign after it
      // we take the parsed expression and transform it into the
      // correct syntax node for assignment
      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get) expr;
        return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          // we only report an error instead of throwing an error
          // since the parser is in a valid state.
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  // Highest level of precedence
  private Expr primary() {
    // Most of the cases are single terminals
    if (match(FALSE))
      return new Expr.Literal(false);
    if (match(TRUE))
      return new Expr.Literal(true);
    if (match(NIL))
      return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression");
      return new Expr.Grouping(expr);
    }

    if (match(FUN))
      return functionBody("function");

    throw error(peek(), "Expect expression.");
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  // The check() method returns true if the current token is of the given type.
  // Unlike match(), it never consumes the token, it only looks at it.
  private boolean check(TokenType type) {
    if (isAtEnd())
      return false;
    return peek().type == type;
  }

  // Another token of lookahead
  private boolean checkNext(TokenType type) {
    if (isAtEnd() || tokens.get(current + 1).type == EOF)
      return false;
    return tokens.get(current + 1).type == type;
  }

  // The advance() method consumes the current token and returns it, similar to
  // how our scanner’s corresponding method crawled through characters.
  private Token advance() {
    if (!isAtEnd())
      current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON)
        return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private Token consume(TokenType type, String message) {
    if (check(type))
      return advance();

    throw error(peek(), message);
  }
}
