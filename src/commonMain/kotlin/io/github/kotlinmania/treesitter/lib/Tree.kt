// port-lint: source lib/src/tree.h
package io.github.kotlinmania.treesitter.lib

/**
 * Cache entry mapping a child subtree to its parent so tree cursors can climb upward without an
 * O(n) scan of the parent's children. The C runtime stored these in an inline array on TSTree;
 * the Kotlin port reaches the same shape with [MutableList].
 */
data class ParentCacheEntry(
    val child: Subtree,
    val parent: Subtree,
    val position: Length,
    val aliasSymbol: TSSymbol,
)

/**
 * A syntax tree. The C runtime exposed this as an opaque struct with the root [Subtree], the
 * owning [TSLanguage], and the configured included [TSRange]s.
 */
class TSTree internal constructor(
    val root: Subtree,
    val language: TSLanguage,
    val includedRanges: List<TSRange>,
)
