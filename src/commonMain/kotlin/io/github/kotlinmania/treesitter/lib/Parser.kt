// port-lint: source lib/src/parser.h
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.treesitter.lib

import kotlin.native.HiddenFromObjC

const val TS_BUILTIN_SYM_END: TSSymbol = 0u
val TS_BUILTIN_SYM_ERROR: TSSymbol = UShort.MAX_VALUE
val TS_BUILTIN_SYM_ERROR_REPEAT: TSSymbol = (UShort.MAX_VALUE.toInt() - 1).toUShort()

const val TREE_SITTER_SERIALIZATION_BUFFER_SIZE: Int = 1024

const val LANGUAGE_VERSION_WITH_RESERVED_WORDS: UInt = 15u
const val LANGUAGE_VERSION_WITH_PRIMARY_STATES: UInt = 14u

data class TSLanguageMetadata(
    val majorVersion: UByte,
    val minorVersion: UByte,
    val patchVersion: UByte,
)

data class TSFieldMapEntry(
    val fieldId: TSFieldId,
    val childIndex: UByte,
    val inherited: Boolean,
)

data class TSMapSlice(val index: UShort, val length: UShort)

data class TSSymbolMetadata(val visible: Boolean, val named: Boolean, val supertype: Boolean)

data class TSLexMode(val lexState: UShort, val externalLexState: UShort)

data class TSLexerMode(
    val lexState: UShort,
    val externalLexState: UShort,
    val reservedWordSetId: UShort,
)

data class TSCharacterRange(val start: Int, val end: Int)

/** Advance the lexer by one character, optionally marking it as skippable. */
@HiddenFromObjC
fun interface TSLexerAdvanceFn { operator fun invoke(lexer: TSLexer, skip: Boolean) }

/** Mark the end of the current token at the lexer's position. */
@HiddenFromObjC
fun interface TSLexerMarkEndFn { operator fun invoke(lexer: TSLexer) }

/** Return the column index of the lexer's current position. */
@HiddenFromObjC
fun interface TSLexerGetColumnFn { operator fun invoke(lexer: TSLexer): UInt }

/** Predicate: is the lexer's position at the start of an included range? */
@HiddenFromObjC
fun interface TSLexerIsAtIncludedRangeStartFn { operator fun invoke(lexer: TSLexer): Boolean }

/** Predicate: has the lexer reached end-of-input? */
@HiddenFromObjC
fun interface TSLexerEofFn { operator fun invoke(lexer: TSLexer): Boolean }

/** Emit a lexer log message. */
@HiddenFromObjC
fun interface TSLexerLogFn { operator fun invoke(lexer: TSLexer, message: String) }

/**
 * Lexer-side surface that grammar-generated lex functions call back into. The C runtime
 * expressed this as a struct of function pointers plus the in-flight lookahead character and
 * accepted symbol; the Kotlin port keeps the same shape with mutable properties for the
 * read/written fields and SAM interfaces for the callbacks.
 */
@HiddenFromObjC
class TSLexer(
    var lookahead: Int,
    var resultSymbol: TSSymbol,
    val advance: TSLexerAdvanceFn,
    val markEnd: TSLexerMarkEndFn,
    val getColumn: TSLexerGetColumnFn,
    val isAtIncludedRangeStart: TSLexerIsAtIncludedRangeStartFn,
    val eof: TSLexerEofFn,
    val log: TSLexerLogFn,
)

@HiddenFromObjC
enum class TSParseActionType { Shift, Reduce, Accept, Recover }

/**
 * A single parse-table action. The C runtime overlapped these in a tagged union (shift /
 * reduce / accept / recover); the Kotlin port makes the variants explicit as a sealed class.
 */
@HiddenFromObjC
sealed class TSParseAction {
    abstract val type: TSParseActionType

    class Shift(val state: TSStateId, val extra: Boolean = false, val repetition: Boolean = false) : TSParseAction() {
        override val type: TSParseActionType = TSParseActionType.Shift
    }

    class Reduce(
        val symbol: TSSymbol,
        val childCount: UByte,
        val dynamicPrecedence: Short,
        val productionId: UShort,
    ) : TSParseAction() {
        override val type: TSParseActionType = TSParseActionType.Reduce
    }

    object Accept : TSParseAction() {
        override val type: TSParseActionType = TSParseActionType.Accept
    }

    object Recover : TSParseAction() {
        override val type: TSParseActionType = TSParseActionType.Recover
    }
}

/**
 * A row entry in the parse_actions table. The C runtime expressed this as a union of a
 * TSParseAction and a length-prefix header `{count, reusable}`; the Kotlin port preserves both
 * arms explicitly so callers don't conflate them.
 */
@HiddenFromObjC
sealed class TSParseActionEntry {
    class Action(val action: TSParseAction) : TSParseActionEntry()
    class Header(val count: UByte, val reusable: Boolean) : TSParseActionEntry()
}

