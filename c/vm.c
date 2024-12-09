#include <stdio.h>

#include "chunk.h"
#include "common.h"
#include "compiler.h"
#include "debug.h"
#include "value.h"
#include "vm.h"

VM vm;

static void resetStack() { vm.stackTop = vm.stack; }

void initVM() { resetStack(); }

void freeVM() {}

// The first line stores value in the array element at the top of the stack.
// Remember, stackTop points just past the last used element, at the next
// available one. This stores the value in that slot. Then we increment the
// pointer itself to point to the next unused slot in the array now that the
// previous slot is occupied.
void push(Value value) {
  *vm.stackTop = value;
  vm.stackTop++;
}

Value pop() {
  vm.stackTop--;
  return *vm.stackTop;
}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define READ_LONG_CONSTANT()                                                   \
  (vm.chunk->constants.values[(uint32_t)(READ_BYTE() | READ_BYTE() << 8 |      \
                                         READ_BYTE() << 16)])
#define BINARY_OP(op)                                                          \
  do {                                                                         \
    double b = pop();                                                          \
    double a = pop();                                                          \
    push(a op b);                                                              \
  } while (false)

  for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
    printf("          ");
    for (Value *slot = vm.stack; slot < vm.stackTop; slot++) {
      printf("[ ");
      printValue(*slot);
      printf(" ]");
    }
    printf("\n");
    disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif
    uint8_t instruction;

    // ip advances as soon as we read the opcode, before we’ve actually started
    // executing the instruction. So, again, ip points to the next byte of code
    // to be used.
    switch (instruction = READ_BYTE()) {
    case OP_CONSTANT: {
      Value constant = READ_CONSTANT();
      push(constant);
      printValue(constant);
      printf("\n");
      break;
    }
    case OP_CONSTANT_LONG: {
      Value longConstant = READ_LONG_CONSTANT();
      push(longConstant);
      printValue(longConstant);
      printf("\n");
      break;
    }
    case OP_ADD:
      BINARY_OP(+);
      break;
    case OP_SUBTRACT:
      BINARY_OP(-);
      break;
    case OP_MULTIPLY:
      BINARY_OP(*);
      break;
    case OP_DIVIDE:
      BINARY_OP(/);
      break;
    case OP_NEGATE: {
      /* push(-pop()); */
      // Directly negate the value in place on the stack
      // by multiplying it with -1
      *(vm.stackTop - 1) *= -1;
    }
    case OP_RETURN: {
      printValue(pop());
      printf("\n");
      return INTERPRET_OK;
    }
    }
  }

#undef READ_BYTE
#undef READ_CONSTANT
#undef READ_LONG_CONSTANT
#undef BINARY_OP
}

InterpretResult interpret(const char *source) {
  compile(source);
  return INTERPRET_OK;
}
