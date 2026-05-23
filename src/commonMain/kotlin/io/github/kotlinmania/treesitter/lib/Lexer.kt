// port-lint: source lib/src/lexer.h
package io.github.kotlinmania.treesitter.lib

data class ColumnData(val value: UInt, val valid: Boolean)

/**
 * Internal lexer state held by the parser engine. The C runtime stored this as a struct
 * embedding a [TSLexer] (the grammar-facing lex callback surface) plus its own private state
 * (positions, included ranges, the active chunk, input, logger, column cache, and a fixed-size
 * debug serialization buffer). The Kotlin port mirrors every field, keeping mutability for the
 * positions and indices that the parser updates in place.
 */
class Lexer internal constructor(
    val data: TSLexer,
    var currentPosition: Length,
    var tokenStartPosition: Length,
    var tokenEndPosition: Length,
    var includedRanges: MutableList<TSRange>,
    var chunk: ByteArray,
    var input: TSInput,
    var logger: TSLogger?,
    var currentIncludedRangeIndex: UInt,
    var chunkStart: UInt,
    var chunkSize: UInt,
    var lookaheadSize: UInt,
    var didGetColumn: Boolean,
    var columnData: ColumnData,
    val debugBuffer: ByteArray = ByteArray(TREE_SITTER_SERIALIZATION_BUFFER_SIZE),
) {
    val includedRangeCount: UInt get() = includedRanges.size.toUInt()
}
