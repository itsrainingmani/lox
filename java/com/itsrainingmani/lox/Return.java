package com.itsrainingmani.lox;

class Return extends RuntimeException {
  final Object value;

  Return(Object value) {

    // Since we're using this class for control flow
    // and not actual exception handling,
    // we don't need overhead like stack traces or suppression
    super(null, null, false, false);
    this.value = value;
  }
}
