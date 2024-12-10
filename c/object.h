#ifndef clox_object_h
#define clox_object_h

#include "common.h"
#include "value.h"

#define OBJ_TYPE(value) (AS_OBJ(value)->type)

#define IS_STRING(value) isObjType(value, OBJ_STRING)

#define AS_STRING(value) ((ObjString *)AS_OBJ(value))
#define AS_CSTRING(value) (((ObjString *)AS_OBJ(value))->chars)

typedef enum {
  OBJ_STRING,
} ObjType;

struct Obj {
  ObjType type;
  struct Obj *next;
};

/*
 * Memory Layout for Obj and ObjString
 *
 * Obj          | | | | |
 *             ObjType type
 *             *        *
 *             *        *
 * ObjString   | | | | | | | | | | | | | | | | | |
 *             *********
 *             Obj Obj
 *
 * C specifies that struct fields are arranged in memory in the order that they
are declared. Also when you nest structs, the inner struct's fields are expanded
right in place
 * First Bytes of ObjString line up with Obj. You can take a pointer to a struct
 * and convert it into a pointer to its first field and back.
 *
 * § 6.7.2.1 13

Within a structure object, the non-bit-field members and the units in which
bit-fields reside have addresses that increase in the order in which they are
declared. A pointer to a structure object, suitably converted, points to its
initial member (or if that member is a bit-field, then to the unit in which it
resides), and vice versa. There may be unnamed padding within a structure
object, but not at its beginning.

Given an ObjString*, you can safely cast it to Obj* and then access the type
field from it. Every ObjString “is” an Obj in the OOP sense of “is”.
 */

struct ObjString {
  Obj obj;
  int length;
  char *chars;
};

ObjString *takeString(char *chars, int length);
ObjString *copyString(const char *chars, int length);
void printObject(Value value);

static inline bool isObjType(Value value, ObjType type) {
  return IS_OBJ(value) && AS_OBJ(value)->type == type;
}

#endif
