package com.itsrainingmani.lox;

import java.util.ArrayList;
import java.util.List;

class Environment {

  // Each environment has a reference to the environment of the immediately
  // enclosing scope. When we look up a variable, we walk that chain from
  // innermost out until we find the variable. Starting at the inner scope
  // is how we make local variables shadow outer ones
  final Environment enclosing;
  // private final Map<String, Object> values = new HashMap<>();
  private final List<Object> values = new ArrayList<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(Object value) {
    values.add(value);
  }

  // Environment ancestor(int distance) {
  // Environment environment = this;
  // for (int i = 0; i < distance; i++) {
  // environment = environment.enclosing;
  // }

  // return environment;
  // }

  Object getAt(int distance, int slot) {
    // return ancestor(distance).values.get(name);
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }

    return environment.values.get(slot);
  }

  // Object get(Token name) {
  // if (values.containsKey(name.lexeme)) {
  // return values.get(name.lexeme);
  // }

  // // Probably faster to iteratively walk this chain
  // // but we're going recursive here for now
  // if (enclosing != null)
  // return enclosing.get(name);

  // throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  // }

  // Key difference between assignment and definition is that assignment is not
  // allowed to create a new variable
  // void assign(Token name, Object value) {
  // if (values.containsKey(name.lexeme)) {
  // values.put(name.lexeme, value);
  // return;
  // }

  // if (enclosing != null) {
  // enclosing.assign(name, value);
  // return;
  // }

  // throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  // }

  void assignAt(int distance, int slot, Object value) {
    // ancestor(distance).values.put(name.lexeme, value);
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }

    environment.values.set(slot, value);
  }
}
