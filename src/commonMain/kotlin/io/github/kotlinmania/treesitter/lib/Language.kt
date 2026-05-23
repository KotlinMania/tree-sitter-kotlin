// port-lint: source lib/src/language.c
package io.github.kotlinmania.treesitter.lib

/**
 * The result of looking up the actions for one (state, symbol) pair in the parse table. The C
 * runtime returned this via an out-parameter; the Kotlin port returns it directly so callers
 * don't need to allocate a mutable holder.
 */
data class TableEntry(
    val actions: List<TSParseAction>,
    val actionCount: UInt,
    val isReusable: Boolean,
)

/**
 * An iterator that yields the valid (symbol, action) pairs for one parse state. The C runtime
 * stored mutation state on this struct inline; the Kotlin port preserves the same fields with
 * Kotlin mutability so the parser engine can step through the table in-place.
 */
class LookaheadIterator internal constructor(
    val language: TSLanguage,
    var dataIndex: Int,
    var groupEnd: Int,
    var state: TSStateId,
    var tableValue: UShort,
    var sectionIndex: UShort,
    var groupCount: UShort,
    var isSmallState: Boolean,
    var actions: List<TSParseAction>,
    var symbol: TSSymbol,
    var nextState: TSStateId,
    var actionCount: UShort,
)

/**
 * Look up the parse-table value for a given (state, symbol). Faithful port of the inline
 * function in language.h: for `state >= large_state_count` the small parse table is searched
 * group-by-group; for the dense large states it's a direct 2-D array index.
 */
fun tsLanguageLookup(self: TSLanguage, state: TSStateId, symbol: TSSymbol): UShort {
    val stateValue = state.toInt() and 0xFFFF
    return if (stateValue >= self.largeStateCount.toInt()) {
        val mapIdx = stateValue - self.largeStateCount.toInt()
        var i = self.smallParseTableMap[mapIdx].toInt()
        val groupCount = self.smallParseTable[i].toInt()
        i++
        repeat(groupCount) {
            val sectionValue = self.smallParseTable[i]; i++
            val symbolCount = self.smallParseTable[i].toInt(); i++
            for (j in 0 until symbolCount) {
                if (self.smallParseTable[i] == symbol) {
                    return sectionValue
                }
                i++
            }
        }
        0u
    } else {
        self.parseTable[stateValue * self.symbolCount.toInt() + (symbol.toInt() and 0xFFFF)]
    }
}

fun tsLanguageHasActions(self: TSLanguage, state: TSStateId, symbol: TSSymbol): Boolean =
    tsLanguageLookup(self, state, symbol) != 0.toUShort()

fun tsLanguageStateIsPrimary(self: TSLanguage, state: TSStateId): Boolean =
    if (self.abiVersion >= LANGUAGE_VERSION_WITH_PRIMARY_STATES) {
        state == self.primaryStateIds[state.toInt() and 0xFFFF]
    } else {
        true
    }

/**
 * Return the slice of the [TSLanguageExternalScanner.states] table that lists which external
 * tokens are enabled in the given scanner state. Returns null when the scanner state is 0
 * (the "no external tokens" sentinel) or there is no scanner at all.
 */
fun tsLanguageEnabledExternalTokens(self: TSLanguage, externalScannerState: UInt): BooleanArray? {
    if (externalScannerState == 0u) return null
    val scanner = self.externalScanner ?: return null
    val tokenCount = self.externalTokenCount.toInt()
    val start = tokenCount * externalScannerState.toInt()
    return BooleanArray(tokenCount) { idx -> scanner.states[start + idx] }
}

/**
 * Slice of [TSLanguage.aliasSequences] containing the alias-mapping array for one production,
 * or null when the production has no aliases.
 */
fun tsLanguageAliasSequence(self: TSLanguage, productionId: UInt): UShortArray? {
    if (productionId == 0u) return null
    val width = self.maxAliasSequenceLength.toInt()
    val start = productionId.toInt() * width
    return UShortArray(width) { idx -> self.aliasSequences[start + idx] }
}

fun tsLanguageAliasAt(self: TSLanguage, productionId: UInt, childIndex: UInt): TSSymbol =
    if (productionId == 0u) {
        0u
    } else {
        val width = self.maxAliasSequenceLength.toInt()
        self.aliasSequences[productionId.toInt() * width + childIndex.toInt()]
    }

/**
 * Slice of [TSLanguage.fieldMapEntries] describing the field map for one production. Returns
 * (empty, empty) when the language has no fields configured.
 */
fun tsLanguageFieldMap(self: TSLanguage, productionId: UInt): List<TSFieldMapEntry> {
    if (self.fieldCount == 0u) return emptyList()
    val slice = self.fieldMapSlices[productionId.toInt()]
    val from = slice.index.toInt()
    val to = from + slice.length.toInt()
    return self.fieldMapEntries.subList(from, to)
}

