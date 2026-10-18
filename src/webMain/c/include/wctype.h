#ifndef KTS_WASM_WCTYPE_H
#define KTS_WASM_WCTYPE_H

#include <stdint.h>

typedef int32_t wint_t;

int iswalnum(wint_t wc);
int iswalpha(wint_t wc);
int iswblank(wint_t wc);
int iswdigit(wint_t wc);
int iswlower(wint_t wc);
int iswspace(wint_t wc);
int iswupper(wint_t wc);
int iswxdigit(wint_t wc);
wint_t towlower(wint_t wc);
wint_t towupper(wint_t wc);

#endif
