package com.itsrainingmani.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();

  void define(String name, Object value) {
    values.put(name, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  // Key difference between assignment and definition is that assignment is not
  // allowed to create a new variable
  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
