// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A stateful object that is passed into a parse progress callback to pass in the current state
 * of the parser.
 */
class ParseState internal constructor(
    private val currentByteOffsetValue: ULong,
    private val hasErrorValue: Boolean,
) {
    fun currentByteOffset(): ULong = currentByteOffsetValue

    fun hasError(): Boolean = hasErrorValue
}
