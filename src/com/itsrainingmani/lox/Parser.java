package com.itsrainingmani.lox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;

import static com.itsrainingmani.lox.TokenType.*;

/*
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
*/
// A recursive descent parser is a literal translation of the grammar’s rules straight into imperative code. 
// Each grammar rules becomes a method inside the Parser class
class Parser {
  private final List<Token> tokens;
  private int current = 0;

  // The entry point, the lookup object
  private static Lookup lookup = MethodHandles.lookup();

  // For a method that returns an Expr and accepts no arguments
  private static MethodType exprMH = MethodType.methodType(Expr.class);

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  private Expr expression() {
    return equality();
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

  private Expr factor() throws Throwable {
    return leftAssocOps("methodName", SLASH, STAR);
  }

  private Expr leftAssocOps(String methodName, TokenType... types)
      throws Throwable {
    MethodHandle handle = lookup.findVirtual(Parser.class, methodName, exprMH);
    Expr expr = (Expr) handle.invoke();

    while (match(types)) {
      Token operator = previous();
      Expr right = (Expr) handle.invoke();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
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
}
