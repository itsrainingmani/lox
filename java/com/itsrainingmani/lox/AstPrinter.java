package com.itsrainingmani.lox;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  private String funcionName = null;

  String print(Expr expr) {
    return expr.accept(this);
  }

  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null)
      return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesizeMore("=", expr.name.lexeme, expr.value);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    return parenthesizeMore("call", expr.callee, expr.arguments);
  }

  @Override
  public String visitFunctionExpr(Expr.Function expr) {
    // return parenthesizeMore("fn", expr.parameters, expr.body);
    StringBuilder builder = new StringBuilder();

    if (funcionName == null) {
      builder.append("(fn (");
    } else {
      builder.append("(fun " + funcionName + "(");
      funcionName = null;
    }

    for (Token param : expr.parameters) {
      if (param != expr.parameters.get(0))
        builder.append(" ");
      builder.append(param.lexeme);
    }

    for (Stmt body : expr.body) {
      builder.append(body.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(block ");

    for (Stmt statement : stmt.statements) {
      builder.append(statement.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitBreakStmt(Stmt.Break stmt) {
    return "break";
  }

  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize(";", stmt.expression);
  }

  @Override
  public String visitFunctionStmt(Stmt.Function stmt) {
    funcionName = stmt.name.lexeme;
    return stmt.function.accept(this);
  }

  @Override
  public String visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      return parenthesizeMore("if", stmt.condition, stmt.thenBranch);
    }

    return parenthesizeMore("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesizeMore("print", stmt.expression);
  }

  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null)
      return "(return)";
    return parenthesize("return", stmt.value);
  }

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return parenthesizeMore("var", stmt.name);
    }

    return parenthesizeMore("var", stmt.name, "=", stmt.initializer);
  }

  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return parenthesizeMore("while", stmt.condition, stmt.body);
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  @Override
  public String visitClassStmt(Stmt.Class stmt) {
    StringBuilder builder = new StringBuilder();

    builder.append("(class " + stmt.name.lexeme);
    for (Stmt.Function method : stmt.methods) {
      builder.append(method.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  private String parenthesizeMore(String name, Object... parts) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Object part : parts) {
      builder.append(" ");
      if (part instanceof Expr) {
        builder.append(((Expr) part).accept(this));
      } else if (part instanceof Stmt) {
        builder.append(((Stmt) part).accept(this));
      } else if (part instanceof Token) {
        builder.append(((Token) part).lexeme);
      } else {
        builder.append(part);
      }
    }
    builder.append(")");

    return builder.toString();
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String visitThisExpr(Expr.This sexpr) {
    // TODO Auto-generated method stub
    return null;
  }

}
