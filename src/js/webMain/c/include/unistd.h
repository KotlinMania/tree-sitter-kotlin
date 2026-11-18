#ifndef KTS_WASM_UNISTD_H
#define KTS_WASM_UNISTD_H

#include <stddef.h>

static inline int dup(int fd) { return fd; }

#endif
