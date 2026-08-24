package io.github.kotlinmania.treesitter

actual class TreeCursor internal constructor(
    internal actual val tree: Tree,
) {
    actual val currentNode: Node get() = Node()
    actual val currentDepth: UInt get() = 0u
    actual val currentFieldId: UShort get() = 0u
    actual val currentFieldName: String? get() = null
    actual val currentDescendantIndex: UInt get() = 0u

    actual fun copy(): TreeCursor = TreeCursor(tree)
    actual fun reset(node: Node) {}
    actual fun reset(cursor: TreeCursor) {}
    actual fun gotoFirstChild(): Boolean = false
    actual fun gotoLastChild(): Boolean = false
    actual fun gotoParent(): Boolean = false
    actual fun gotoNextSibling(): Boolean = false
    actual fun gotoPreviousSibling(): Boolean = false
    actual fun gotoDescendant(index: UInt) {}
    actual fun gotoFirstChildForByte(byte: UInt): UInt? = null
    actual fun gotoFirstChildForPoint(point: Point): UInt? = null
}
