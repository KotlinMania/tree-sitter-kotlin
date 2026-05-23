// port-lint: source lib/src/tree_cursor.h
package io.github.kotlinmania.treesitter.lib

data class TreeCursorEntry(
    val subtree: Subtree,
    val position: Length,
    val childIndex: UInt,
    val structuralChildIndex: UInt,
    val descendantIndex: UInt,
)

class TreeCursor internal constructor(
    val tree: TSTree,
    val stack: MutableList<TreeCursorEntry>,
    var rootAliasSymbol: TSSymbol,
)

enum class TreeCursorStep { None, Hidden, Visible }

fun tsTreeCursorCurrentSubtree(self: TreeCursor): Subtree = self.stack.last().subtree
