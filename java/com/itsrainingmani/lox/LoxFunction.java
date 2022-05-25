package com.itsrainingmani.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

  private final String name;
  private final Expr.Function declaration;
  private final Environment closure;

  private final boolean isInitializer;

  LoxFunction(String name, Expr.Function declaration, Environment closure, boolean isInitializer) {
    this.name = name;
    this.closure = closure;
    this.declaration = declaration;
    this.isInitializer = isInitializer;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(name, declaration, environment, isInitializer);
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {

    // This creates an environment chain that goes from the function's body
    // out through the environments where the function is declared.
    // all the way out to the global scope
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.parameters.size(); i++) {
      environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      // If we are initialized and execute a return statement
      // instead of returning the value (which will always be nil)
      // we return this
      if (isInitializer)
        return closure.getAt(0, "this");
      return returnValue.value;
    }

    if (isInitializer)
      return closure.getAt(0, "this");
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
