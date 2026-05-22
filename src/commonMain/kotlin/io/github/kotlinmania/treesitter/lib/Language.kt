// port-lint: source lib/src/language.h
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
