// port-lint: source include/tree_sitter/api.h
package io.github.kotlinmania.treesitter.lib

typealias TSStateId = UShort
typealias TSSymbol = UShort
typealias TSFieldId = UShort

enum class TSInputEncoding {
    UTF8,
    UTF16LE,
    UTF16BE,
    Custom,
}

enum class TSSymbolType {
    Regular,
    Anonymous,
    Supertype,
    Auxiliary,
}

enum class TSLogType {
    Parse,
    Lex,
}

enum class TSQuantifier(val raw: UInt) {
    Zero(0u),
    ZeroOrOne(1u),
    ZeroOrMore(2u),
    One(3u),
    OneOrMore(4u),
}

enum class TSQueryPredicateStepType {
    Done,
    Capture,
    String,
}

enum class TSQueryError(val raw: UInt) {
    None(0u),
    Syntax(1u),
    NodeType(2u),
    Field(3u),
    Capture(4u),
    Structure(5u),
    Language(6u),
}

data class TSPoint(val row: UInt, val column: UInt)

data class TSRange(
    val startPoint: TSPoint,
    val endPoint: TSPoint,
    val startByte: UInt,
    val endByte: UInt,
)

data class TSInputEdit(
    val startByte: UInt,
    val oldEndByte: UInt,
    val newEndByte: UInt,
    val startPoint: TSPoint,
    val oldEndPoint: TSPoint,
    val newEndPoint: TSPoint,
)

/**
 * Callback signature for [TSInputEncoding.Custom]: decode the next code point from [string]
 * starting at offset 0, returning the number of bytes consumed. The decoded code point is
 * written to [codePoint] (index 0), or [TS_DECODE_ERROR] for an invalid sequence.
 */
typealias DecodeFunction = (string: ByteArray, length: UInt, codePoint: IntArray) -> UInt
