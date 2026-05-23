// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A stateful object that is passed into a query progress callback to pass in the current state
 * of the query execution.
 */
class QueryCursorState internal constructor(
    private val currentByteOffsetValue: ULong,
) {
    fun currentByteOffset(): ULong = currentByteOffsetValue
}
