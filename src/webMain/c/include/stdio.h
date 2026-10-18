#ifndef KTS_WASM_STDIO_H
#define KTS_WASM_STDIO_H

#include <stdarg.h>
#include <stddef.h>

typedef struct _kts_FILE FILE;
extern FILE *stderr;
extern FILE *stdout;

int fprintf(FILE *stream, const char *format, ...);
int fputs(const char *s, FILE *stream);
int fputc(int c, FILE *stream);
int snprintf(char *str, size_t size, const char *format, ...);
int vsnprintf(char *str, size_t size, const char *format, va_list ap);

#endif
