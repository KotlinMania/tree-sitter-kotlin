package io.github.kotlinmania.treesitter

actual class Node internal constructor() {
    actual val id: ULong get() = 0u
    actual val symbol: UShort get() = 0u
    actual val grammarSymbol: UShort get() = 0u
    actual val type: String get() = ""
    actual val grammarType: String get() = ""
    actual val isNamed: Boolean get() = false
    actual val isExtra: Boolean get() = false
    actual val isError: Boolean get() = false
    actual val isMissing: Boolean get() = false
    actual val hasChanges: Boolean get() = false
    actual val hasError: Boolean get() = false
    actual val parseState: UShort get() = 0u
    actual val nextParseState: UShort get() = 0u
    actual val startByte: UInt get() = 0u
    actual val endByte: UInt get() = 0u
    actual val byteRange: UIntRange get() = 0u..0u
    actual val range: Range get() = Range(Point.MIN, Point.MIN, 0u, 0u)
    actual val startPoint: Point get() = Point.MIN
    actual val endPoint: Point get() = Point.MIN
    actual val childCount: UInt get() = 0u
    actual val namedChildCount: UInt get() = 0u
    actual val descendantCount: UInt get() = 0u
    actual val parent: Node? get() = null
    actual val nextSibling: Node? get() = null
    actual val prevSibling: Node? get() = null
    actual val nextNamedSibling: Node? get() = null
    actual val prevNamedSibling: Node? get() = null
    actual val children: List<Node> get() = emptyList()
    actual val namedChildren: List<Node> get() = emptyList()

    @Throws(IndexOutOfBoundsException::class)
    actual fun child(index: UInt): Node? = null

    @Throws(IndexOutOfBoundsException::class)
    actual fun namedChild(index: UInt): Node? = null

    actual fun firstChildForByte(byte: UInt): Node? = null

    actual fun firstNamedChildForByte(byte: UInt): Node? = null

    actual fun childByFieldId(id: UShort): Node? = null

    actual fun childByFieldName(name: String): Node? = null

    actual fun childrenByFieldId(id: UShort): List<Node> = emptyList()

    actual fun childrenByFieldName(name: String): List<Node> = emptyList()

    @Throws(IndexOutOfBoundsException::class)
    actual fun fieldNameForChild(index: UInt): String? = null

    @Throws(IndexOutOfBoundsException::class)
    actual fun fieldNameForNamedChild(index: UInt): String? = null

    actual fun childWithDescendant(descendant: Node): Node? = null

    actual fun descendant(start: UInt, end: UInt): Node? = null

    actual fun descendant(start: Point, end: Point): Node? = null

    actual fun namedDescendant(start: UInt, end: UInt): Node? = null

    actual fun namedDescendant(start: Point, end: Point): Node? = null

    actual fun edit(edit: InputEdit) {}

    actual fun walk(): TreeCursor = throw UnsupportedOperationException("Tree-sitter is not supported on Web")

    actual fun text(): CharSequence? = null

    actual fun sexp(): String = ""

    actual override fun equals(other: Any?): Boolean = other is Node

    actual override fun hashCode(): Int = 0
}
