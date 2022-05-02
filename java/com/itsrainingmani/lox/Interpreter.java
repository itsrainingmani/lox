package com.itsrainingmani.lox;

import com.itsrainingmani.lox.Expr.Binary;
import com.itsrainingmani.lox.Expr.Grouping;
import com.itsrainingmani.lox.Expr.Literal;
import com.itsrainingmani.lox.Expr.Unary;

class Interpreter implements Expr.Visitor<Object> {

  /*
   * The evaluation recursively traverses the tree. We can't
   * evaluate the unary operator till after we've evaluated its
   * operand subexpression. This means that our interpreter is doing
   * POST-ORDER TRAVERSAL - each node evaluates its children before
   * doing its own work
   */

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
        return -(double) right;
    }

    // Unreachable
    return null;
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        return (double) left > (double) right;
      case GREATER_EQUAL:
        return (double) left >= (double) right;
      case LESS:
        return (double) left < (double) right;
      case LESS_EQUAL:
        return (double) left <= (double) right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case MINUS:
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        break;
      case SLASH:
        return (double) left / (double) right;
      case STAR:
        return (double) left * (double) right;
    }

    // Unreachable
    return null;
  }

  private Object evaluate(Expr expr) {
    // This helper method simply sends the expression back
    // into the interpreter's visitor implementation
    return expr.accept(this);
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
}
