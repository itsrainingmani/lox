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
static ObjString* allocateString(char* chars, int length, uint32_t hash) {
  ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
  string->length = length;
  string->chars = chars;
  string->hash = hash;
  return string;
}

/// @brief Implementation of FNV-1a Hashing Algorithm
/// @param key 
/// @param length 
/// @return Hashed value
static uint32_t hashString(const char* key, int length) {
  uint32_t hash = 2166136261u;
  for (int i = 0; i < length; i++) {
    hash ^= (uint8_t)key[i];
    hash *= 16777619;
  }
  return hash;
}

/// @brief takeString() claims ownership of the string you give it
ObjString* takeString(char* chars, int length) {
  uint32_t hash = hashString(chars, length);
  return allocateString(chars, length, hash);
}

ObjString* copyString(const char* chars, int length) {
  uint32_t hash = hashString(chars, length);
  // Allocating array on the heap that is big enough for the string
  // and the trailing terminator
  char* heapChars = ALLOCATE(char, length + 1);
  memcpy(heapChars, chars, length);
  heapChars[length] = '\0';
  return allocateString(heapChars, length, hash);
}

void printObject(Value value) {
  switch (OBJ_TYPE(value))
  {
  case OBJ_STRING:
    printf("%s", AS_CSTRING(value));
    break;
  }
}