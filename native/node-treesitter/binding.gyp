{
  "targets": [
    {
      "target_name": "treesitter_native",
      "sources": [
        "src/treesitter_bindings.cpp",
        "../../tree-sitter/lib/src/lib.c",
        "../../src/nativeInterop/cinterop/treesitter_ext.c"
      ],
      "include_dirs": [
        "<!@(node -p \"require('node-addon-api').include\")",
        "../../tree-sitter/lib/include",
        "../../tree-sitter/lib/src",
        "../../src/nativeInterop/cinterop"
      ],
      "dependencies": [
        "<!(node -p \"require('node-addon-api').gyp\")"
      ],
      "cflags!": [ "-fno-exceptions" ],
      "cflags_cc!": [ "-fno-exceptions" ],
      "defines": [ "NAPI_DISABLE_CPP_EXCEPTIONS", "TREE_SITTER_HIDE_SYMBOLS" ],
      "xcode_settings": {
        "GCC_ENABLE_CPP_EXCEPTIONS": "YES",
        "CLANG_CXX_LIBRARY": "libc++",
        "MACOSX_DEPLOYMENT_TARGET": "10.15"
      }
    }
  ]
}
