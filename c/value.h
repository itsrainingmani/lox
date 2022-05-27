#ifndef clox_value_h
#define clox_value_h

#include "common.h"

typedef double Value;

typedef struct {
  int capacity;
  int count;
  Value* values;
} ValueArray;

void initValueArray(Value* array);
void writeValueArray(Value* array, Value value);
void freeValueArray(Value* array);

#endif