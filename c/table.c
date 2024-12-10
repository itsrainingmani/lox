#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

void initTable(Table *table) {
  table->count = 0;
  table->capacity = 0;
  table->entries = NULL;
}

void freeTable(Table *table) {
  FREE_ARRAY(Entry, table->entries, table->capacity);
  initTable(table);
}

static Entry *findEntry(Entry *entries, int capacity, ObjString *key) {
  // Map the key's hash code to an index within the array's bounds using modulo
  // This gives us a bucket index where we'll be able to find or place the entry
  uint32_t index = key->hash % capacity;
  Entry *tombstone = NULL;

  for (;;) {
    Entry *entry = &entries[index];
    // Exit the loop when we either find an empty bucket or a bucket with the
    // same key as the one we're looking for
    if (entry->key == NULL) {
      if (IS_NIL(entry->value)) {
        // Empty entry.
        return tombstone != NULL ? tombstone : entry;
      } else {
        // We found a tombstone.
        if (tombstone == NULL)
          tombstone = entry;
      }
    } else if (entry->key == key) {
      // We found the key.
      return entry;
    }

    // Collision: Bucket has an entry with a different key
    // Embark on linear probing (by advancing to the next element in the entries
    // array)
    index = (index + 1) % capacity;
  }
}

bool tableGet(Table *table, ObjString *key, Value *value) {
  if (table->count == 0)
    return false;

  Entry *entry = findEntry(table->entries, table->capacity, key);
  if (entry->key == NULL)
    return false;

  *value = entry->value;
  return true;
}

static void adjustCapacity(Table *table, int capacity) {
  Entry *entries = ALLOCATE(Entry, capacity);
  for (int i = 0; i < capacity; i++) {
    entries[i].key = NULL;
    entries[i].value = NIL_VAL;
  }

  // When the array size changes, entries may end up in different buckets with
  // different collisions. We rebuild the table from scratch by re-inserting
  // every entry into the new empty array
  table->count = 0;
  for (int i = 0; i < table->capacity; i++) {
    Entry *entry = &table->entries[i];
    if (entry->key == NULL)
      continue;

    Entry *dest = findEntry(entries, capacity, entry->key);
    dest->key = entry->key;
    dest->value = entry->value;
    table->count++;
  }

  FREE_ARRAY(Entry, table->entries, table->capacity);
  table->entries = entries;
  table->capacity = capacity;
}

// Add given key/value pair to the given hash table
bool tableSet(Table *table, ObjString *key, Value value) {
  if (table->count + 1 > table->capacity * TABLE_MAX_LOAD) {
    int capacity = GROW_CAPACITY(table->capacity);
    adjustCapacity(table, capacity);
  }
  Entry *entry = findEntry(table->entries, table->capacity, key);
  bool isNewKey = entry->key == NULL;

  // If an entry for that key is already present, the new value overwrites the
  // old value
  //
  // We increment the count during insertion only if the new entry goes into an
  // entirely empty bucket
  if (isNewKey && IS_NIL(entry->value))
    table->count++;

  entry->key = key;
  entry->value = value;
  return isNewKey;
}

bool tableDelete(Table *table, ObjString *key) {
  if (table->count == 0)
    return false;

  // Find the entry
  Entry *entry = findEntry(table->entries, table->capacity, key);
  if (entry->key == NULL)
    return false;

  // Place a tombstone in the entry
  //
  /* Instead of clearing the entry on deletion, we replace it with a special
   * sentinel entry called a “tombstone”. When we are following a probe sequence
   * during a lookup, and we hit a tombstone, we don’t treat it like an empty
   * slot and stop iterating. Instead, we keep going so that deleting an entry
   * doesn’t break any implicit collision chains and we can still find entries
   * after it. */

  entry->key = NULL;
  entry->value = BOOL_VAL(true);
  return true;
}

// Copying all the entries of one hash table to another
void tableAddAll(Table *from, Table *to) {
  for (int i = 0; i < from->capacity; i++) {
    Entry *entry = &from->entries[i];
    if (entry->key != NULL) {
      tableSet(to, entry->key, entry->value);
    }
  }
}

ObjString *tableFindString(Table *table, const char *chars, int length,
                           uint32_t hash) {
  if (table->count == 0)
    return NULL;

  uint32_t index = hash % table->capacity;
  for (;;) {
    Entry *entry = &table->entries[index];
    if (entry->key == NULL) {
      // Stop if we find a non-tombstone entry.
      if (IS_NIL(entry->value))
        return NULL;
    } else if (entry->key->length == length && entry->key->hash == hash &&
               memcmp(entry->key->chars, chars, length) == 0) {
      // If there is a hash collision, we do an actual character-by-character
      // string comparison. This is the one place in the VM where we actually
      // test strings for textual equality. We do it here to deduplicate strings
      // and then the rest of the VM can take for granted that any two strings
      // at different addresses in memory must have different contents.
      //
      // We found it
      return entry->key;
    }

    index = (index + 1) % table->capacity;
  }
}
