#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType)

static Obj* allocateObject(size_t size, ObjType type) {
  Obj* object = (Obj*)reallocate(NULL, 0, size);
  object->type = type;

  // When we allocate an object, we insert it in the list
  // We insert in the head since it's easier than maintaining a pointer
  // to the tail
  object->next = vm.objects;
  vm.objects = object;
  return object;
}

/// @brief Allocates Object of given size on heap. Size includes room needed for extra payload fields needed by the specific object type being created
/// @param chars 
/// @param length 
/// @return 
static ObjString* allocateString(char* chars, int length) {
  ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
  string->length = length;
  string->chars = chars;
  return string;
}

ObjString* takeString(char* chars, int length) {
  // This function claims ownership of the string you give it
  return allocateString(chars, length);
}

ObjString* copyString(const char* chars, int length) {
  // Allocating array on the heap that is big enough for the string
  // and the trailing terminator
  char* heapChars = ALLOCATE(char, length + 1);
  memcpy(heapChars, chars, length);
  heapChars[length] = '\0';
  return allocateString(heapChars, length);
}

void printObject(Value value) {
  switch (OBJ_TYPE(value))
  {
  case OBJ_STRING:
    printf("%s", AS_CSTRING(value));
    break;
  }
}