# N-API Native Bindings (tree-sitter)

Shared C++ N-API addon providing direct tree-sitter C library bindings for Kotlin/JS and Kotlin/WASM targets.

## Architecture

```
src/js/node-treesitter/dist/
├── binding.gyp            — source/link specification for the Java builder (target: treesitter_native)
├── package.json           — npm package (@kotlinmania/treesitter-native-bindings)
├── index.js               — loader (Release → Debug fallback)
└── src/
    └── treesitter_bindings.cpp — module init + tree-sitter C API exports
```

## Building

Run from the repository root using the Java N-API builder:

```sh
./gradlew buildNodeTreeSitter -Pproject.ffi.napi=true -Pproject.ffi.name=TreeSitter
```

The Gradle task installs the required Node addon headers with package lifecycle scripts disabled, then `NodeFfiBuilder` compiles the declared C/C++ sources against the pinned Node headers. The addon is written to this directory's `build/Release/`. The corresponding Gradle copy tasks place it in the Kotlin JS/Wasm test runtime. Build failures and compiler diagnostics remain failures.