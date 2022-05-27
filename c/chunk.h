#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"

typedef enum
{
  OP_RETURN,
} OpCode;

/* Dynamic array - wrapper around an array of bytes
  Cache-friendly, dense storage
  Constant-time indexed element lookup
  Constant-time appending to end of array
*/
typedef struct
{
  int count;
  int capacity;
  uint8_t* code;
} Chunk;

void initChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte);

#endif