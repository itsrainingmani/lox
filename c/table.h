#ifndef clox_table_h
#define clox_table_h

#include "common.h"
#include "value.h"

/// @brief Entry is a KV pair. Key is always a string so we store the ObjString pointer directly (faster and smaller this way)
typedef struct
{
  ObjString* key;
  Value value;
} Entry;


/// @brief Hash Table is an array of entries. We keep track of both allocated size of array (capacity) and the number of KV pairs currently stored (count).
/// Ratio of count to capacity is the load factor of the hash table
typedef struct
{
  int count;
  int capacity;
  Entry* entries;
} Table;

void initTable(Table* table);
void freeTable(Table* table);
bool tableGet(Table* table, ObjString* key, Value* value);
bool tableSet(Table* table, ObjString* key, Value value);
bool tableDelete(Table* table, ObjString* key);
void tableAddAll(Table* from, Table* to);

#endif
