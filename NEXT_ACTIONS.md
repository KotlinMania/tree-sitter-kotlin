# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/6 (50.0%)
- **Function parity:** 34/36 matched (target 40) — 94.4%
- **Class/type parity:** 5/42 matched (target 26) — 11.9%
- **Combined symbol parity:** 39/78 matched (target 66) — 50.0%
- **Average inline-code cosine:** 0.08 (function body across 3 matched files)
- **Average documentation cosine:** 0.00 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 3 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. util

- **Target:** `util.CBufferIter`
- **Similarity:** 0.05
- **Dependents:** 0
- **Priority Score:** 10609.5
- **Functions:** 4/4 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

### 2. ffi

- **Target:** `ffi.Ffi`
- **Similarity:** 0.16
- **Dependents:** 0
- **Priority Score:** 2008.4
- **Functions:** 20/20 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 20)
- **Missing types:** _none_

### 3. wasm_language

- **Target:** `wasmlanguage.WasmLanguage`
- **Similarity:** 0.03
- **Dependents:** 0
- **Priority Score:** 1409.7
- **Functions:** 10/10 matched (target 14)
- **Missing functions:** _none_
- **Types:** 4/4 matched (target 5)
- **Missing types:** _none_
- **Lint issues:** 2

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

