#include <stdio.h>

#include "common.h"
#include "debug.h"
#include "vm.h"

VM vm;

static void resetStack() {
  vm.stackTop = vm.stack;
}

void initVM() {
  resetStack();
}

void freeVM() {}

void push(Value value) {
  // stores value in the array element at the top of the stack
  *vm.stackTop = value;

  // increment the stackTop to point to the next unused slot in the array
  vm.stackTop++;
}

Value pop() {
  vm.stackTop--;
  return *vm.stackTop;
}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
  for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
    printf("          ");
    for (Value* slot = vm.stack; slot < vm.stackTop; slot++) {
      printf("[ ");
      printValue(*slot);
      printf(" ]");
    }
    printf("\n");
    disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif
    uint8_t instruction;

    // READ_BYTE reads the byte currently pointed at by ip and then advances the
    // instruction pointer. The first byte of any instruction is the opcode.
    // Given a numeric opcode, we need to get to the right C code that
    // implements the semantics. this is called decoding or dispatching the
    // instructions
    switch (instruction = READ_BYTE()) {
    case OP_CONSTANT: {
      Value constant = READ_CONSTANT();
      push(constant);
      break;
    }
    case OP_NEGATE: push(-pop()); break;
    case OP_RETURN: {
      printValue(pop());
      printf("\n");
      return INTERPRET_OK;
    }
    }
  }
#undef READ_BYTE
#undef READ_CONSTANT
}

InterpretResult interpret(Chunk* chunk) {
  vm.chunk = chunk;

  // We initialize ip by pointing it at the first byte in the chunk.
  // ip points to the instruction about to be executed not the one being
  // currently handled
  vm.ip = vm.chunk->code;
  return run();
}
