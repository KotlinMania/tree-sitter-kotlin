#include <stdarg.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <wctype.h>
#include "tree_sitter/api.h"

#define WASM_EXPORT __attribute__((visibility("default")))

extern unsigned char __heap_base;
extern const TSLanguage *kts_rust_fixture_language(void);

struct _kts_FILE {
    int dummy;
};
static struct _kts_FILE kts_stderr_obj = { 0 };
static struct _kts_FILE kts_stdout_obj = { 0 };
FILE *stderr = &kts_stderr_obj;
FILE *stdout = &kts_stdout_obj;

int fclose(FILE *stream) { (void)stream; return 0; }
FILE *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return NULL; }
size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream) { (void)ptr; (void)size; (void)nmemb; (void)stream; return size * nmemb; }

#define KTS_WASM_PAGE_SIZE 65536u

typedef struct {
    size_t size;
} KtsAllocHeader;

static uintptr_t kts_heap_ptr = 0;
static uintptr_t kts_heap_end = 0;

static void kts_ensure_heap_init(void) {
    if (kts_heap_ptr == 0) {
        uintptr_t base = (uintptr_t)&__heap_base;
        kts_heap_ptr = (base + 7u) & ~(uintptr_t)7u;
        kts_heap_end = (uintptr_t)__builtin_wasm_memory_size(0) * KTS_WASM_PAGE_SIZE;
    }
}

void *malloc(size_t size) {
    kts_ensure_heap_init();
    if (size == 0) size = 1;
    size_t aligned_size = (size + 7u) & ~(size_t)7u;
    size_t total = sizeof(KtsAllocHeader) + aligned_size;
    if (kts_heap_ptr + total > kts_heap_end) {
        size_t needed = (kts_heap_ptr + total) - kts_heap_end;
        size_t pages = (needed + KTS_WASM_PAGE_SIZE - 1u) / KTS_WASM_PAGE_SIZE;
        if (__builtin_wasm_memory_grow(0, pages) == (size_t)-1) {
            return NULL;
        }
        kts_heap_end = (uintptr_t)__builtin_wasm_memory_size(0) * KTS_WASM_PAGE_SIZE;
    }
    KtsAllocHeader *hdr = (KtsAllocHeader *)kts_heap_ptr;
    hdr->size = aligned_size;
    void *ptr = (void *)(kts_heap_ptr + sizeof(KtsAllocHeader));
    kts_heap_ptr += total;
    return ptr;
}

void free(void *ptr) {
    if (!ptr) return;
    KtsAllocHeader *hdr = ((KtsAllocHeader *)ptr) - 1;
    uintptr_t block_end = (uintptr_t)ptr + hdr->size;
    if (block_end == kts_heap_ptr) {
        kts_heap_ptr = (uintptr_t)hdr;
    }
}

void *calloc(size_t count, size_t size) {
    size_t total = count * size;
    void *ptr = malloc(total);
    if (ptr) {
        memset(ptr, 0, total);
    }
    return ptr;
}

void *realloc(void *ptr, size_t new_size) {
    if (!ptr) return malloc(new_size);
    if (new_size == 0) {
        free(ptr);
        return NULL;
    }
    KtsAllocHeader *hdr = ((KtsAllocHeader *)ptr) - 1;
    size_t aligned_new = (new_size + 7u) & ~(size_t)7u;
    if (aligned_new <= hdr->size) {
        return ptr;
    }
    uintptr_t block_end = (uintptr_t)ptr + hdr->size;
    if (block_end == kts_heap_ptr) {
        size_t extra = aligned_new - hdr->size;
        if (kts_heap_ptr + extra <= kts_heap_end) {
            hdr->size = aligned_new;
            kts_heap_ptr += extra;
            return ptr;
        }
    }
    void *new_ptr = malloc(new_size);
    if (new_ptr) {
        memcpy(new_ptr, ptr, hdr->size < new_size ? hdr->size : new_size);
        free(ptr);
    }
    return new_ptr;
}

void *memcpy(void *dst, const void *src, size_t n) {
    unsigned char *d = (unsigned char *)dst;
    const unsigned char *s = (const unsigned char *)src;
    for (size_t i = 0; i < n; i++) d[i] = s[i];
    return dst;
}

void *memmove(void *dst, const void *src, size_t n) {
    unsigned char *d = (unsigned char *)dst;
    const unsigned char *s = (const unsigned char *)src;
    if (d < s || d >= s + n) {
        for (size_t i = 0; i < n; i++) d[i] = s[i];
    } else if (d > s) {
        for (size_t i = n; i > 0; i--) d[i - 1] = s[i - 1];
    }
    return dst;
}

void *memset(void *s, int c, size_t n) {
    unsigned char *p = (unsigned char *)s;
    unsigned char v = (unsigned char)c;
    for (size_t i = 0; i < n; i++) p[i] = v;
    return s;
}

int memcmp(const void *s1, const void *s2, size_t n) {
    const unsigned char *p1 = (const unsigned char *)s1;
    const unsigned char *p2 = (const unsigned char *)s2;
    for (size_t i = 0; i < n; i++) {
        if (p1[i] != p2[i]) return (int)p1[i] - (int)p2[i];
    }
    return 0;
}

void *memchr(const void *s, int c, size_t n) {
    const unsigned char *p = (const unsigned char *)s;
    unsigned char v = (unsigned char)c;
    for (size_t i = 0; i < n; i++) {
        if (p[i] == v) return (void *)(p + i);
    }
    return NULL;
}

