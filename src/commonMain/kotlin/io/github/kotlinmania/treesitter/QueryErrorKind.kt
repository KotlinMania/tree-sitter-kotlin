// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * The kind of error that occurred when creating a [Query].
 */
enum class QueryErrorKind {
    Syntax,
    NodeType,
    Field,
    Capture,
    Predicate,
    Structure,
    Language,
}