/**
 * Return the public-symbol aliases for [originalSymbol]: a slice of [TSLanguage.aliasMap] when
 * the symbol has explicit aliases, or a one-element list containing the public symbol when it
 * doesn't. Matches the C `ts_language_aliases_for_symbol`'s pointer-pair output shape with a
 * Kotlin list return so callers don't carry start/end pointers around.
 */
fun tsLanguageCopy(self: TSLanguage?): TSLanguage? = self

fun tsLanguageSymbolCount(self: TSLanguage): UInt = self.symbolCount + self.aliasCount

fun tsLanguageStateCount(self: TSLanguage): UInt = self.stateCount

fun tsLanguageSupertypes(self: TSLanguage): UShortArray =
    if (self.abiVersion >= LANGUAGE_VERSION_WITH_RESERVED_WORDS) self.supertypeSymbols
    else UShortArray(0)

fun tsLanguageSubtypes(self: TSLanguage, supertype: TSSymbol): UShortArray {
    if (self.abiVersion < LANGUAGE_VERSION_WITH_RESERVED_WORDS ||
        !tsLanguageSymbolMetadata(self, supertype).supertype
    ) {
        return UShortArray(0)
    }
    val slice = self.supertypeMapSlices[supertype.toInt()]
    val from = slice.index.toInt()
    val length = slice.length.toInt()
    return UShortArray(length) { idx -> self.supertypeMapEntries[from + idx] }
}

fun tsLanguageVersion(self: TSLanguage): UInt = self.abiVersion

fun tsLanguageAbiVersion(self: TSLanguage): UInt = self.abiVersion

fun tsLanguageMetadataOf(self: TSLanguage): TSLanguageMetadata? =
    if (self.abiVersion >= LANGUAGE_VERSION_WITH_RESERVED_WORDS) self.metadata else null

fun tsLanguageName(self: TSLanguage): String? =
    if (self.abiVersion >= LANGUAGE_VERSION_WITH_RESERVED_WORDS) self.name else null

fun tsLanguageFieldCount(self: TSLanguage): UInt = self.fieldCount

fun tsLanguageTableEntry(self: TSLanguage, state: TSStateId, symbol: TSSymbol): TableEntry =
    if (symbol == TS_BUILTIN_SYM_ERROR || symbol == TS_BUILTIN_SYM_ERROR_REPEAT) {
        TableEntry(actions = emptyList(), actionCount = 0u, isReusable = false)
    } else {
        check(symbol < self.tokenCount.toUShort()) { "symbol $symbol out of range for tokenCount ${self.tokenCount}" }
        val actionIndex = tsLanguageLookup(self, state, symbol).toInt()
        val header = self.parseActions[actionIndex] as TSParseActionEntry.Header
        val actions = mutableListOf<TSParseAction>()
        for (i in 0 until header.count.toInt()) {
            val entry = self.parseActions[actionIndex + 1 + i] as TSParseActionEntry.Action
            actions.add(entry.action)
        }
        TableEntry(actions = actions, actionCount = header.count.toUInt(), isReusable = header.reusable)
    }

fun tsLanguageLexModeForState(self: TSLanguage, state: TSStateId): TSLexerMode {
    return self.lexModes[state.toInt() and 0xFFFF]
}

fun tsLanguageIsReservedWord(self: TSLanguage, state: TSStateId, symbol: TSSymbol): Boolean {
    val lexMode = tsLanguageLexModeForState(self, state)
    if (lexMode.reservedWordSetId > 0u) {
        val maxSize = self.maxReservedWordSetSize.toInt()
        val start = lexMode.reservedWordSetId.toInt() * maxSize
        val end = start + maxSize
        for (i in start until end) {
            val word = self.reservedWords[i]
            if (word == symbol) return true
            if (word == 0.toUShort()) break
        }
    }
    return false
}

fun tsLanguageSymbolMetadata(self: TSLanguage, symbol: TSSymbol): TSSymbolMetadata = when (symbol) {
    TS_BUILTIN_SYM_ERROR -> TSSymbolMetadata(visible = true, named = true, supertype = false)
    TS_BUILTIN_SYM_ERROR_REPEAT -> TSSymbolMetadata(visible = false, named = false, supertype = false)
    else -> self.symbolMetadata[symbol.toInt() and 0xFFFF]
}

fun tsLanguagePublicSymbol(self: TSLanguage, symbol: TSSymbol): TSSymbol =
    if (symbol == TS_BUILTIN_SYM_ERROR) symbol
    else self.publicSymbolMap[symbol.toInt() and 0xFFFF]

