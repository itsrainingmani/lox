#ifndef clox_value_h
#define clox_value_h

#include "common.h"

// VM's notion of type, not the user's
typedef enum { VAL_BOOL, VAL_NIL, VAL_NUMBER } ValueType;

// In a native compiler to machine code, those bigger constants get stored in a
// separate “constant data” region in the binary executable. Then, the
// instruction to load a constant has an address or offset pointing to where the
// value is stored in that section. Each chunk will carry with it a list of the
// values that appear as literals in the program. To keep things simpler, we’ll
// put all constants in there, even simple integers.

// Updating Value to be a tagged union representation
// typedef double Value;
typedef struct {
  ValueType type;
  union {
    bool boolean;
    double number;
  } as;
} Value;

#define IS_BOOL(value) ((value).type == VAL_BOOL)
#define IS_NIL(value) ((value).type == VAL_NIL)
#define IS_NUMBER(value) ((value).type == VAL_NUMBER)

#define AS_BOOL(value) ((value).as.boolean)
#define AS_NUMBER(value) ((value).as.number)

#define BOOL_VAL(value) ((Value){VAL_BOOL, {.boolean = value}})
#define NIL_VAL ((Value){VAL_NIL, {.number = 0}})
#define NUMBER_VAL(value) ((Value){VAL_NUMBER, {.number = value}})

// The constant pool is an array of values.
typedef struct {
  int capacity;
  int count;
  Value *values;
} ValueArray;

bool valuesEqual(Value a, Value b);
void initValueArray(ValueArray *array);
void writeValueArray(ValueArray *array, Value value);
void freeValueArray(ValueArray *array);
void printValue(Value value);

#endif
