// port-lint: source ffi.rs
package io.github.kotlinmania.treesitter.ffi

import io.github.kotlinmania.treesitter.Language
import io.github.kotlinmania.treesitter.LookaheadIterator
import io.github.kotlinmania.treesitter.Node
import io.github.kotlinmania.treesitter.ParseState
import io.github.kotlinmania.treesitter.Parser
import io.github.kotlinmania.treesitter.Query
import io.github.kotlinmania.treesitter.QueryCursor
import io.github.kotlinmania.treesitter.QueryCursorState
import io.github.kotlinmania.treesitter.Tree
import io.github.kotlinmania.treesitter.TreeCursor

const val TREE_SITTER_LANGUAGE_VERSION: UInt = 15u
const val TREE_SITTER_MIN_COMPATIBLE_LANGUAGE_VERSION: UInt = 13u

class TSLanguage(
    val ptr: Long,
)

class TSParser(
    val ptr: Long,
)

class TSParseState(
    val ptr: Long,
    val currentByteOffset: ULong = 0uL,
    val hasError: Boolean = false,
)

class TSTree(
    val ptr: Long,
)

class TSNode(
    val node: Node,
)

class TSTreeCursor(
    val cursor: TreeCursor,
)

class TSQuery(
    val ptr: Long,
)

class TSQueryCursor(
    val ptr: Long,
)

class TSQueryCursorState(
    val ptr: Long,
    val currentByteOffset: ULong = 0uL,
)

class TSLookaheadIterator(
    val ptr: Long,
)

object LanguageFfi {
    fun fromRaw(ptr: TSLanguage): Language = Language(ptr.ptr)

    fun intoRaw(language: Language): TSLanguage = TSLanguage(language.hashCode().toLong())
}

object ParserFfi {
    fun fromRaw(ptr: TSParser): Parser = Parser()

    fun intoRaw(parser: Parser): TSParser = TSParser(parser.hashCode().toLong())
}

object ParseStateFfi {
    fun fromRaw(ptr: TSParseState): ParseState = ParseState(ptr.currentByteOffset, ptr.hasError)

    fun intoRaw(state: ParseState): TSParseState = TSParseState(0L, state.currentByteOffset(), state.hasError())
}

object TreeFfi {
    fun fromRaw(tree: Tree): Tree = tree

    fun intoRaw(tree: Tree): TSTree = TSTree(tree.hashCode().toLong())
}

object NodeFfi {
    fun fromRaw(raw: TSNode): Node = raw.node

    fun intoRaw(node: Node): TSNode = TSNode(node)
}

object TreeCursorFfi {
    fun fromRaw(raw: TSTreeCursor): TreeCursor = raw.cursor

    fun intoRaw(cursor: TreeCursor): TSTreeCursor = TSTreeCursor(cursor)
}

object QueryFfi {
    fun fromRaw(language: Language, source: String): Query = Query(language, source)

    fun intoRaw(query: Query): TSQuery = TSQuery(query.hashCode().toLong())
}

object QueryCursorFfi {
    fun fromRaw(cursor: QueryCursor): QueryCursor = cursor

    fun intoRaw(cursor: QueryCursor): TSQueryCursor = TSQueryCursor(cursor.hashCode().toLong())
}

object QueryCursorStateFfi {
    fun fromRaw(ptr: TSQueryCursorState): QueryCursorState = QueryCursorState(ptr.currentByteOffset)

    fun intoRaw(state: QueryCursorState): TSQueryCursorState = TSQueryCursorState(0L, state.currentByteOffset())
}

object LookaheadIteratorFfi {
    fun fromRaw(language: Language, state: UShort): LookaheadIterator = language.lookaheadIterator(state)

    fun intoRaw(iterator: LookaheadIterator): TSLookaheadIterator = TSLookaheadIterator(iterator.hashCode().toLong())
}
