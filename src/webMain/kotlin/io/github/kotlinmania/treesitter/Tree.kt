package io.github.kotlinmania.treesitter

actual class Tree internal constructor(
    actual val language: Language,
) {
    actual val rootNode: Node get() = Node()
    actual val includedRanges: List<Range> get() = emptyList()

    actual fun rootNodeWithOffset(bytes: UInt, extent: Point): Node? = null
    actual fun edit(edit: InputEdit) {}
    actual fun copy(): Tree = Tree(language)
    actual fun walk(): TreeCursor = throw UnsupportedOperationException("Tree-sitter is not supported on Web")
    actual fun text(): CharSequence? = null
    actual fun changedRanges(newTree: Tree): List<Range> = emptyList()
}
