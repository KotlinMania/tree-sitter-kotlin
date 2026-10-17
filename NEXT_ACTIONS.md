# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/6 (33.3%)
- **Function parity:** 12/36 matched (target 17) — 33.3%
- **Class/type parity:** 4/42 matched (target 5) — 9.5%
- **Combined symbol parity:** 16/78 matched (target 22) — 20.5%
- **Average inline-code cosine:** 0.03 (function body across 2 matched files)
- **Average documentation cosine:** 0.00 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 2 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. wasm_language

- **Target:** `treesitter.WasmLanguage`
- **Similarity:** 0.02
- **Dependents:** 0
- **Priority Score:** 31409.8
- **Functions:** 8/10 matched (target 11)
- **Missing functions:** `_use_wasmtime`, `fmt`
- **Types:** 3/4 matched
- **Missing types:** `wasm_engine_t`
- **Lint issues:** 2

### 2. util

- **Target:** `treesitter.CBufferIter`
- **Similarity:** 0.05
- **Dependents:** 0
- **Priority Score:** 10609.5
- **Functions:** 4/4 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

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

