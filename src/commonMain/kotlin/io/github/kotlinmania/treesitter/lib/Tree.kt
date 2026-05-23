// port-lint: source lib/src/tree.c
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
    var root: Subtree,
    val language: TSLanguage,
    val includedRanges: MutableList<TSRange>,
)

fun tsTreeNew(
    root: Subtree,
    language: TSLanguage,
    includedRanges: List<TSRange>,
): TSTree = TSTree(
    root = root,
    language = language,
    includedRanges = includedRanges.toMutableList(),
)

fun tsTreeCopy(self: TSTree): TSTree =
    tsTreeNew(self.root, self.language, self.includedRanges)

fun tsTreeLanguage(self: TSTree): TSLanguage = self.language

fun tsTreeIncludedRanges(self: TSTree): List<TSRange> = self.includedRanges.toList()
