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

  // We can track whether or not the code we are currently visiting
  // is inside a function declaration
  private FunctionType currentFunction = FunctionType.NONE;

  // We can track whether or not the code we are currently visiting
  // is inside a class
  private ClassType currentClass = ClassType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }

  private enum ClassType {
    NONE,
    CLASS
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
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name);
    define(stmt.name);

    // Whenever a this expr is envountered (atleast inside a method)
    // it will resolve to a "local" variable defined in an implicit scope
    // just outside of the block for the method body
    beginScope();
    scopes.peek().put("this", false);

    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method.function, declaration);
    }

    for (Stmt.Function method : stmt.classMethods) {
      beginScope();
      scopes.peek().put("this", false);
      resolveFunction(method.function, FunctionType.METHOD);
      endScope();
    }

    endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    // Since we've implemented anonymous functions, we need to resolve the
    // underlying function expression within the named function
    resolveFunction(stmt.function, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Can't return a value from an initializer");
      }
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null)
      resolve(stmt.elseBranch);
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
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name, false);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
      return null;
    }
    resolveLocal(expr, expr.keyword, false);
    return null;
  }

  @Override
  public Void visitFunctionExpr(Expr.Function expr) {
    resolveFunction(expr, currentFunction);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().containsKey(expr.name.lexeme)
        && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name, true);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveFunction(Expr.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.parameters) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
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
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Already a variable with this name in this scope");
    }

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
  private void resolveLocal(Expr expr, Token name, boolean isRead) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

}
