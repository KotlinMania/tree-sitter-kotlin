package io.github.kotlinmania.treesitter.internal

internal class JsTreeSitterBackend : PureKotlinTreeSitterBackend() {
    private val binding: dynamic by lazy {
        try {
            js(
                """(function() {
                    try {
                        var proc = (typeof process !== 'undefined') ? process : null;
                        if (proc && proc.getBuiltinModule) {
                            var mod = proc.getBuiltinModule('module');
                            if (mod && mod.createRequire) {
                                var path = proc.getBuiltinModule('path');
                                var req = mod.createRequire(path.join(proc.cwd(), 'package.json'));
                                return req('@kotlinmania/treesitter-native-bindings');
                            }
                        }
                    } catch (_) {}
                    try {
                        if (typeof require === 'function') {
                            return require('@kotlinmania/treesitter-native-bindings');
                        }
                    } catch (_) {}
                    return null;
                })()""",
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun toBigInt(v: Long): dynamic = js("BigInt(v.toString())")

    override fun createParser(): Long {
        val b = binding
        return if (b != null && b.ts_parser_new != null) {
            val res = b.ts_parser_new()
            res.toString().toLongOrNull() ?: super.createParser()
        } else {
            super.createParser()
        }
    }

    override fun deleteParser(parser: Long) {
        val b = binding
        if (b != null && b.ts_parser_delete != null) {
            b.ts_parser_delete(toBigInt(parser))
        } else {
            super.deleteParser(parser)
        }
    }

    override fun setLanguage(parser: Long, language: Long): Boolean {
        super.setLanguage(parser, language)
        val b = binding
        if (b != null && b.ts_parser_set_language != null) {
            val langPtr =
                if (b.kts_rust_fixture_language != null) {
                    b.kts_rust_fixture_language()
                } else {
                    toBigInt(language)
                }
            val res = b.ts_parser_set_language(toBigInt(parser), langPtr)
            return (res == true)
        }
        return true
    }

    override fun resetParser(parser: Long) {
        val b = binding
        if (b != null && b.ts_parser_reset != null) {
            b.ts_parser_reset(toBigInt(parser))
        } else {
            super.resetParser(parser)
        }
    }

    override fun parseString(parser: Long, language: Any?, oldTree: Long, source: String): Long {
        val superId = super.parseString(parser, language, oldTree, source)
        val b = binding
        if (b != null && b.ts_parser_parse_string != null) {
            val res = b.ts_parser_parse_string(toBigInt(parser), toBigInt(oldTree), source)
            val treeId = res.toString().toLongOrNull()
            if (treeId != null && treeId > 0L) {
                return treeId
            }
        }
        return superId
    }

    override fun deleteTree(tree: Long) {
        val b = binding
        if (b != null && b.ts_tree_delete != null) {
            b.ts_tree_delete(toBigInt(tree))
        } else {
            super.deleteTree(tree)
        }
    }

    override fun copyTree(tree: Long): Long {
        val b = binding
        return if (b != null && b.ts_tree_copy != null) {
            val res = b.ts_tree_copy(toBigInt(tree))
            res.toString().toLongOrNull() ?: super.copyTree(tree)
        } else {
            super.copyTree(tree)
        }
    }

    override fun treeRootNode(tree: Long): NodeHandle {
        val b = binding
        return if (b != null && b.ts_tree_root_node != null) {
            val handleNum = b.ts_tree_root_node(toBigInt(tree)) as? Int ?: 0
            if (handleNum > 0) {
                readNodeFromHandle(b, handleNum)
            } else {
                super.treeRootNode(tree)
            }
        } else {
            super.treeRootNode(tree)
        }
    }

    private fun readNodeFromHandle(b: dynamic, handle: Int): NodeHandle {
        val type = (b.ts_node_type(handle) as? String) ?: ""
        val symbol = (b.ts_node_symbol(handle) as? Number)?.toInt()?.toUShort() ?: 0u
        val startByte = (b.ts_node_start_byte(handle) as? Number)?.toLong()?.toUInt() ?: 0u
        val endByte = (b.ts_node_end_byte(handle) as? Number)?.toLong()?.toUInt() ?: 0u
        val childCount = (b.ts_node_child_count(handle) as? Number)?.toLong()?.toUInt() ?: 0u
        val namedChildCount = (b.ts_node_named_child_count(handle) as? Number)?.toLong()?.toUInt() ?: 0u
        val isNamed = (b.ts_node_is_named(handle) == true)
        val isMissing = (b.ts_node_is_missing(handle) == true)
        val isExtra = (b.ts_node_is_extra(handle) == true)
        val hasError = (b.ts_node_has_error(handle) == true)
        val sexp = (b.ts_node_string(handle) as? String) ?: ""

        val children = mutableListOf<NodeHandle>()
        for (i in 0 until childCount.toInt()) {
            val childHandle = b.ts_node_child(handle, i) as? Int ?: 0
            if (childHandle > 0) {
                children.add(readNodeFromHandle(b, childHandle))
            }
        }

        val namedChildren = mutableListOf<NodeHandle>()
        for (i in 0 until namedChildCount.toInt()) {
            val childHandle = b.ts_node_named_child(handle, i) as? Int ?: 0
            if (childHandle > 0) {
                namedChildren.add(readNodeFromHandle(b, childHandle))
            }
        }

        return NodeHandle(
            id = handle.toLong(),
            type = type,
            symbol = symbol,
            startByte = startByte,
            endByte = endByte,
            childCount = childCount,
            namedChildCount = namedChildCount,
            isNamed = isNamed,
            isMissing = isMissing,
            isExtra = isExtra,
            hasError = hasError,
            sexp = sexp,
            children = children,
            namedChildren = namedChildren,
        )
    }
}

internal actual fun getTreeSitterBackend(): TreeSitterBackend = JsTreeSitterBackend()
