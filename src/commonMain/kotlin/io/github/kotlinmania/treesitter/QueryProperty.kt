// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A key-value pair associated with a particular pattern in a [Query].
 */
data class QueryProperty(
    val key: String,
    val value: String?,
    val captureId: ULong?,
)
