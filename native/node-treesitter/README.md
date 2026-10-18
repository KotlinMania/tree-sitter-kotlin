# N-API Native Bindings (tree-sitter)

Shared C++ N-API addon providing direct tree-sitter C library bindings for Kotlin/JS and Kotlin/WASM targets.

## Architecture

```
native/node-treesitter/
├── binding.gyp            — node-gyp build config (target: treesitter_native)
├── package.json           — npm package (@kotlinmania/treesitter-native-bindings)
├── index.js               — loader (Release → Debug fallback)
└── src/
    └── treesitter_bindings.cpp — module init + tree-sitter C API exports
```

## Building

The Gradle `buildNodeTreeSitter` task (enabled by `project.ffi.napi=true` in `gradle.properties`) builds this addon automatically:

```bash
cd native/node-treesitter
npm install --ignore-scripts
npx node-gyp configure
npx node-gyp build
```

This produces `build/Release/treesitter_native.node`.
