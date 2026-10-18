#ifndef KTS_WASM_TIME_H
#define KTS_WASM_TIME_H

#include <stdint.h>

typedef int64_t clock_t;
#define CLOCKS_PER_SEC ((clock_t)1000000)

clock_t clock(void);

#endif
