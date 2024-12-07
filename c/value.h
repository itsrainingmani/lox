#ifndef clox_value_h
#define clox_value_h

#include "common.h"

// In a native compiler to machine code, those bigger constants get stored in a
// separate “constant data” region in the binary executable. Then, the
// instruction to load a constant has an address or offset pointing to where the
// value is stored in that section. Each chunk will carry with it a list of the
// values that appear as literals in the program. To keep things simpler, we’ll
// put all constants in there, even simple integers.

typedef double Value;

// The constant pool is an array of values.
typedef struct {
  int capacity;
  int count;
  Value *values;
} ValueArray;

void initValueArray(ValueArray *array);
void writeValueArray(ValueArray *array, Value value);
void freeValueArray(ValueArray *array);
void printValue(Value value);

#endif