fun tsLanguageNextState(self: TSLanguage, state: TSStateId, symbol: TSSymbol): TSStateId {
    if (symbol == TS_BUILTIN_SYM_ERROR || symbol == TS_BUILTIN_SYM_ERROR_REPEAT) return 0u
    if (symbol < self.tokenCount.toUShort()) {
        val entry = tsLanguageTableEntry(self, state, symbol)
        if (entry.actionCount > 0u) {
            val action = entry.actions.last()
            if (action is TSParseAction.Shift) {
                return if (action.extra) state else action.state
            }
        }
        return 0u
    }
    return tsLanguageLookup(self, state, symbol)
}

fun tsLanguageSymbolName(self: TSLanguage, symbol: TSSymbol): String? = when {
    symbol == TS_BUILTIN_SYM_ERROR -> "ERROR"
    symbol == TS_BUILTIN_SYM_ERROR_REPEAT -> "_ERROR"
    symbol < tsLanguageSymbolCount(self).toUShort() -> self.symbolNames[symbol.toInt() and 0xFFFF]
    else -> null
}

fun tsLanguageSymbolForName(self: TSLanguage, name: String, isNamed: Boolean): TSSymbol {
    if (isNamed && name == "ERROR") return TS_BUILTIN_SYM_ERROR
    val count = tsLanguageSymbolCount(self).toInt()
    for (i in 0 until count) {
        val metadata = tsLanguageSymbolMetadata(self, i.toUShort())
        if ((!metadata.visible && !metadata.supertype) || metadata.named != isNamed) continue
        if (self.symbolNames[i] == name) {
            return self.publicSymbolMap[i]
        }
    }
    return 0u
}

fun tsLanguageSymbolType(self: TSLanguage, symbol: TSSymbol): TSSymbolType {
    val metadata = tsLanguageSymbolMetadata(self, symbol)
    return when {
        metadata.named && metadata.visible -> TSSymbolType.Regular
        metadata.visible -> TSSymbolType.Anonymous
        metadata.supertype -> TSSymbolType.Supertype
        else -> TSSymbolType.Auxiliary
    }
}

fun tsLanguageFieldNameForId(self: TSLanguage, id: TSFieldId): String? {
    val count = tsLanguageFieldCount(self)
    return if (count > 0u && id <= count.toUShort()) {
        self.fieldNames[id.toInt() and 0xFFFF]
    } else {
        null
    }
}

fun tsLanguageFieldIdForName(self: TSLanguage, name: String): TSFieldId {
    val count = tsLanguageFieldCount(self).toInt()
    for (i in 1..count) {
        if (self.fieldNames[i] == name) return i.toUShort()
    }
    return 0u
}

fun tsLanguageLookaheads(self: TSLanguage, state: TSStateId): LookaheadIterator {
    val stateValue = state.toInt() and 0xFFFF
    val isSmallState = stateValue >= self.largeStateCount.toInt()
    return if (isSmallState) {
        val index = self.smallParseTableMap[stateValue - self.largeStateCount.toInt()].toInt()
        val groupCount = self.smallParseTable[index]
        LookaheadIterator(
            language = self,
            dataIndex = index + 1,
            groupEnd = index + 2,
            state = state,
            tableValue = 0u,
            sectionIndex = 0u,
            groupCount = groupCount,
            isSmallState = true,
            actions = emptyList(),
            symbol = UShort.MAX_VALUE,
            nextState = 0u,
            actionCount = 0u,
        )
    } else {
        LookaheadIterator(
            language = self,
            dataIndex = stateValue * self.symbolCount.toInt() - 1,
            groupEnd = -1,
            state = state,
            tableValue = 0u,
            sectionIndex = 0u,
            groupCount = 0u,
            isSmallState = false,
            actions = emptyList(),
            symbol = UShort.MAX_VALUE,
            nextState = 0u,
            actionCount = 0u,
        )
    }
}

fun tsLanguageAliasesForSymbol(self: TSLanguage, originalSymbol: TSSymbol): List<TSSymbol> {
    val defaultSlice = listOf(self.publicSymbolMap[originalSymbol.toInt() and 0xFFFF])
    var idx = 0
    while (idx < self.aliasMap.size) {
        val symbol = self.aliasMap[idx]
        idx++
        if (symbol == 0.toUShort() || symbol > originalSymbol) return defaultSlice
        val count = self.aliasMap[idx].toInt()
        idx++
        if (symbol == originalSymbol) {
            val from = idx
            val to = idx + count
            return List(count) { self.aliasMap[from + it] }
                .let { if (it.isEmpty()) defaultSlice else it }
        }
        idx += count
    }
    return defaultSlice
}
