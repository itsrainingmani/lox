#ifndef clox_value_h
#define clox_value_h

#include "common.h"

// Forward declaration to circumvent Cyclical dependencies
// Obj refers to a struct that contains the state shared across all object types
typedef struct Obj Obj;
typedef struct ObjString ObjString;

// VM's notion of type, not the user's
typedef enum { VAL_BOOL, VAL_NIL, VAL_NUMBER, VAL_OBJ } ValueType;

// In a native compiler to machine code, those bigger constants get stored in a
// separate “constant data” region in the binary executable. Then, the
// instruction to load a constant has an address or offset pointing to where the
// value is stored in that section. Each chunk will carry with it a list of the
// values that appear as literals in the program. To keep things simpler, we’ll
// put all constants in there, even simple integers.

// Updating Value to be a tagged union representation
// typedef double Value;
//
// Every Lox value that you can store in a variable or return from an expression
// will be a Value. For small, fixed-size types like numbers, the payload is
// stored inside the Value struct.
//
// if the object is larger, it's data lives on the heap  and the payload is a
// Pointer to the blob of memory on the heap

typedef struct {
  ValueType type;
  union {
    bool boolean;
    double number;
    Obj *obj;
  } as;
} Value;

#define IS_BOOL(value) ((value).type == VAL_BOOL)
#define IS_NIL(value) ((value).type == VAL_NIL)
#define IS_NUMBER(value) ((value).type == VAL_NUMBER)
#define IS_OBJ(value) ((value).type == VAL_OBJ)

#define AS_BOOL(value) ((value).as.boolean)
#define AS_NUMBER(value) ((value).as.number)
#define AS_OBJ(value) ((value).as.obj)

#define BOOL_VAL(value) ((Value){VAL_BOOL, {.boolean = value}})
#define NIL_VAL ((Value){VAL_NIL, {.number = 0}})
#define NUMBER_VAL(value) ((Value){VAL_NUMBER, {.number = value}})
#define OBJ_VAL(object) ((Value){VAL_OBJ, {.obj = (Obj *)object}})

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
