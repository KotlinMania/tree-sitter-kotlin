#ifndef KTS_WASM_STDLIB_H
#define KTS_WASM_STDLIB_H

#include <stddef.h>
#include <stdint.h>

void *malloc(size_t size);
void *calloc(size_t count, size_t size);
void *realloc(void *ptr, size_t new_size);
void free(void *ptr);
void abort(void);
void exit(int status);

static inline int abs(int x) { return x < 0 ? -x : x; }
static inline long labs(long x) { return x < 0 ? -x : x; }

#endif
