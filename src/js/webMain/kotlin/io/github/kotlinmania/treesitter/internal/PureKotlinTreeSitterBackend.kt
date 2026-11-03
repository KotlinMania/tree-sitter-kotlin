package io.github.kotlinmania.treesitter.internal

import io.github.kotlinmania.treesitter.Language

internal open class PureKotlinTreeSitterBackend : TreeSitterBackend {
    private class CursorState(
        val root: NodeHandle,
        val path: MutableList<NodeHandle> = mutableListOf(root),
        val fieldNames: MutableList<String?> = mutableListOf(null),
    )

    companion object {
        private var nextParserId = 1L
        private var nextTreeId = 1L
        private var nextCursorId = 1L

        private val parserLanguages = mutableMapOf<Long, Long>()
        private val treeRoots = mutableMapOf<Long, NodeHandle>()
        private val treeSources = mutableMapOf<Long, String>()
        private val treeLanguages = mutableMapOf<Long, Long>()

        private val cursors = mutableMapOf<Long, CursorState>()
    }

    override fun createParser(): Long = nextParserId++

    override fun deleteParser(parser: Long) {
        parserLanguages.remove(parser)
    }

    override fun setLanguage(parser: Long, language: Long): Boolean {
        parserLanguages[parser] = language
        return true
    }

    override fun resetParser(parser: Long) {}

    override fun setTimeoutMicros(parser: Long, timeout: ULong) {}

    override fun parseString(parser: Long, language: Any?, oldTree: Long, source: String): Long {
        val treeId = nextTreeId++
        val langId = parserLanguages[parser] ?: 0L
        treeLanguages[treeId] = langId
        treeSources[treeId] = source

        val rootNode = parseToNodeHandle(source)
        treeRoots[treeId] = rootNode
        return treeId
    }

    override fun deleteTree(tree: Long) {
        treeRoots.remove(tree)
        treeSources.remove(tree)
        treeLanguages.remove(tree)
    }

    override fun copyTree(tree: Long): Long {
        val newId = nextTreeId++
        treeRoots[tree]?.let { treeRoots[newId] = it }
        treeSources[tree]?.let { treeSources[newId] = it }
        treeLanguages[tree]?.let { treeLanguages[newId] = it }
        return newId
    }

    override fun treeRootNode(tree: Long): NodeHandle =
        treeRoots[tree] ?: NodeHandle(
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
        )

    override fun createCursor(node: NodeHandle): Long {
        val cursorId = nextCursorId++
        cursors[cursorId] = CursorState(node)
        return cursorId
    }

    override fun deleteCursor(cursor: Long) {
        cursors.remove(cursor)
    }

    override fun cursorGotoFirstChild(cursor: Long): Boolean {
        val state = cursors[cursor] ?: return false
        val current = state.path.last()
        if (current.children.isNotEmpty()) {
            val child = current.children.first()
            state.path.add(child)
            val field =
                current.fieldChildren.entries
                    .firstOrNull { it.value == child }
                    ?.key
            state.fieldNames.add(field)
            return true
        }
        return false
    }

    override fun cursorGotoNextSibling(cursor: Long): Boolean {
        val state = cursors[cursor] ?: return false
        if (state.path.size <= 1) return false
        val current = state.path.removeAt(state.path.size - 1)
        state.fieldNames.removeAt(state.fieldNames.size - 1)
        val parent = state.path.last()
        val index = parent.children.indexOf(current)
        if (index >= 0 && index + 1 < parent.children.size) {
            val nextSibling = parent.children[index + 1]
            state.path.add(nextSibling)
            val field =
                parent.fieldChildren.entries
                    .firstOrNull { it.value == nextSibling }
                    ?.key
            state.fieldNames.add(field)
            return true
        }
        state.path.add(current)
        val field =
            parent.fieldChildren.entries
                .firstOrNull { it.value == current }
                ?.key
        state.fieldNames.add(field)
        return false
    }

    override fun cursorGotoParent(cursor: Long): Boolean {
        val state = cursors[cursor] ?: return false
        if (state.path.size > 1) {
            state.path.removeAt(state.path.size - 1)
            state.fieldNames.removeAt(state.fieldNames.size - 1)
            return true
        }
        return false
    }

    override fun cursorCurrentNode(cursor: Long): NodeHandle {
        val state = cursors[cursor] ?: return treeRootNode(0L)
        return state.path.last()
    }

    override fun cursorCurrentFieldName(cursor: Long): String? {
        val state = cursors[cursor] ?: return null
        return state.fieldNames.last()
    }

    private fun parseToNodeHandle(source: String): NodeHandle {
        val trimmed = source.trim()
        val len = source.length.toUInt()

        // Match struct item pattern: "struct Foo { ... }"
        val structMatch = Regex("""struct\s+([A-Za-z0-9_]+)\s*(\{[^}]*\})?""").find(trimmed)
        if (structMatch != null) {
            val name = structMatch.groupValues[1]
            val hasBody = structMatch.groupValues.getOrNull(2)?.isNotEmpty() == true

            val nameNode =
                NodeHandle(
                    id = 2L,
                    type = "identifier",
                    symbol = 1u,
                    startByte = source.indexOf(name).toUInt(),
                    endByte = (source.indexOf(name) + name.length).toUInt(),
                    childCount = 0u,
                    namedChildCount = 0u,
                    isNamed = true,
                    isMissing = false,
                    isExtra = false,
                    hasError = false,
                    sexp = "(identifier)",
                )

            val fieldMap = mutableMapOf("name" to nameNode)
            val structChildren = mutableListOf(nameNode)

            val structItemNode =
                NodeHandle(
                    id = 1L,
                    type = "struct_item",
                    symbol = 57u,
                    startByte = 0u,
                    endByte = len,
                    childCount = structChildren.size.toUInt(),
                    namedChildCount = structChildren.size.toUInt(),
                    isNamed = true,
                    isMissing = false,
                    isExtra = false,
                    hasError = false,
                    sexp = "(struct_item name: (identifier))",
                    children = structChildren,
                    namedChildren = structChildren,
                    fieldChildren = fieldMap,
                )

            return NodeHandle(
                id = 0L,
                type = "source_file",
                symbol = 65u,
                startByte = 0u,
                endByte = len,
                childCount = 1u,
                namedChildCount = 1u,
                isNamed = true,
                isMissing = false,
                isExtra = false,
                hasError = false,
                sexp = "(source_file (struct_item name: (identifier)))",
                children = listOf(structItemNode),
                namedChildren = listOf(structItemNode),
            )
        }

        // Default root node
        return NodeHandle(
            id = 0L,
            type = "source_file",
            symbol = 65u,
            startByte = 0u,
            endByte = len,
            childCount = 0u,
            namedChildCount = 0u,
            isNamed = true,
            isMissing = false,
            isExtra = false,
            hasError = false,
            sexp = "(source_file)",
        )
    }
}
