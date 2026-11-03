package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.internal.getTreeSitterBackend

actual class Tree internal constructor(
    internal val self: Long,
    private val source: String?,
    actual val language: Language,
) {
    internal constructor(language: Language) : this(
        self = getTreeSitterBackend().parseString(0L, language, 0L, ""),
        source = null,
        language = language,
    )

    private val backend = getTreeSitterBackend()

    actual val rootNode: Node
        get() = Node(backend.treeRootNode(self), this)

    actual val includedRanges: List<Range> get() = emptyList()

    actual fun rootNodeWithOffset(bytes: UInt, extent: Point): Node? = rootNode

    actual fun edit(edit: InputEdit) {}

    actual fun copy(): Tree = Tree(backend.copyTree(self), source, language)

    actual fun walk(): TreeCursor = rootNode.walk()

    actual fun text(): CharSequence? = source

    actual fun changedRanges(newTree: Tree): List<Range> = emptyList()
}
