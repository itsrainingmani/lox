#ifndef clox_vm_h
#define clox_vm_h

#include "chunk.h"
#include "table.h"
#include "value.h"

#define STACK_MAX 256

typedef struct {
  Chunk *chunk;

  // Location of the instruction being currently executed
  // We use a C pointer pointing right into the middle of the bytecode array
  // instead of an integer index bc it's faster to dereference a pointer than
  // look up an element in an array by index
  // Always points to the instruction about to be executed
  uint8_t *ip;
  Value stack[STACK_MAX];
  Value *stackTop;
  Table strings;
  Obj *objects;
} VM;

typedef enum {
  INTERPRET_OK,
  INTERPRET_COMPILE_ERROR,
  INTERPRET_RUNTIME_ERROR
} InterpretResult;

extern VM vm;

void initVM();
void freeVM();
InterpretResult interpret(const char *source);
void push(Value value);
Value pop();

#endif
