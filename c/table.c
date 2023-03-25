#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75

void initTable(Table* table) {
  table->count = 0;
  table->capacity = 0;
  table->entries = NULL;
}

void freeTable(Table* table) {
  FREE_ARRAY(Entry, table->entries, table->capacity);
  initTable(table);
}

/// @brief findEntry takes a key and an array of buckets and figures out which bucket the entry belongs in
/// @param entries 
/// @param capacity 
/// @param key 
static Entry* findEntry(Entry* entries, int capacity, ObjString* key) {
  // Modulo to map the key's hash to an index within the array's bounds (bucket index)
  uint32_t index = key->hash % capacity;

  // Linear Probing
  for (;;) {
    // Start at bucket that the entry would ideally go to
    Entry* entry = &entries[index];

    // If bucket has the same key or the bucket is empty, we're done
    if (entry->key == key || entry->key == NULL) {

      // Yield a pointer to the Found Entry so the caller can either insert something into it or read from it
      return entry;
    }

    // Else,  we advance to next element. If we go past the end of the array, the modulo op wraps us back to the beginning
    index = (index + 1) % capacity;
  }
}

static void adjustCapacity(Table* table, int capacity) {
  Entry* entries = ALLOCATE(Entry, capacity);

  for (int i = 0; i < capacity; i++)
  {
    entries[i].key = NULL;
    entries[i].value = NIL_VAL;
  }

  // When array size changes, the entries may end up in diff buckets.
  // These new buckets may have new collisions so we simply rebuild the table from scratch

  for (int i = 0; i < table->capacity; i++)
  {
    Entry* entry = &table->entries[i];
    if (entry->key == NULL) continue;

    Entry* dest = findEntry(entries, capacity, entry->key);
    dest->key = entry->key;
    dest->value = entry->value;
  }

  // Free memory for the old array
  FREE_ARRAY(Entry, table->entries, table->capacity);

  table->entries = entries;
  table->capacity = capacity;
}

/// @brief Adds given KV pair to the given hash table. If an entry is already present, new value overwrites the old value. The function returns true if a new entry was added
/// @param table 
/// @param key 
/// @param value 
/// @return Boolean
bool tableSet(Table* table, ObjString* key, Value value) {
  if (table->count + 1 > table->capacity * TABLE_MAX_LOAD) {
    int capacity = GROW_CAPACITY(table->capacity);
    adjustCapacity(table, capacity);
  }

  Entry* entry = findEntry(table->entries, table->capacity, key);
  bool isNewKey = entry->key == NULL;
  if (isNewKey) table->count++;

  entry->key = key;
  entry->value = value;

  return isNewKey;
}

void tableAddAll(Table* from, Table* to) {
  for (int i = 0; i < from->capacity; i++)
  {
    Entry* entry = &from->entries[i];
    if (entry->key != NULL) {
      tableSet(to, entry->key, entry->value);
    }
  }

}