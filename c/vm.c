#include <stdio.h>

#include "common.h"
#include "vm.h"

VM vm;

void initVM() {

}

void freeVM() {

}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
  for (;;) {
    uint8_t instruction;

    // READ_BYTE reads the byte currently pointed at by ip and then advances the instruction pointer. The first byte of any instruction is the opcode.
    // Given a numeric opcode, we need to get to the right C code that implements the semantics. this is called decoding or dispatching the instructions
    switch (instruction = READ_BYTE()) {
    case OP_CONSTANT: {
      Value constant = READ_CONSTANT();
      printValue(constant);
      printf("\n");
      break;
    }
    case OP_RETURN: {
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
  // ip points to the instruction about to be executed not the one being currently handled
  vm.ip = vm.chunk->code;
  return run();
}