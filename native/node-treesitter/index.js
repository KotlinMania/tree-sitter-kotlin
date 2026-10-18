// Entry point for the treesitter N-API native module.
// Shared between Kotlin/JS (nodeMain) and Kotlin/WASM (wasmJsMain via Node.js).
//
// Transliterated from the libc-kotlin pattern
// (kotlinmania/libc-kotlin/native/node-libc/index.js), generalized.
//
// The addon wraps direct C library calls — no insecure NPM dependencies.
// Both JS and WASM targets load the same compiled .node binary.

try {
    module.exports = require('./build/Release/treesitter_native.node');
} catch (err) {
    try {
        module.exports = require('./build/Debug/treesitter_native.node');
    } catch (err2) {
        throw new Error(
            'Could not load treesitter_native addon. ' +
            'Run `npm install` or `node-gyp rebuild` in native/node-treesitter directory.\n' +
            'Original error: ' + err.message + '\n' +
            'Debug error: ' + err2.message
        );
    }
}
