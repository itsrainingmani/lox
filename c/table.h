#ifndef clox_table_h
#define clox_table_h

#include "common.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75

typedef struct {
  ObjString *key;
  Value value;
} Entry;

// A hash table is an array of entries
//
//                  Count (number of kv pairs stored currently)
// Load Factor =    -------------------------------------------
//                      Capacity (Allocated size of array)
//
// Our Hash Table uses Open Addressing (Closed Hashing) where all entries live
// directly in the bucket array (one entry per bucket) combined with Linear
// Probing (When looking for an entry, look in the first bucket the key maps to.
// If it's not there, look in the very next element in the array and so on
// including wrapping around to the beginning)
//
// Cache-Friendly -> Walking the array directly in memory keeps the CPU cache
// lines full
typedef struct {
  int count;
  int capacity;
  Entry *entries;
} Table;

void initTable(Table *table);
void freeTable(Table *table);
bool tableGet(Table *table, ObjString *key, Value *value);
bool tableSet(Table *table, ObjString *key, Value value);
bool tableDelete(Table *table, ObjString *key);
void tableAddAll(Table *from, Table *to);
ObjString *tableFindString(Table *table, const char *chars, int length,
                           uint32_t hash);

#endif
