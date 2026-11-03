package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.internal.NodeHandle

actual class Node internal constructor(
    internal val handle: NodeHandle,
    internal val tree: Tree,
) {
    internal constructor() : this(
        handle =
            NodeHandle(
                id = 0L,
                type = "ERROR",
                symbol = 65535u,
                startByte = 0u,
                endByte = 0u,
                childCount = 0u,
                namedChildCount = 0u,
                isNamed = false,
                isMissing = false,
                isExtra = false,
                hasError = true,
                sexp = "(ERROR)",
            ),
        tree = Tree(Language(0L)),
    )

    actual val id: ULong get() = handle.id.toULong()
    actual val symbol: UShort get() = handle.symbol
    actual val grammarSymbol: UShort get() = handle.grammarSymbol
    actual val type: String get() = handle.type
    actual val grammarType: String get() = handle.grammarType
    actual val isNamed: Boolean get() = handle.isNamed
    actual val isExtra: Boolean get() = handle.isExtra
    actual val isError: Boolean get() = handle.symbol == 65535u.toUShort()
    actual val isMissing: Boolean get() = handle.isMissing
    actual val hasChanges: Boolean get() = false
    actual val hasError: Boolean get() = handle.hasError
    actual val parseState: UShort get() = 0u
    actual val nextParseState: UShort get() = 0u
    actual val startByte: UInt get() = handle.startByte
    actual val endByte: UInt get() = handle.endByte
    actual val byteRange: UIntRange get() = handle.startByte..handle.endByte
    actual val range: Range get() = Range(startPoint, endPoint, handle.startByte, handle.endByte)
    actual val startPoint: Point get() = Point(0u, handle.startByte)
    actual val endPoint: Point get() = Point(0u, handle.endByte)
    actual val childCount: UInt get() = handle.childCount
    actual val namedChildCount: UInt get() = handle.namedChildCount
    actual val descendantCount: UInt get() = handle.childCount
    actual val parent: Node? get() = null
    actual val nextSibling: Node? get() = null
    actual val prevSibling: Node? get() = null
    actual val nextNamedSibling: Node? get() = null
    actual val prevNamedSibling: Node? get() = null
    actual val children: List<Node> get() = handle.children.map { Node(it, tree) }
    actual val namedChildren: List<Node> get() = handle.namedChildren.map { Node(it, tree) }

    @Throws(IndexOutOfBoundsException::class)
    actual fun child(index: UInt): Node? {
        val idx = index.toInt()
        if (idx < 0 || idx >= handle.children.size) return null
        return Node(handle.children[idx], tree)
    }

    @Throws(IndexOutOfBoundsException::class)
    actual fun namedChild(index: UInt): Node? {
        val idx = index.toInt()
        if (idx < 0 || idx >= handle.namedChildren.size) return null
        return Node(handle.namedChildren[idx], tree)
    }

    actual fun firstChildForByte(byte: UInt): Node? =
        handle.children.firstOrNull { it.endByte > byte }?.let { Node(it, tree) }

    actual fun firstNamedChildForByte(byte: UInt): Node? =
        handle.namedChildren.firstOrNull { it.endByte > byte }?.let { Node(it, tree) }

    actual fun childByFieldId(id: UShort): Node? {
        val name = tree.language.fieldNameForId(id) ?: return null
        return childByFieldName(name)
    }

    actual fun childByFieldName(name: String): Node? =
        handle.fieldChildren[name]?.let { Node(it, tree) }

    actual fun childrenByFieldId(id: UShort): List<Node> {
        val child = childByFieldId(id) ?: return emptyList()
        return listOf(child)
    }

    actual fun childrenByFieldName(name: String): List<Node> {
        val child = childByFieldName(name) ?: return emptyList()
        return listOf(child)
    }

    @Throws(IndexOutOfBoundsException::class)
    actual fun fieldNameForChild(index: UInt): String? {
        val target = child(index)?.handle ?: return null
        return handle.fieldChildren.entries
            .firstOrNull { it.value == target }
            ?.key
    }

    @Throws(IndexOutOfBoundsException::class)
    actual fun fieldNameForNamedChild(index: UInt): String? {
        val target = namedChild(index)?.handle ?: return null
        return handle.fieldChildren.entries
            .firstOrNull { it.value == target }
            ?.key
    }

    actual fun childWithDescendant(descendant: Node): Node? =
        children.firstOrNull { it == descendant }

    actual fun descendant(start: UInt, end: UInt): Node? =
        if (handle.startByte <= start && handle.endByte >= end) this else null

    actual fun descendant(start: Point, end: Point): Node? = this

    actual fun namedDescendant(start: UInt, end: UInt): Node? =
        if (isNamed && handle.startByte <= start && handle.endByte >= end) this else null

    actual fun namedDescendant(start: Point, end: Point): Node? = if (isNamed) this else null

    actual fun edit(edit: InputEdit) {}

    actual fun walk(): TreeCursor = TreeCursor(this)

    actual fun text(): CharSequence? =
        tree.text()?.subSequence(
            handle.startByte.toInt().coerceAtLeast(0),
            handle.endByte.toInt().coerceAtMost(tree.text()?.length ?: 0),
        )

    actual fun sexp(): String = handle.sexp

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        return handle.id == other.handle.id &&
            handle.startByte == other.handle.startByte &&
            handle.endByte == other.handle.endByte &&
            handle.symbol == other.handle.symbol
    }

    actual override fun hashCode(): Int {
        var result = handle.id.hashCode()
        result = 31 * result + handle.symbol.hashCode()
        result = 31 * result + handle.startByte.hashCode()
        result = 31 * result + handle.endByte.hashCode()
        return result
    }

    override fun toString(): String = "Node($type, range=$range)"
}
