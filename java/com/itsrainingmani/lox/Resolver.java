package com.itsrainingmani.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/* After the parser produces the Syntax tree but before
the interpreter starts executing, we do a single walk
over the tree to resolve all of the variables it contains

The variable resolution is similar to a mini-interpreter.
It walks the tree visiting each node, but a static analysis 
differs from dynamic execution

- There are no side effects (print stmt doesn't do anything)
- There is no control flow (loops are visited once. both branchs
in an if stmt are visited. Logic operators are not short-circuited)
*/

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  // Declaration adds the variable to the innermost scope
  // so that it shadows any outer one and we know that the
  // variable exists
  private void declare(Token name) {
    if (scopes.isEmpty())
      return;

    // We mark the variable as "not ready yet" by binding
    // its name to false in the scope map.
    // The value associated with a key in the scope map
    // represents whether or not we have finished resolving
    // that variable's initializer
    Map<String, Boolean> scope = scopes.peek();
    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty())
      return;

    // we mark the variable as fully initialized and available for use
    scopes.peek().put(name.lexeme, true);
  }

  // We start at the innermost scope and work outwards, looking in each map
  // for a matching name. If we find the variable, we resolve it, passing in
  // the number o scopes between the current innermost scope and the scope
  // where the variable was found
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }
}
