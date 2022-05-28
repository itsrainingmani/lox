#include "common.h"
#include "vm.h"

VM vm;

void initVM() {

}

void freeVM() {

}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
  for (;;) {
    uint8_t instruction;
    switch (instruction = READ_BYTE()) {
    case OP_RETURN: {
      return INTERPRET_OK;
    }
    }
  }
#undef READ_BYTE
}

InterpretResult interpret(Chunk* chunk) {
  vm.chunk = chunk;

  // We initialize ip by pointing it at the first byte in the chunk.
  // ip points to the instruction about to be executed not the one being currently handled
  vm.ip = vm.chunk->code;
  return run();
}