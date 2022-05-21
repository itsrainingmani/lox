package com.itsrainingmani.lox;

import java.util.List;

import com.itsrainingmani.lox.Expr.Assign;
import com.itsrainingmani.lox.Expr.Binary;
import com.itsrainingmani.lox.Expr.Grouping;
import com.itsrainingmani.lox.Expr.Literal;
import com.itsrainingmani.lox.Expr.Unary;
import com.itsrainingmani.lox.Expr.Variable;
import com.itsrainingmani.lox.Stmt.Block;
import com.itsrainingmani.lox.Stmt.Var;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  private Environment environment = new Environment();

  /*
   * The evaluation recursively traverses the tree. We can't
   * evaluate the unary operator till after we've evaluated its
   * operand subexpression. This means that our interpreter is doing
   * POST-ORDER TRAVERSAL - each node evaluates its children before
   * doing its own work
   */

  void interpret(List<Stmt> statmenets) {
    try {
      for (Stmt statement : statmenets) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  String interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      return stringify(value);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
      return null;
    }
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    // A Grouping node has a reference to an inner node for the expression
    // contained inside the parantheses. To evaluate the grouping expr itself
    // we recursively evalute that subexpression and return it

    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
    }

    // Unreachable
    return null;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
    }

    // Unreachable
    return null;
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;
    throw new RuntimeError(operator, "Operands must be numbers");
  }

  private Object evaluate(Expr expr) {
    // This helper method simply sends the expression back
    // into the interpreter's visitor implementation
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  // This method executes a list of statements in the context of a given
  // environment
  // Up till now, the environment field in Interpreter always pointed to the
  // global
  // env. But now it represents the current environment
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  private boolean isTruthy(Object object) {
    /*
     * Truthiness in Lox
     * Most dynamically typed languages take the universe of values
     * of all types and partition them into two sets, one of which
     * they define to be "true" or "truthy" and the rest are false or
     * "falsey".
     * 
     * In Lox, we follow Ruby's example - false and nil are falsey.
     * Everything else is truthy
     */

    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;

    return true;
  }

  private boolean isEqual(Object a, Object b) {
    // Lox doesn't do implicit conversions
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text.substring(0, text.length() - 2);
      }

      return text;
    }

    return object.toString();
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    // unlike expressions, statmenets produce no values. so the return type
    // of the visit methods is Void, not Object
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return environment.get(expr.name);
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }
}
