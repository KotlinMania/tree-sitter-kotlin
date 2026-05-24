// port-lint: source include/tree_sitter/api.h
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.treesitter.lib

import kotlin.native.HiddenFromObjC

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
@HiddenFromObjC
fun interface DecodeFunction {
    operator fun invoke(string: ByteArray, length: UInt, codePoint: IntArray): UInt
}

/**
 * Callback signature for [TSInput.read]: return the next chunk of source bytes at the requested
 * byte index / position. An empty result signals end-of-input.
 */
@HiddenFromObjC
fun interface TSInputReadFn {
    operator fun invoke(payload: Any?, byteIndex: UInt, position: TSPoint): ByteArray
}

@HiddenFromObjC
class TSInput(
    val payload: Any?,
    val read: TSInputReadFn,
    val encoding: TSInputEncoding,
    val decode: DecodeFunction? = null,
)

@HiddenFromObjC
class TSParseState(
    val payload: Any?,
    val currentByteOffset: UInt,
    val hasError: Boolean,
)

@HiddenFromObjC
fun interface TSParseProgressCallback {
    operator fun invoke(state: TSParseState): Boolean
}

@HiddenFromObjC
class TSParseOptions(
    val payload: Any?,
    val progressCallback: TSParseProgressCallback? = null,
)

@HiddenFromObjC
fun interface TSLoggerLogFn {
    operator fun invoke(payload: Any?, logType: TSLogType, buffer: String)
}

@HiddenFromObjC
class TSLogger(
    val payload: Any?,
    val log: TSLoggerLogFn,
)

/**
 * Opaque node handle. The C runtime stored a 4-slot context array, a void* id, and a tree
 * pointer; the Kotlin port preserves the same shape with the id as the Subtree reference the
 * node points at and the context array as a [UIntArray] of length 4.
 */
@HiddenFromObjC
class TSNode internal constructor(
    val context: UIntArray,
    val subtree: Subtree,
    val tree: TSTree,
)

@HiddenFromObjC
class TSQueryCapture(val node: TSNode, val index: UInt)

@HiddenFromObjC
class TSQueryMatch(
    val id: UInt,
    val patternIndex: UShort,
    val captures: List<TSQueryCapture>,
)
