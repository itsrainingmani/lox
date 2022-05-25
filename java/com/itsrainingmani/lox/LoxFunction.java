package com.itsrainingmani.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

  private final String name;
  private final Expr.Function declaration;
  private final Environment closure;

  LoxFunction(String name, Expr.Function declaration, Environment closure) {
    this.name = name;
    this.closure = closure;
    this.declaration = declaration;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {

    // This creates an environment chain that goes from the function's body
    // out through the environments where the function is declared.
    // all the way out to the global scope
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.parameters.size(); i++) {
      environment.define(arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }
    return null;
  }

  @Override
  public int arity() {
    return declaration.parameters.size();
  }

  @Override
  public String toString() {
    if (name == null)
      return "<fn>";
    return "<fn " + name + ">";
  }

}
