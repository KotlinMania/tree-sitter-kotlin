// port-lint: source ffi.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

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
import kotlin.native.HiddenFromObjC

const val TREE_SITTER_LANGUAGE_VERSION: UInt = 15u
const val TREE_SITTER_MIN_COMPATIBLE_LANGUAGE_VERSION: UInt = 13u

@HiddenFromObjC
class TSLanguage(val ptr: Long)

@HiddenFromObjC
class TSParser(val ptr: Long)

@HiddenFromObjC
class TSParseState(val ptr: Long, val currentByteOffset: ULong = 0uL, val hasError: Boolean = false)

@HiddenFromObjC
class TSTree(val ptr: Long)

@HiddenFromObjC
class TSNode(val node: Node)

@HiddenFromObjC
class TSTreeCursor(val cursor: TreeCursor)

@HiddenFromObjC
class TSQuery(val ptr: Long)

@HiddenFromObjC
class TSQueryCursor(val ptr: Long)

@HiddenFromObjC
class TSQueryCursorState(val ptr: Long, val currentByteOffset: ULong = 0uL)

@HiddenFromObjC
class TSLookaheadIterator(val ptr: Long)

@HiddenFromObjC
object LanguageFfi {
    fun fromRaw(ptr: TSLanguage): Language = Language(ptr.ptr)

    fun intoRaw(self: Language): TSLanguage = TSLanguage(self.hashCode().toLong())
}

@HiddenFromObjC
object ParserFfi {
    fun fromRaw(ptr: TSParser): Parser = Parser()

    fun intoRaw(self: Parser): TSParser = TSParser(self.hashCode().toLong())
}

@HiddenFromObjC
object ParseStateFfi {
    fun fromRaw(ptr: TSParseState): ParseState = ParseState(ptr.currentByteOffset, ptr.hasError)

    fun intoRaw(self: ParseState): TSParseState = TSParseState(0L, self.currentByteOffset(), self.hasError())
}

@HiddenFromObjC
object TreeFfi {
    fun fromRaw(tree: Tree): Tree = tree

    fun intoRaw(self: Tree): TSTree = TSTree(self.hashCode().toLong())
}

@HiddenFromObjC
object NodeFfi {
    fun fromRaw(raw: TSNode): Node = raw.node

    fun intoRaw(self: Node): TSNode = TSNode(self)
}

@HiddenFromObjC
object TreeCursorFfi {
    fun fromRaw(raw: TSTreeCursor): TreeCursor = raw.cursor

    fun intoRaw(self: TreeCursor): TSTreeCursor = TSTreeCursor(self)
}

@HiddenFromObjC
object QueryFfi {
    fun fromRaw(language: Language, source: String): Query = Query(language, source)

    fun intoRaw(self: Query): TSQuery = TSQuery(self.hashCode().toLong())
}

@HiddenFromObjC
object QueryCursorFfi {
    fun fromRaw(cursor: QueryCursor): QueryCursor = cursor

    fun intoRaw(self: QueryCursor): TSQueryCursor = TSQueryCursor(self.hashCode().toLong())
}

@HiddenFromObjC
object QueryCursorStateFfi {
    fun fromRaw(ptr: TSQueryCursorState): QueryCursorState = QueryCursorState(ptr.currentByteOffset)

    fun intoRaw(self: QueryCursorState): TSQueryCursorState = TSQueryCursorState(0L, self.currentByteOffset())
}

@HiddenFromObjC
object LookaheadIteratorFfi {
    fun fromRaw(language: Language, state: UShort): LookaheadIterator = language.lookaheadIterator(state)

    fun intoRaw(self: LookaheadIterator): TSLookaheadIterator = TSLookaheadIterator(self.hashCode().toLong())
}
