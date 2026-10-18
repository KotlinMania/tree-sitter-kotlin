package io.github.kotlinmania.treesitter.internal

internal class NodeHandle(
    val id: Long,
    val type: String,
    val symbol: UShort,
    val grammarSymbol: UShort = symbol,
    val grammarType: String = type,
    val startByte: UInt,
    val endByte: UInt,
    val childCount: UInt,
    val namedChildCount: UInt,
    val isNamed: Boolean,
    val isMissing: Boolean,
    val isExtra: Boolean,
    val hasError: Boolean,
    val sexp: String,
    val children: List<NodeHandle> = emptyList(),
    val namedChildren: List<NodeHandle> = emptyList(),
    val fieldChildren: Map<String, NodeHandle> = emptyMap(),
)

internal interface TreeSitterBackend {
    fun createParser(): Long

    fun deleteParser(parser: Long)

    fun setLanguage(parser: Long, language: Long): Boolean

    fun resetParser(parser: Long)

    fun setTimeoutMicros(parser: Long, timeout: ULong)

    fun parseString(parser: Long, language: Any?, oldTree: Long, source: String): Long

    fun deleteTree(tree: Long)

    fun copyTree(tree: Long): Long

    fun treeRootNode(tree: Long): NodeHandle

    fun createCursor(node: NodeHandle): Long

    fun deleteCursor(cursor: Long)

    fun cursorGotoFirstChild(cursor: Long): Boolean

    fun cursorGotoNextSibling(cursor: Long): Boolean

    fun cursorGotoParent(cursor: Long): Boolean

    fun cursorCurrentNode(cursor: Long): NodeHandle

    fun cursorCurrentFieldName(cursor: Long): String?
}

internal expect fun getTreeSitterBackend(): TreeSitterBackend
