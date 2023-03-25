#ifndef clox_vm_h
#define clox_vm_h

#include "chunk.h"
#include "table.h"
#include "value.h"

#define STACK_MAX 256

typedef struct {
  Chunk* chunk;
  // byte pointer into the middle of the bytecode array- ip : Instruction Pointer
  uint8_t* ip;
  Value stack[STACK_MAX];

  // Direct pointer instead of index since it's faster to deref the ptr
  // than calculate offset from index each time.
  // stackTop points to where the next value is to be pushed
  Value* stackTop;
  Table strings;
  Obj* objects;
} VM;

typedef enum {
  INTERPRET_OK,
  INTERPRET_COMPILE_ERROR,
  INTERPRET_RUNTIME_ERROR
} InterpretResult;

extern VM vm;

void initVM();
void freeVM();
InterpretResult interpret(const char* source);
void push(Value value);
Value pop();

#endif