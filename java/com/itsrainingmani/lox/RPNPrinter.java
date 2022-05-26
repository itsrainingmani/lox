package com.itsrainingmani.lox;

class RPNPrinter implements Expr.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return expr.left.accept(this) + " " + expr.right.accept(this) + " " + expr.operator.lexeme;
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return expr.expression.accept(this);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    String operator = expr.operator.lexeme;
    if (expr.operator.type == TokenType.MINUS) {
      // Can't use same symbol for unary and binary
      operator = "~";
    }

    return expr.right.accept(this) + " " + operator;
  }

  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(
            new Expr.Literal("str")));

    System.out.println(new RPNPrinter().print(expression));
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return null;
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return null;
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return null;
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    return null;
  }

  @Override
  public String visitFunctionExpr(Expr.Function expr) {
    return null;
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return null;
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return null;
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return null;
  }

  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return null;
  }

}