/** Construct a fresh external-scanner payload. */
@HiddenFromObjC
fun interface TSExternalScannerCreateFn { operator fun invoke(): Any? }

/** Tear down a previously-created external-scanner payload. */
@HiddenFromObjC
fun interface TSExternalScannerDestroyFn { operator fun invoke(payload: Any?) }

/** Run the external scanner; populate [validSymbols] with the accepted symbols. */
@HiddenFromObjC
fun interface TSExternalScannerScanFn {
    operator fun invoke(payload: Any?, lexer: TSLexer, validSymbols: BooleanArray): Boolean
}

/** Serialize external-scanner state into [buffer], returning the written length. */
@HiddenFromObjC
fun interface TSExternalScannerSerializeFn {
    operator fun invoke(payload: Any?, buffer: ByteArray): UInt
}

/** Restore external-scanner state from [buffer] with [length] bytes. */
@HiddenFromObjC
fun interface TSExternalScannerDeserializeFn {
    operator fun invoke(payload: Any?, buffer: ByteArray, length: UInt)
}

/**
 * External-scanner extension that lets a grammar contribute its own token recognizer alongside
 * the generated lex/keyword_lex functions. The C runtime stored these as opaque pointers
 * (`void *(*create)`, `void (*destroy)(void *)`, etc.); the Kotlin port keeps the same shape
 * with `Any?` standing in for the opaque payload and SAM interfaces for each hook.
 */
@HiddenFromObjC
class TSLanguageExternalScanner(
    val states: BooleanArray,
    val symbolMap: UShortArray,
    val create: TSExternalScannerCreateFn,
    val destroy: TSExternalScannerDestroyFn,
    val scan: TSExternalScannerScanFn,
    val serialize: TSExternalScannerSerializeFn,
    val deserialize: TSExternalScannerDeserializeFn,
)

/**
 * The compiled metadata for a single grammar. tree-sitter CLI generates one of these per
 * language as a static C struct populated with the parse tables, symbol tables, alias maps,
 * lex functions, and external-scanner hooks. The Kotlin port mirrors every field; the
 * compile-time arrays from C become Kotlin arrays / lists so the grammar generator (or hand
 * port) can construct one literal.
 */
/** Run the generated lex function for a parse state, returning whether a token was accepted. */
@HiddenFromObjC
fun interface TSLexFn {
    operator fun invoke(lexer: TSLexer, state: TSStateId): Boolean
}

@HiddenFromObjC
class TSLanguage internal constructor(
    val abiVersion: UInt,
    val symbolCount: UInt,
    val aliasCount: UInt,
    val tokenCount: UInt,
    val externalTokenCount: UInt,
    val stateCount: UInt,
    val largeStateCount: UInt,
    val productionIdCount: UInt,
    val fieldCount: UInt,
    val maxAliasSequenceLength: UShort,
    val parseTable: UShortArray,
    val smallParseTable: UShortArray,
    val smallParseTableMap: UIntArray,
    val parseActions: List<TSParseActionEntry>,
    val symbolNames: List<String>,
    val fieldNames: List<String>,
    val fieldMapSlices: List<TSMapSlice>,
    val fieldMapEntries: List<TSFieldMapEntry>,
    val symbolMetadata: List<TSSymbolMetadata>,
    val publicSymbolMap: UShortArray,
    val aliasMap: UShortArray,
    val aliasSequences: UShortArray,
    val lexModes: List<TSLexerMode>,
    val lexFn: TSLexFn,
    val keywordLexFn: TSLexFn,
    val keywordCaptureToken: TSSymbol,
    val externalScanner: TSLanguageExternalScanner?,
    val primaryStateIds: UShortArray,
    val name: String,
    val reservedWords: UShortArray,
    val maxReservedWordSetSize: UShort,
    val supertypeCount: UInt,
    val supertypeSymbols: UShortArray,
    val supertypeMapSlices: List<TSMapSlice>,
    val supertypeMapEntries: UShortArray,
    val metadata: TSLanguageMetadata,
)

/**
 * Binary search over a sorted [TSCharacterRange] list, asking whether [lookahead] falls inside
 * any of them. Faithful port of the `set_contains` helper that tree-sitter's generated lex
 * states call when classifying a code point against an inline character set.
 */
fun setContains(ranges: List<TSCharacterRange>, lookahead: Int): Boolean {
    if (ranges.isEmpty()) return false
    var index = 0
    var size = ranges.size - index
    while (size > 1) {
        val halfSize = size / 2
        val midIndex = index + halfSize
        val range = ranges[midIndex]
        when {
            lookahead in range.start..range.end -> return true
            lookahead > range.end -> index = midIndex
        }
        size -= halfSize
    }
    val range = ranges[index]
    return lookahead in range.start..range.end
}
