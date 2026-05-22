// port-lint: source lib/src/reusable_node.h
package io.github.kotlinmania.treesitter.lib

data class StackEntry(
    val tree: Subtree,
    val childIndex: UInt,
    val byteOffset: UInt,
)

class ReusableNode {
    val stack: MutableList<StackEntry> = mutableListOf()
    var lastExternalToken: Subtree? = null
}

fun reusableNodeNew(): ReusableNode = ReusableNode()

fun reusableNodeClear(self: ReusableNode) {
    self.stack.clear()
    self.lastExternalToken = null
}

fun reusableNodeTree(self: ReusableNode): Subtree? =
    if (self.stack.isNotEmpty()) self.stack.last().tree else null

fun reusableNodeByteOffset(self: ReusableNode): UInt =
    if (self.stack.isNotEmpty()) self.stack.last().byteOffset else UInt.MAX_VALUE

fun reusableNodeAdvance(self: ReusableNode) {
    val lastEntry = self.stack.last()
    val byteOffset = lastEntry.byteOffset + tsSubtreeTotalBytes(lastEntry.tree)
    if (tsSubtreeHasExternalTokens(lastEntry.tree)) {
        self.lastExternalToken = tsSubtreeLastExternalToken(lastEntry.tree)
    }

    var tree: Subtree
    var nextIndex: UInt
    do {
        val poppedEntry = self.stack.removeAt(self.stack.size - 1)
        nextIndex = poppedEntry.childIndex + 1u
        if (self.stack.isEmpty()) return
        tree = self.stack.last().tree
    } while (tsSubtreeChildCount(tree) <= nextIndex)

    self.stack.add(
        StackEntry(
            tree = tsSubtreeChildren(tree)[nextIndex.toInt()],
            childIndex = nextIndex,
            byteOffset = byteOffset,
        ),
    )
}

fun reusableNodeDescend(self: ReusableNode): Boolean {
    val lastEntry = self.stack.last()
    return if (tsSubtreeChildCount(lastEntry.tree) > 0u) {
        self.stack.add(
            StackEntry(
                tree = tsSubtreeChildren(lastEntry.tree)[0],
                childIndex = 0u,
                byteOffset = lastEntry.byteOffset,
            ),
        )
        true
    } else {
        false
    }
}

fun reusableNodeAdvancePastLeaf(self: ReusableNode) {
    while (reusableNodeDescend(self)) {}
    reusableNodeAdvance(self)
}

fun reusableNodeReset(self: ReusableNode, tree: Subtree) {
    reusableNodeClear(self)
    self.stack.add(StackEntry(tree = tree, childIndex = 0u, byteOffset = 0u))

    // Never reuse the root node, because it has a non-standard internal structure due to
    // transformations that are applied when it is accepted: adding the EOF child and any extra
    // children.
    if (!reusableNodeDescend(self)) {
        reusableNodeClear(self)
    }
}
