#include <stdlib.h>

#include "memory.h"
#include "vm.h"

void* reallocate(void* pointer, size_t oldSize, size_t newSize) {
  if (newSize == 0) {
    free(pointer);
    return NULL;
  }

  void* result = realloc(pointer, newSize);
  if (result == NULL) exit(1);
  return result;
}

// Since we aren't freeing only the Obj. Some object types also allocate other memory that they own.
// We need some type-specific code to handle each object type's special needs.
static void freeObject(Obj* object) {
  switch (object->type) {
  case OBJ_STRING: {
    ObjString* string = (ObjString*)object;

    // We free the char array and then free the ObjString
    FREE_ARRAY(char, string->chars, string->length + 1);
    FREE(ObjString, object);
    break;
  }
  }
}

void freeObjects() {
  Obj* object = vm.objects;
  while (object != NULL) {
    Obj* next = object->next;
    freeObject(object);
    object = next;
  }
}