size_t strlen(const char *s) {
    size_t len = 0;
    while (s[len] != '\0') len++;
    return len;
}

int strcmp(const char *s1, const char *s2) {
    while (*s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    return (int)(unsigned char)*s1 - (int)(unsigned char)*s2;
}

int strncmp(const char *s1, const char *s2, size_t n) {
    for (size_t i = 0; i < n; i++) {
        unsigned char c1 = (unsigned char)s1[i];
        unsigned char c2 = (unsigned char)s2[i];
        if (c1 != c2) return (int)c1 - (int)c2;
        if (c1 == '\0') return 0;
    }
    return 0;
}

char *strncpy(char *dst, const char *src, size_t n) {
    size_t i = 0;
    for (; i < n && src[i] != '\0'; i++) dst[i] = src[i];
    for (; i < n; i++) dst[i] = '\0';
    return dst;
}

char *strncat(char *dst, const char *src, size_t n) {
    size_t dst_len = strlen(dst);
    size_t i = 0;
    for (; i < n && src[i] != '\0'; i++) dst[dst_len + i] = src[i];
    dst[dst_len + i] = '\0';
    return dst;
}

int iswspace(wint_t wc) {
    return wc == ' ' || wc == '\t' || wc == '\n' || wc == '\r' || wc == '\f' || wc == '\v';
}
int iswdigit(wint_t wc) { return wc >= '0' && wc <= '9'; }
int iswlower(wint_t wc) { return wc >= 'a' && wc <= 'z'; }
int iswupper(wint_t wc) { return wc >= 'A' && wc <= 'Z'; }
int iswalpha(wint_t wc) { return iswlower(wc) || iswupper(wc); }
int iswalnum(wint_t wc) { return iswalpha(wc) || iswdigit(wc); }
int iswblank(wint_t wc) { return wc == ' ' || wc == '\t'; }
int iswxdigit(wint_t wc) {
    return iswdigit(wc) || (wc >= 'a' && wc <= 'f') || (wc >= 'A' && wc <= 'F');
}
wint_t towlower(wint_t wc) { return iswupper(wc) ? (wc + ('a' - 'A')) : wc; }
wint_t towupper(wint_t wc) { return iswlower(wc) ? (wc - ('a' - 'A')) : wc; }

clock_t clock(void) { return 0; }
void abort(void) { __builtin_trap(); }
void exit(int status) { (void)status; __builtin_trap(); }
int fprintf(FILE *stream, const char *format, ...) { (void)stream; (void)format; return 0; }
int fputs(const char *s, FILE *stream) { (void)s; (void)stream; return 0; }
int fputc(int c, FILE *stream) { (void)c; (void)stream; return c; }

static void kts_append_char(char *buf, size_t size, size_t *pos, char c) {
    if (size > 0 && *pos + 1 < size) {
        buf[*pos] = c;
    }
    (*pos)++;
}

static void kts_append_uint(char *buf, size_t size, size_t *pos, uint64_t val, unsigned base) {
    char tmp[32];
    size_t i = 0;
    if (val == 0) {
        kts_append_char(buf, size, pos, '0');
        return;
    }
    while (val > 0 && i < sizeof(tmp)) {
        unsigned digit = (unsigned)(val % base);
        tmp[i++] = (char)(digit < 10 ? ('0' + digit) : ('a' + (digit - 10)));
        val /= base;
    }
    while (i > 0) {
        kts_append_char(buf, size, pos, tmp[--i]);
    }
}

int vsnprintf(char *str, size_t size, const char *format, va_list ap) {
    size_t pos = 0;
    for (const char *p = format; *p != '\0'; p++) {
        if (*p != '%') {
            kts_append_char(str, size, &pos, *p);
            continue;
        }
        p++;
        if (*p == '\0') break;
        if (*p == '%') {
            kts_append_char(str, size, &pos, '%');
            continue;
        }
        int precision = -1;
        if (*p == '.') {
            p++;
            if (*p == '*') {
                precision = va_arg(ap, int);
                p++;
            } else {
                precision = 0;
                while (*p >= '0' && *p <= '9') {
                    precision = precision * 10 + (*p - '0');
                    p++;
                }
            }
        }
        int is_long = 0;
        while (*p == 'l' || *p == 'z' || *p == 'h') {
            if (*p == 'l') is_long++;
            p++;
        }
        switch (*p) {
            case 's': {
                const char *s = va_arg(ap, const char *);
                if (!s) s = "(null)";
                for (int i = 0; s[i] != '\0' && (precision < 0 || i < precision); i++) {
                    kts_append_char(str, size, &pos, s[i]);
                }
                break;
            }
            case 'c': {
                int c = va_arg(ap, int);
                kts_append_char(str, size, &pos, (char)c);
                break;
            }
            case 'd':
            case 'i': {
                int64_t v = is_long >= 2 ? va_arg(ap, int64_t) : (int64_t)va_arg(ap, int);
                if (v < 0) {
                    kts_append_char(str, size, &pos, '-');
                    kts_append_uint(str, size, &pos, (uint64_t)(-v), 10);
                } else {
                    kts_append_uint(str, size, &pos, (uint64_t)v, 10);
                }
                break;
            }
            case 'u': {
                uint64_t v = is_long >= 2 ? va_arg(ap, uint64_t) : (uint64_t)va_arg(ap, unsigned int);
                kts_append_uint(str, size, &pos, v, 10);
                break;
            }
            case 'x':
            case 'X':
            case 'p': {
                uint64_t v = is_long >= 2 ? va_arg(ap, uint64_t) : (uint64_t)va_arg(ap, unsigned int);
                kts_append_uint(str, size, &pos, v, 16);
                break;
            }
            default:
                kts_append_char(str, size, &pos, *p);
                break;
        }
    }
    if (size > 0) {
        str[pos < size ? pos : size - 1] = '\0';
    }
    return (int)pos;
}

int snprintf(char *str, size_t size, const char *format, ...) {
    va_list ap;
    va_start(ap, format);
    int res = vsnprintf(str, size, format, ap);
    va_end(ap);
    return res;
}

/* ========================================================================== */
/* Flat scalar WebAssembly exports for jsMain, wasmJsMain, and wasmWasiMain   */
/* ========================================================================== */

static TSNode kts_node_slots[4];

WASM_EXPORT int32_t kts_wasm_malloc(int32_t size) {
    return (int32_t)(uintptr_t)malloc((size_t)size);
}

WASM_EXPORT void kts_wasm_free(int32_t ptr) {
    free((void *)(uintptr_t)ptr);
}

WASM_EXPORT int32_t kts_wasm_mem_read_u8(int32_t ptr) {
    return (int32_t)(*((const uint8_t *)(uintptr_t)ptr));
}

WASM_EXPORT void kts_wasm_mem_write_u8(int32_t ptr, int32_t val) {
    *((uint8_t *)(uintptr_t)ptr) = (uint8_t)val;
}

WASM_EXPORT int32_t kts_wasm_mem_read_u16(int32_t ptr) {
    return (int32_t)(*((const uint16_t *)(uintptr_t)ptr));
}

WASM_EXPORT void kts_wasm_mem_write_u16(int32_t ptr, int32_t val) {
    *((uint16_t *)(uintptr_t)ptr) = (uint16_t)val;
}

WASM_EXPORT int32_t kts_wasm_mem_read_i32(int32_t ptr) {
    return *((const int32_t *)(uintptr_t)ptr);
}

WASM_EXPORT void kts_wasm_mem_write_i32(int32_t ptr, int32_t val) {
    *((int32_t *)(uintptr_t)ptr) = val;
}

WASM_EXPORT void kts_wasm_set_node_slot(
    int32_t slot,
    int32_t c0,
    int32_t c1,
    int32_t c2,
    int32_t c3,
    int32_t id,
    int32_t tree
) {
    TSNode *n = &kts_node_slots[slot & 3];
    n->context[0] = (uint32_t)c0;
    n->context[1] = (uint32_t)c1;
    n->context[2] = (uint32_t)c2;
    n->context[3] = (uint32_t)c3;
    n->id = (const void *)(uintptr_t)id;
    n->tree = (const TSTree *)(uintptr_t)tree;
}

WASM_EXPORT int32_t kts_wasm_get_node_c0(int32_t slot) { return (int32_t)kts_node_slots[slot & 3].context[0]; }
WASM_EXPORT int32_t kts_wasm_get_node_c1(int32_t slot) { return (int32_t)kts_node_slots[slot & 3].context[1]; }
WASM_EXPORT int32_t kts_wasm_get_node_c2(int32_t slot) { return (int32_t)kts_node_slots[slot & 3].context[2]; }
WASM_EXPORT int32_t kts_wasm_get_node_c3(int32_t slot) { return (int32_t)kts_node_slots[slot & 3].context[3]; }
WASM_EXPORT int32_t kts_wasm_get_node_id(int32_t slot) { return (int32_t)(uintptr_t)kts_node_slots[slot & 3].id; }
WASM_EXPORT int32_t kts_wasm_get_node_tree(int32_t slot) { return (int32_t)(uintptr_t)kts_node_slots[slot & 3].tree; }

static int32_t kts_store_node(int32_t slot, TSNode node) {
    if (ts_node_is_null(node)) return 0;
    kts_node_slots[slot & 3] = node;
    return 1;
}

/* Language */
WASM_EXPORT int32_t kts_wasm_rust_fixture_language(void) {
    return (int32_t)(uintptr_t)kts_rust_fixture_language();
}

WASM_EXPORT int32_t kts_wasm_language_copy(int32_t self) {
    return (int32_t)(uintptr_t)ts_language_copy((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_abi_version(int32_t self) {
    return (int32_t)ts_language_abi_version((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_symbol_count(int32_t self) {
    return (int32_t)ts_language_symbol_count((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_state_count(int32_t self) {
    return (int32_t)ts_language_state_count((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_field_count(int32_t self) {
    return (int32_t)ts_language_field_count((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_name(int32_t self) {
    return (int32_t)(uintptr_t)ts_language_name((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_metadata(int32_t self) {
    return (int32_t)(uintptr_t)ts_language_metadata((const TSLanguage *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_language_supertypes(int32_t self, int32_t len_out_ptr) {
    uint32_t len = 0;
    const TSSymbol *res = ts_language_supertypes((const TSLanguage *)(uintptr_t)self, &len);
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)res;
}

WASM_EXPORT int32_t kts_wasm_language_subtypes(int32_t self, int32_t supertype, int32_t len_out_ptr) {
    uint32_t len = 0;
    const TSSymbol *res = ts_language_subtypes((const TSLanguage *)(uintptr_t)self, (TSSymbol)supertype, &len);
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)res;
}

WASM_EXPORT int32_t kts_wasm_language_symbol_name(int32_t self, int32_t symbol) {
    return (int32_t)(uintptr_t)ts_language_symbol_name((const TSLanguage *)(uintptr_t)self, (TSSymbol)symbol);
}

WASM_EXPORT int32_t kts_wasm_language_symbol_for_name(int32_t self, int32_t name_ptr, int32_t len, int32_t is_named) {
    return (int32_t)ts_language_symbol_for_name(
        (const TSLanguage *)(uintptr_t)self,
        (const char *)(uintptr_t)name_ptr,
        (uint32_t)len,
        is_named != 0
    );
}

WASM_EXPORT int32_t kts_wasm_language_symbol_type(int32_t self, int32_t symbol) {
    return (int32_t)ts_language_symbol_type((const TSLanguage *)(uintptr_t)self, (TSSymbol)symbol);
}

WASM_EXPORT int32_t kts_wasm_language_field_name_for_id(int32_t self, int32_t id) {
    return (int32_t)(uintptr_t)ts_language_field_name_for_id((const TSLanguage *)(uintptr_t)self, (TSFieldId)id);
}

WASM_EXPORT int32_t kts_wasm_language_field_id_for_name(int32_t self, int32_t name_ptr, int32_t len) {
    return (int32_t)ts_language_field_id_for_name(
        (const TSLanguage *)(uintptr_t)self,
        (const char *)(uintptr_t)name_ptr,
        (uint32_t)len
    );
}

WASM_EXPORT int32_t kts_wasm_language_next_state(int32_t self, int32_t state, int32_t symbol) {
    return (int32_t)ts_language_next_state((const TSLanguage *)(uintptr_t)self, (TSStateId)state, (TSSymbol)symbol);
}

/* LookaheadIterator */
WASM_EXPORT int32_t kts_wasm_lookahead_iterator_new(int32_t lang, int32_t state) {
    return (int32_t)(uintptr_t)ts_lookahead_iterator_new((const TSLanguage *)(uintptr_t)lang, (TSStateId)state);
}

WASM_EXPORT void kts_wasm_lookahead_iterator_delete(int32_t self) {
    ts_lookahead_iterator_delete((TSLookaheadIterator *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_language(int32_t self) {
    return (int32_t)(uintptr_t)ts_lookahead_iterator_language((const TSLookaheadIterator *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_current_symbol(int32_t self) {
    return (int32_t)ts_lookahead_iterator_current_symbol((const TSLookaheadIterator *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_current_symbol_name(int32_t self) {
    return (int32_t)(uintptr_t)ts_lookahead_iterator_current_symbol_name((const TSLookaheadIterator *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_reset_state(int32_t self, int32_t state) {
    return ts_lookahead_iterator_reset_state((TSLookaheadIterator *)(uintptr_t)self, (TSStateId)state) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_reset(int32_t self, int32_t lang, int32_t state) {
    return ts_lookahead_iterator_reset(
        (TSLookaheadIterator *)(uintptr_t)self,
        (const TSLanguage *)(uintptr_t)lang,
        (TSStateId)state
    ) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_lookahead_iterator_next(int32_t self) {
    return ts_lookahead_iterator_next((TSLookaheadIterator *)(uintptr_t)self) ? 1 : 0;
}

/* Parser */
WASM_EXPORT int32_t kts_wasm_parser_new(void) {
    return (int32_t)(uintptr_t)ts_parser_new();
}

WASM_EXPORT void kts_wasm_parser_delete(int32_t self) {
    ts_parser_delete((TSParser *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_parser_set_language(int32_t self, int32_t lang) {
    return ts_parser_set_language((TSParser *)(uintptr_t)self, (const TSLanguage *)(uintptr_t)lang) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_parser_set_included_ranges(int32_t self, int32_t ranges_ptr, int32_t count) {
    return ts_parser_set_included_ranges(
        (TSParser *)(uintptr_t)self,
        (const TSRange *)(uintptr_t)ranges_ptr,
        (uint32_t)count
    ) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_parser_timeout_micros_low(int32_t self) {
    uint64_t v = ts_parser_timeout_micros((const TSParser *)(uintptr_t)self);
    return (int32_t)(uint32_t)(v & 0xFFFFFFFFu);
}

WASM_EXPORT int32_t kts_wasm_parser_timeout_micros_high(int32_t self) {
    uint64_t v = ts_parser_timeout_micros((const TSParser *)(uintptr_t)self);
    return (int32_t)(uint32_t)(v >> 32);
}

WASM_EXPORT void kts_wasm_parser_set_timeout_micros(int32_t self, int32_t low, int32_t high) {
    uint64_t v = ((uint64_t)(uint32_t)high << 32) | (uint64_t)(uint32_t)low;
    ts_parser_set_timeout_micros((TSParser *)(uintptr_t)self, v);
}

WASM_EXPORT void kts_wasm_parser_reset(int32_t self) {
    ts_parser_reset((TSParser *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_parser_parse_string_encoding(
    int32_t self,
    int32_t old_tree,
    int32_t str_ptr,
    int32_t len,
    int32_t encoding
) {
    return (int32_t)(uintptr_t)ts_parser_parse_string_encoding(
        (TSParser *)(uintptr_t)self,
        (const TSTree *)(uintptr_t)old_tree,
        (const char *)(uintptr_t)str_ptr,
        (uint32_t)len,
        (TSInputEncoding)encoding
    );
}

/* Tree */
WASM_EXPORT int32_t kts_wasm_tree_copy(int32_t self) {
    return (int32_t)(uintptr_t)ts_tree_copy((const TSTree *)(uintptr_t)self);
}

WASM_EXPORT void kts_wasm_tree_delete(int32_t self) {
    ts_tree_delete((TSTree *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_tree_root_node(int32_t self, int32_t out_slot) {
    return kts_store_node(out_slot, ts_tree_root_node((const TSTree *)(uintptr_t)self));
}

WASM_EXPORT int32_t kts_wasm_tree_root_node_with_offset(
    int32_t self,
    int32_t offset_bytes,
    int32_t offset_row,
    int32_t offset_col,
    int32_t out_slot
) {
    TSPoint extent = { (uint32_t)offset_row, (uint32_t)offset_col };
    TSNode node = ts_tree_root_node_with_offset((const TSTree *)(uintptr_t)self, (uint32_t)offset_bytes, extent);
    return kts_store_node(out_slot, node);
}

WASM_EXPORT int32_t kts_wasm_tree_included_ranges(int32_t self, int32_t len_out_ptr) {
    uint32_t len = 0;
    TSRange *ranges = ts_tree_included_ranges((const TSTree *)(uintptr_t)self, &len);
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)ranges;
}

WASM_EXPORT void kts_wasm_tree_edit(int32_t self, int32_t edit_ptr) {
    ts_tree_edit((TSTree *)(uintptr_t)self, (const TSInputEdit *)(uintptr_t)edit_ptr);
}

WASM_EXPORT int32_t kts_wasm_tree_get_changed_ranges(int32_t old_tree, int32_t new_tree, int32_t len_out_ptr) {
    uint32_t len = 0;
    TSRange *ranges = ts_tree_get_changed_ranges(
        (const TSTree *)(uintptr_t)old_tree,
        (const TSTree *)(uintptr_t)new_tree,
        &len
    );
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)ranges;
}

/* Node (operating on kts_node_slots[slot]) */
WASM_EXPORT int32_t kts_wasm_node_symbol(int32_t slot) {
    return (int32_t)ts_node_symbol(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_grammar_symbol(int32_t slot) {
    return (int32_t)ts_node_grammar_symbol(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_type(int32_t slot) {
    return (int32_t)(uintptr_t)ts_node_type(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_grammar_type(int32_t slot) {
    return (int32_t)(uintptr_t)ts_node_grammar_type(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_is_named(int32_t slot) {
    return ts_node_is_named(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_is_extra(int32_t slot) {
    return ts_node_is_extra(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_is_error(int32_t slot) {
    return ts_node_is_error(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_is_missing(int32_t slot) {
    return ts_node_is_missing(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_has_changes(int32_t slot) {
    return ts_node_has_changes(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_has_error(int32_t slot) {
    return ts_node_has_error(kts_node_slots[slot & 3]) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_node_parse_state(int32_t slot) {
    return (int32_t)ts_node_parse_state(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_next_parse_state(int32_t slot) {
    return (int32_t)ts_node_next_parse_state(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_start_byte(int32_t slot) {
    return (int32_t)ts_node_start_byte(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_end_byte(int32_t slot) {
    return (int32_t)ts_node_end_byte(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_start_point_row(int32_t slot) {
    return (int32_t)ts_node_start_point(kts_node_slots[slot & 3]).row;
}

WASM_EXPORT int32_t kts_wasm_node_start_point_column(int32_t slot) {
    return (int32_t)ts_node_start_point(kts_node_slots[slot & 3]).column;
}

WASM_EXPORT int32_t kts_wasm_node_end_point_row(int32_t slot) {
    return (int32_t)ts_node_end_point(kts_node_slots[slot & 3]).row;
}

WASM_EXPORT int32_t kts_wasm_node_end_point_column(int32_t slot) {
    return (int32_t)ts_node_end_point(kts_node_slots[slot & 3]).column;
}

WASM_EXPORT int32_t kts_wasm_node_child_count(int32_t slot) {
    return (int32_t)ts_node_child_count(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_named_child_count(int32_t slot) {
    return (int32_t)ts_node_named_child_count(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_descendant_count(int32_t slot) {
    return (int32_t)ts_node_descendant_count(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_parent(int32_t slot, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_parent(kts_node_slots[slot & 3]));
}

WASM_EXPORT int32_t kts_wasm_node_next_sibling(int32_t slot, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_next_sibling(kts_node_slots[slot & 3]));
}

WASM_EXPORT int32_t kts_wasm_node_prev_sibling(int32_t slot, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_prev_sibling(kts_node_slots[slot & 3]));
}

WASM_EXPORT int32_t kts_wasm_node_next_named_sibling(int32_t slot, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_next_named_sibling(kts_node_slots[slot & 3]));
}

WASM_EXPORT int32_t kts_wasm_node_prev_named_sibling(int32_t slot, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_prev_named_sibling(kts_node_slots[slot & 3]));
}

WASM_EXPORT int32_t kts_wasm_node_child(int32_t slot, int32_t index, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_child(kts_node_slots[slot & 3], (uint32_t)index));
}

WASM_EXPORT int32_t kts_wasm_node_named_child(int32_t slot, int32_t index, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_named_child(kts_node_slots[slot & 3], (uint32_t)index));
}

WASM_EXPORT int32_t kts_wasm_node_first_child_for_byte(int32_t slot, int32_t byte_offset, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_first_child_for_byte(kts_node_slots[slot & 3], (uint32_t)byte_offset));
}

WASM_EXPORT int32_t kts_wasm_node_first_named_child_for_byte(int32_t slot, int32_t byte_offset, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_first_named_child_for_byte(kts_node_slots[slot & 3], (uint32_t)byte_offset));
}

WASM_EXPORT int32_t kts_wasm_node_child_by_field_id(int32_t slot, int32_t field_id, int32_t out_slot) {
    return kts_store_node(out_slot, ts_node_child_by_field_id(kts_node_slots[slot & 3], (TSFieldId)field_id));
}

WASM_EXPORT int32_t kts_wasm_node_child_by_field_name(int32_t slot, int32_t name_ptr, int32_t name_len, int32_t out_slot) {
    return kts_store_node(
        out_slot,
        ts_node_child_by_field_name(kts_node_slots[slot & 3], (const char *)(uintptr_t)name_ptr, (uint32_t)name_len)
    );
}

WASM_EXPORT int32_t kts_wasm_node_field_name_for_child(int32_t slot, int32_t index) {
    return (int32_t)(uintptr_t)ts_node_field_name_for_child(kts_node_slots[slot & 3], (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_node_field_name_for_named_child(int32_t slot, int32_t index) {
    return (int32_t)(uintptr_t)ts_node_field_name_for_named_child(kts_node_slots[slot & 3], (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_node_child_with_descendant(int32_t slot, int32_t desc_slot, int32_t out_slot) {
    return kts_store_node(
        out_slot,
        ts_node_child_with_descendant(kts_node_slots[slot & 3], kts_node_slots[desc_slot & 3])
    );
}

WASM_EXPORT int32_t kts_wasm_node_descendant_for_byte_range(int32_t slot, int32_t start, int32_t end, int32_t out_slot) {
    return kts_store_node(
        out_slot,
        ts_node_descendant_for_byte_range(kts_node_slots[slot & 3], (uint32_t)start, (uint32_t)end)
    );
}

WASM_EXPORT int32_t kts_wasm_node_named_descendant_for_byte_range(int32_t slot, int32_t start, int32_t end, int32_t out_slot) {
    return kts_store_node(
        out_slot,
        ts_node_named_descendant_for_byte_range(kts_node_slots[slot & 3], (uint32_t)start, (uint32_t)end)
    );
}

WASM_EXPORT int32_t kts_wasm_node_descendant_for_point_range(
    int32_t slot,
    int32_t start_row,
    int32_t start_col,
    int32_t end_row,
    int32_t end_col,
    int32_t out_slot
) {
    TSPoint s = { (uint32_t)start_row, (uint32_t)start_col };
    TSPoint e = { (uint32_t)end_row, (uint32_t)end_col };
    return kts_store_node(out_slot, ts_node_descendant_for_point_range(kts_node_slots[slot & 3], s, e));
}

WASM_EXPORT int32_t kts_wasm_node_named_descendant_for_point_range(
    int32_t slot,
    int32_t start_row,
    int32_t start_col,
    int32_t end_row,
    int32_t end_col,
    int32_t out_slot
) {
    TSPoint s = { (uint32_t)start_row, (uint32_t)start_col };
    TSPoint e = { (uint32_t)end_row, (uint32_t)end_col };
    return kts_store_node(out_slot, ts_node_named_descendant_for_point_range(kts_node_slots[slot & 3], s, e));
}

WASM_EXPORT void kts_wasm_node_edit(int32_t slot, int32_t edit_ptr) {
    ts_node_edit(&kts_node_slots[slot & 3], (const TSInputEdit *)(uintptr_t)edit_ptr);
}

WASM_EXPORT int32_t kts_wasm_node_string(int32_t slot) {
    return (int32_t)(uintptr_t)ts_node_string(kts_node_slots[slot & 3]);
}

WASM_EXPORT int32_t kts_wasm_node_eq(int32_t slot_a, int32_t slot_b) {
    return ts_node_eq(kts_node_slots[slot_a & 3], kts_node_slots[slot_b & 3]) ? 1 : 0;
}

/* TreeCursor */
WASM_EXPORT int32_t kts_wasm_tree_cursor_new(int32_t slot) {
    TSTreeCursor *cursor = (TSTreeCursor *)malloc(sizeof(TSTreeCursor));
    if (!cursor) return 0;
    *cursor = ts_tree_cursor_new(kts_node_slots[slot & 3]);
    return (int32_t)(uintptr_t)cursor;
}

WASM_EXPORT void kts_wasm_tree_cursor_delete(int32_t self) {
    TSTreeCursor *cursor = (TSTreeCursor *)(uintptr_t)self;
    if (!cursor) return;
    ts_tree_cursor_delete(cursor);
    free(cursor);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_copy(int32_t self) {
    TSTreeCursor *cursor = (TSTreeCursor *)malloc(sizeof(TSTreeCursor));
    if (!cursor) return 0;
    *cursor = ts_tree_cursor_copy((const TSTreeCursor *)(uintptr_t)self);
    return (int32_t)(uintptr_t)cursor;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_current_node(int32_t self, int32_t out_slot) {
    return kts_store_node(out_slot, ts_tree_cursor_current_node((const TSTreeCursor *)(uintptr_t)self));
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_current_depth(int32_t self) {
    return (int32_t)ts_tree_cursor_current_depth((const TSTreeCursor *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_current_field_id(int32_t self) {
    return (int32_t)ts_tree_cursor_current_field_id((const TSTreeCursor *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_current_field_name(int32_t self) {
    return (int32_t)(uintptr_t)ts_tree_cursor_current_field_name((const TSTreeCursor *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_current_descendant_index(int32_t self) {
    return (int32_t)ts_tree_cursor_current_descendant_index((const TSTreeCursor *)(uintptr_t)self);
}

WASM_EXPORT void kts_wasm_tree_cursor_reset(int32_t self, int32_t slot) {
    ts_tree_cursor_reset((TSTreeCursor *)(uintptr_t)self, kts_node_slots[slot & 3]);
}

WASM_EXPORT void kts_wasm_tree_cursor_reset_to(int32_t self, int32_t src_cursor) {
    ts_tree_cursor_reset_to((TSTreeCursor *)(uintptr_t)self, (const TSTreeCursor *)(uintptr_t)src_cursor);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_first_child(int32_t self) {
    return ts_tree_cursor_goto_first_child((TSTreeCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_last_child(int32_t self) {
    return ts_tree_cursor_goto_last_child((TSTreeCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_parent(int32_t self) {
    return ts_tree_cursor_goto_parent((TSTreeCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_next_sibling(int32_t self) {
    return ts_tree_cursor_goto_next_sibling((TSTreeCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_previous_sibling(int32_t self) {
    return ts_tree_cursor_goto_previous_sibling((TSTreeCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT void kts_wasm_tree_cursor_goto_descendant(int32_t self, int32_t index) {
    ts_tree_cursor_goto_descendant((TSTreeCursor *)(uintptr_t)self, (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_first_child_for_byte(int32_t self, int32_t byte_offset) {
    int64_t idx = ts_tree_cursor_goto_first_child_for_byte((TSTreeCursor *)(uintptr_t)self, (uint32_t)byte_offset);
    return idx < 0 ? -1 : (int32_t)idx;
}

WASM_EXPORT int32_t kts_wasm_tree_cursor_goto_first_child_for_point(int32_t self, int32_t row, int32_t col) {
    TSPoint pt = { (uint32_t)row, (uint32_t)col };
    int64_t idx = ts_tree_cursor_goto_first_child_for_point((TSTreeCursor *)(uintptr_t)self, pt);
    return idx < 0 ? -1 : (int32_t)idx;
}

/* Query & QueryCursor */
WASM_EXPORT int32_t kts_wasm_query_new(
    int32_t lang,
    int32_t src_ptr,
    int32_t src_len,
    int32_t err_offset_out_ptr,
    int32_t err_type_out_ptr
) {
    uint32_t err_offset = 0;
    TSQueryError err_type = TSQueryErrorNone;
    TSQuery *q = ts_query_new(
        (const TSLanguage *)(uintptr_t)lang,
        (const char *)(uintptr_t)src_ptr,
        (uint32_t)src_len,
        &err_offset,
        &err_type
    );
    *((uint32_t *)(uintptr_t)err_offset_out_ptr) = err_offset;
    *((int32_t *)(uintptr_t)err_type_out_ptr) = (int32_t)err_type;
    return (int32_t)(uintptr_t)q;
}

WASM_EXPORT void kts_wasm_query_delete(int32_t self) {
    ts_query_delete((TSQuery *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_query_pattern_count(int32_t self) {
    return (int32_t)ts_query_pattern_count((const TSQuery *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_query_capture_count(int32_t self) {
    return (int32_t)ts_query_capture_count((const TSQuery *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_query_string_count(int32_t self) {
    return (int32_t)ts_query_string_count((const TSQuery *)(uintptr_t)self);
}

WASM_EXPORT int32_t kts_wasm_query_start_byte_for_pattern(int32_t self, int32_t index) {
    return (int32_t)ts_query_start_byte_for_pattern((const TSQuery *)(uintptr_t)self, (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_query_end_byte_for_pattern(int32_t self, int32_t index) {
    return (int32_t)ts_query_end_byte_for_pattern((const TSQuery *)(uintptr_t)self, (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_query_predicates_for_pattern(int32_t self, int32_t index, int32_t count_out_ptr) {
    uint32_t count = 0;
    const TSQueryPredicateStep *steps = ts_query_predicates_for_pattern(
        (const TSQuery *)(uintptr_t)self,
        (uint32_t)index,
        &count
    );
    *((uint32_t *)(uintptr_t)count_out_ptr) = count;
    return (int32_t)(uintptr_t)steps;
}

WASM_EXPORT int32_t kts_wasm_query_is_pattern_rooted(int32_t self, int32_t index) {
    return ts_query_is_pattern_rooted((const TSQuery *)(uintptr_t)self, (uint32_t)index) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_is_pattern_non_local(int32_t self, int32_t index) {
    return ts_query_is_pattern_non_local((const TSQuery *)(uintptr_t)self, (uint32_t)index) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_is_pattern_guaranteed_at_step(int32_t self, int32_t byte_offset) {
    return ts_query_is_pattern_guaranteed_at_step((const TSQuery *)(uintptr_t)self, (uint32_t)byte_offset) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_capture_name_for_id(int32_t self, int32_t index, int32_t len_out_ptr) {
    uint32_t len = 0;
    const char *name = ts_query_capture_name_for_id((const TSQuery *)(uintptr_t)self, (uint32_t)index, &len);
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)name;
}

WASM_EXPORT int32_t kts_wasm_query_capture_quantifier_for_id(int32_t self, int32_t pattern_idx, int32_t capture_idx) {
    return (int32_t)ts_query_capture_quantifier_for_id(
        (const TSQuery *)(uintptr_t)self,
        (uint32_t)pattern_idx,
        (uint32_t)capture_idx
    );
}

WASM_EXPORT int32_t kts_wasm_query_string_value_for_id(int32_t self, int32_t index, int32_t len_out_ptr) {
    uint32_t len = 0;
    const char *val = ts_query_string_value_for_id((const TSQuery *)(uintptr_t)self, (uint32_t)index, &len);
    *((uint32_t *)(uintptr_t)len_out_ptr) = len;
    return (int32_t)(uintptr_t)val;
}

WASM_EXPORT void kts_wasm_query_disable_capture(int32_t self, int32_t name_ptr, int32_t len) {
    ts_query_disable_capture((TSQuery *)(uintptr_t)self, (const char *)(uintptr_t)name_ptr, (uint32_t)len);
}

WASM_EXPORT void kts_wasm_query_disable_pattern(int32_t self, int32_t index) {
    ts_query_disable_pattern((TSQuery *)(uintptr_t)self, (uint32_t)index);
}

WASM_EXPORT int32_t kts_wasm_query_cursor_new(void) {
    return (int32_t)(uintptr_t)ts_query_cursor_new();
}

WASM_EXPORT void kts_wasm_query_cursor_delete(int32_t self) {
    ts_query_cursor_delete((TSQueryCursor *)(uintptr_t)self);
}

WASM_EXPORT void kts_wasm_query_cursor_exec(int32_t self, int32_t query, int32_t slot) {
    ts_query_cursor_exec(
        (TSQueryCursor *)(uintptr_t)self,
        (const TSQuery *)(uintptr_t)query,
        kts_node_slots[slot & 3]
    );
}

WASM_EXPORT int32_t kts_wasm_query_cursor_did_exceed_match_limit(int32_t self) {
    return ts_query_cursor_did_exceed_match_limit((const TSQueryCursor *)(uintptr_t)self) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_cursor_match_limit(int32_t self) {
    return (int32_t)ts_query_cursor_match_limit((const TSQueryCursor *)(uintptr_t)self);
}

WASM_EXPORT void kts_wasm_query_cursor_set_match_limit(int32_t self, int32_t limit) {
    ts_query_cursor_set_match_limit((TSQueryCursor *)(uintptr_t)self, (uint32_t)limit);
}

WASM_EXPORT void kts_wasm_query_cursor_set_max_start_depth(int32_t self, int32_t depth) {
    ts_query_cursor_set_max_start_depth((TSQueryCursor *)(uintptr_t)self, (uint32_t)depth);
}

WASM_EXPORT int32_t kts_wasm_query_cursor_set_byte_range(int32_t self, int32_t start, int32_t end) {
    return ts_query_cursor_set_byte_range((TSQueryCursor *)(uintptr_t)self, (uint32_t)start, (uint32_t)end) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_cursor_set_point_range(
    int32_t self,
    int32_t start_row,
    int32_t start_col,
    int32_t end_row,
    int32_t end_col
) {
    TSPoint s = { (uint32_t)start_row, (uint32_t)start_col };
    TSPoint e = { (uint32_t)end_row, (uint32_t)end_col };
    return ts_query_cursor_set_point_range((TSQueryCursor *)(uintptr_t)self, s, e) ? 1 : 0;
}

static TSQueryMatch kts_last_match;

WASM_EXPORT int32_t kts_wasm_query_cursor_next_match(int32_t self) {
    return ts_query_cursor_next_match((TSQueryCursor *)(uintptr_t)self, &kts_last_match) ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_query_cursor_next_capture(int32_t self, int32_t out_capture_idx_ptr) {
    uint32_t capture_idx = 0;
    bool ok = ts_query_cursor_next_capture((TSQueryCursor *)(uintptr_t)self, &kts_last_match, &capture_idx);
    if (ok) {
        *((uint32_t *)(uintptr_t)out_capture_idx_ptr) = capture_idx;
    }
    return ok ? 1 : 0;
}

WASM_EXPORT int32_t kts_wasm_last_match_id(void) {
    return (int32_t)kts_last_match.id;
}

WASM_EXPORT int32_t kts_wasm_last_match_pattern_index(void) {
    return (int32_t)kts_last_match.pattern_index;
}

WASM_EXPORT int32_t kts_wasm_last_match_capture_count(void) {
    return (int32_t)kts_last_match.capture_count;
}

WASM_EXPORT int32_t kts_wasm_last_match_capture_index(int32_t idx) {
    return (int32_t)kts_last_match.captures[idx].index;
}

WASM_EXPORT int32_t kts_wasm_last_match_capture_node(int32_t idx, int32_t out_slot) {
    return kts_store_node(out_slot, kts_last_match.captures[idx].node);
}
