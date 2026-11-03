package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.internal.getTreeSitterBackend

actual class TreeCursor internal constructor(
    internal var node: Node,
) {
    internal actual val tree: Tree
        get() = node.tree

    private val backend = getTreeSitterBackend()
    private var cursorId: Long = backend.createCursor(node.handle)

    actual val currentNode: Node
        get() = Node(backend.cursorCurrentNode(cursorId), node.tree)

    actual val currentDepth: UInt get() = 0u

    actual val currentDescendantIndex: UInt get() = 0u

    actual val currentFieldName: String?
        get() = backend.cursorCurrentFieldName(cursorId)

    actual val currentFieldId: UShort
        get() = currentFieldName?.let { node.tree.language.fieldIdForName(it) } ?: 0u

    actual fun copy(): TreeCursor = TreeCursor(currentNode)

    actual fun reset(node: Node) {
        backend.deleteCursor(cursorId)
        this.node = node
        this.cursorId = backend.createCursor(node.handle)
    }

    actual fun reset(cursor: TreeCursor) {
        reset(cursor.currentNode)
    }

    actual fun gotoFirstChild(): Boolean = backend.cursorGotoFirstChild(cursorId)

    actual fun gotoLastChild(): Boolean = false

    actual fun gotoParent(): Boolean = backend.cursorGotoParent(cursorId)

    actual fun gotoNextSibling(): Boolean = backend.cursorGotoNextSibling(cursorId)

    actual fun gotoPreviousSibling(): Boolean = false

    actual fun gotoDescendant(index: UInt) {}

    actual fun gotoFirstChildForByte(byte: UInt): UInt? = null

    actual fun gotoFirstChildForPoint(point: Point): UInt? = null
}
