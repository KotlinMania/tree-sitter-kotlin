// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * An error that occurred in [Parser.setIncludedRanges].
 */
data class IncludedRangesError(val index: ULong)
