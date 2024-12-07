#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum { OP_CONSTANT, OP_CONSTANT_LONG, OP_RETURN } OpCode;

// Each of these marks the beginning of a new source line in the code, and the
// corresponding byte offset of the first instruction on that line. Any bytes
// after that first one are understood to be on that same line, until we hit the
// next LineStart.
typedef struct {
  int offset;
  int line;
} LineStart;

// Dynamic Array of Bytes
// Cache-friendly, dense storage
// Constant-time indexed element lookup
// Constant-time appending to end of array
typedef struct {
  int count;
  int capacity;
  uint8_t *code;
  ValueArray constants;

  int lineCount;
  int lineCapacity;
  LineStart *lines;
} Chunk;

void initChunk(Chunk *chunk);
void freeChunk(Chunk *chunk);
void writeChunk(Chunk *chunk, uint8_t byte, int line);
void writeConstant(Chunk *chunk, Value value, int line);
int getLine(Chunk *chunk, int instrIndex);
int addConstant(Chunk *chunk, Value value);

#endif // !clox_chunk_h
