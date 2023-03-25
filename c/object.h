#ifndef clox_object_h
#define clox_object_h

#include "common.h"
#include "value.h"

#define OBJ_TYPE(value)   (AS_OBJ(value)->type)

#define IS_STRING(value)  isObjType(value, OBJ_STRING)

// These macros take a Value that is expected to contain a pointer
// to a valid ObjString on the heap
#define AS_STRING(value)  ((ObjString*)AS_OBJ(value))
#define AS_CSTRING(value) (((ObjString*)AS_OBJ(value))->chars)

typedef enum {
  OBJ_STRING,
} ObjType;

/// @brief Linked List that stores every Obj. The VM traverses the list to find every single object that has been allocated on the heap, whether or not the uesr's program or the VM's stack still has a reference to it.
struct Obj {
  ObjType type;

  // Intrusive list - The obj struct itself will be the linked list node
  // Each obj gets a pointer to the next Obj on the chain
  struct Obj* next;
};

struct ObjString {
  Obj obj;
  int length;
  char* chars;

  // Eagerly calculate the hash code since strings are Immutable
  uint32_t hash;
};

ObjString* takeString(char* chars, int length);
ObjString* copyString(const char* chars, int length);
void printObject(Value value);

static inline bool isObjType(Value value, ObjType type) {
  return IS_OBJ(value) && AS_OBJ(value)->type == type;
}

#endif