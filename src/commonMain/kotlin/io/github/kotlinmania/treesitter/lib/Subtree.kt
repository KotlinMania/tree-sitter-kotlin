// port-lint: source lib/src/subtree.c
package io.github.kotlinmania.treesitter.lib

const val TS_TREE_STATE_NONE: UShort = UShort.MAX_VALUE
val TS_MAX_INLINE_TREE_LENGTH: UInt = UByte.MAX_VALUE.toUInt()
const val TS_MAX_TREE_POOL_SIZE: Int = 32

/**
 * The serialized state of an external scanner.
 *
 * Every time an external token subtree is created after a call to an external scanner, the
 * scanner's `serialize` function is called to retrieve a serialized copy of its state. The
 * bytes are stored on the subtree so that the scanner's state can be restored later via
 * `deserialize`. The C runtime stored short payloads inline (24 bytes) and long payloads on the
 * heap; the Kotlin port stores all payloads in a single [ByteArray] and lets the GC handle
 * lifetime.
 */
class ExternalScannerState internal constructor(internal val bytes: ByteArray) {
    val length: UInt get() = bytes.size.toUInt()
}

/**
 * A handle to a syntax-tree node.
 *
 * The C runtime expressed [Subtree] as a tagged union: a 24-byte inline payload for small leaves
 * that aren't errors and weren't created by an external scanner, or a heap pointer for parent
 * nodes, errors, external tokens, and other leaves whose payload exceeds the inline budget. The
 * Kotlin port preserves that two-variant shape as a sealed class so callers reason about each
 * case explicitly; GC handles the allocation distinction the inline-vs-heap split was paying for.
 */
sealed class Subtree {

    abstract val isInline: Boolean

    /**
     * Inline-only subtree payload. Mirrors `SubtreeInlineData` (the 24-byte struct the C
     * runtime stamped directly into the [Subtree] union).
     */
    class Inline(
        val symbol: UByte,
        val parseState: UShort,
        val visible: Boolean,
        val named: Boolean,
        val extra: Boolean,
        val hasChanges: Boolean,
        val isMissing: Boolean,
        val isKeyword: Boolean,
        val paddingBytes: UByte,
        val paddingRows: UByte,
        val paddingColumns: UByte,
        val lookaheadBytes: UByte,
        val sizeBytes: UByte,
    ) : Subtree() {
        override val isInline: Boolean = true
    }

    /**
     * Heap-allocated subtree payload. Mirrors `SubtreeHeapData` (the C runtime's full struct for
     * parent nodes, external tokens, errors, and any leaf whose data is too large to fit
     * inline). Children sit in a list rather than the C pattern of allocating them immediately
     * before the heap struct.
     */
    class Heap(
        @Suppress("MemberVisibilityCanBePrivate")
        val children: List<Subtree>,
        var symbol: TSSymbol,
        val parseState: TSStateId,
        val padding: Length,
        val size: Length,
        val lookaheadBytes: UInt,
        val errorCost: UInt,
        val childCount: UInt,
        var visible: Boolean,
        val named: Boolean,
        var extra: Boolean,
        val fragileLeft: Boolean,
        val fragileRight: Boolean,
        val hasChanges: Boolean,
        val hasExternalTokens: Boolean,
        val hasExternalScannerStateChange: Boolean,
        val dependsOnColumn: Boolean,
        val isMissing: Boolean,
        val isKeyword: Boolean,
        val branch: HeapBranch,
    ) : Subtree() {
        override val isInline: Boolean = false
    }

    /**
     * The three branches the C runtime expressed as an anonymous union inside `SubtreeHeapData`:
     * non-terminal parent nodes, external-terminal leaves, and error-terminal leaves. The
     * Kotlin port makes the variant explicit so call sites pattern-match instead of guessing
     * which union arm is live.
     */
    sealed class HeapBranch {
        class NonTerminal(
            val visibleChildCount: UInt,
            val namedChildCount: UInt,
            val visibleDescendantCount: UInt,
            val dynamicPrecedence: Int,
            val repeatDepth: UShort,
            val productionId: UShort,
            val firstLeafSymbol: TSSymbol,
            val firstLeafParseState: TSStateId,
        ) : HeapBranch()

        class ExternalTerminal(val state: ExternalScannerState) : HeapBranch()

        class ErrorTerminal(val lookaheadChar: Int) : HeapBranch()
    }
}

typealias SubtreeArray = MutableList<Subtree>

class SubtreePool internal constructor(
    @Suppress("MemberVisibilityCanBePrivate")
    val freeTrees: MutableList<Subtree> = mutableListOf(),
    @Suppress("MemberVisibilityCanBePrivate")
    val treeStack: MutableList<Subtree> = mutableListOf(),
)

fun tsSubtreeSymbol(self: Subtree): TSSymbol = when (self) {
    is Subtree.Inline -> self.symbol.toUShort()
    is Subtree.Heap -> self.symbol
}

fun tsSubtreeVisible(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.visible
    is Subtree.Heap -> self.visible
}

fun tsSubtreeNamed(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.named
    is Subtree.Heap -> self.named
}

fun tsSubtreeExtra(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.extra
    is Subtree.Heap -> self.extra
}

fun tsSubtreeHasChanges(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.hasChanges
    is Subtree.Heap -> self.hasChanges
}

fun tsSubtreeMissing(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.isMissing
    is Subtree.Heap -> self.isMissing
}

fun tsSubtreeIsKeyword(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> self.isKeyword
    is Subtree.Heap -> self.isKeyword
}

fun tsSubtreeParseState(self: Subtree): TSStateId = when (self) {
    is Subtree.Inline -> self.parseState
    is Subtree.Heap -> self.parseState
}

fun tsSubtreeLookaheadBytes(self: Subtree): UInt = when (self) {
    is Subtree.Inline -> self.lookaheadBytes.toUInt()
    is Subtree.Heap -> self.lookaheadBytes
}

fun tsSubtreeChildren(self: Subtree): List<Subtree> = when (self) {
    is Subtree.Inline -> emptyList()
    is Subtree.Heap -> self.children
}

fun tsSubtreeSetExtra(self: Subtree, isExtra: Boolean) {
    when (self) {
        is Subtree.Inline -> error("inline subtree extra flag is immutable; mutate the parent variant instead")
        is Subtree.Heap -> self.extra = isExtra
    }
}

fun tsSubtreeLeafSymbol(self: Subtree): TSSymbol = when (self) {
    is Subtree.Inline -> self.symbol.toUShort()
    is Subtree.Heap -> when {
        self.childCount == 0u -> self.symbol
        else -> when (val branch = self.branch) {
            is Subtree.HeapBranch.NonTerminal -> branch.firstLeafSymbol
            is Subtree.HeapBranch.ExternalTerminal -> self.symbol
            is Subtree.HeapBranch.ErrorTerminal -> self.symbol
        }
    }
}

fun tsSubtreeLeafParseState(self: Subtree): TSStateId = when (self) {
    is Subtree.Inline -> self.parseState
    is Subtree.Heap -> when {
        self.childCount == 0u -> self.parseState
        else -> when (val branch = self.branch) {
            is Subtree.HeapBranch.NonTerminal -> branch.firstLeafParseState
            is Subtree.HeapBranch.ExternalTerminal -> self.parseState
            is Subtree.HeapBranch.ErrorTerminal -> self.parseState
        }
    }
}

fun tsSubtreePadding(self: Subtree): Length = when (self) {
    is Subtree.Inline -> Length(
        bytes = self.paddingBytes.toUInt(),
        extent = Point(self.paddingRows.toUInt(), self.paddingColumns.toUInt()),
    )
    is Subtree.Heap -> self.padding
}

fun tsSubtreeSize(self: Subtree): Length = when (self) {
    is Subtree.Inline -> Length(
        bytes = self.sizeBytes.toUInt(),
        extent = Point(0u, self.sizeBytes.toUInt()),
    )
    is Subtree.Heap -> self.size
}

fun tsSubtreeTotalSize(self: Subtree): Length =
    lengthAdd(tsSubtreePadding(self), tsSubtreeSize(self))

fun tsSubtreeTotalBytes(self: Subtree): UInt = tsSubtreeTotalSize(self).bytes

fun tsSubtreeChildCount(self: Subtree): UInt = when (self) {
    is Subtree.Inline -> 0u
    is Subtree.Heap -> self.childCount
}

fun tsSubtreeRepeatDepth(self: Subtree): UInt = when (self) {
    is Subtree.Inline -> 0u
    is Subtree.Heap -> (self.branch as? Subtree.HeapBranch.NonTerminal)?.repeatDepth?.toUInt() ?: 0u
}

fun tsSubtreeIsRepetition(self: Subtree): UInt = when (self) {
    is Subtree.Inline -> 0u
    is Subtree.Heap -> if (!self.named && !self.visible && self.childCount != 0u) 1u else 0u
}

fun tsSubtreeVisibleDescendantCount(self: Subtree): UInt = when (self) {
    is Subtree.Inline -> 0u
    is Subtree.Heap -> if (self.childCount == 0u) {
        0u
    } else {
        (self.branch as? Subtree.HeapBranch.NonTerminal)?.visibleDescendantCount ?: 0u
    }
}

fun tsSubtreeVisibleChildCount(self: Subtree): UInt =
    if (tsSubtreeChildCount(self) > 0u && self is Subtree.Heap) {
        (self.branch as? Subtree.HeapBranch.NonTerminal)?.visibleChildCount ?: 0u
    } else {
        0u
    }

fun tsSubtreeErrorCost(self: Subtree): UInt =
    if (tsSubtreeMissing(self)) {
        (ERROR_COST_PER_MISSING_TREE + ERROR_COST_PER_RECOVERY).toUInt()
    } else when (self) {
        is Subtree.Inline -> 0u
        is Subtree.Heap -> self.errorCost
    }

fun tsSubtreeDynamicPrecedence(self: Subtree): Int = when (self) {
    is Subtree.Inline -> 0
    is Subtree.Heap -> if (self.childCount == 0u) {
        0
    } else {
        (self.branch as? Subtree.HeapBranch.NonTerminal)?.dynamicPrecedence ?: 0
    }
}

fun tsSubtreeProductionId(self: Subtree): UShort =
    if (tsSubtreeChildCount(self) > 0u && self is Subtree.Heap) {
        (self.branch as? Subtree.HeapBranch.NonTerminal)?.productionId ?: 0u
    } else {
        0u
    }

fun tsSubtreeFragileLeft(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.fragileLeft
}

fun tsSubtreeFragileRight(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.fragileRight
}

fun tsSubtreeHasExternalTokens(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.hasExternalTokens
}

fun tsSubtreeHasExternalScannerStateChange(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.hasExternalScannerStateChange
}

fun tsSubtreeDependsOnColumn(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.dependsOnColumn
}

fun tsSubtreeIsFragile(self: Subtree): Boolean = when (self) {
    is Subtree.Inline -> false
    is Subtree.Heap -> self.fragileLeft || self.fragileRight
}

/**
 * Construct an [ExternalScannerState] from a raw byte payload. The C runtime decided inline vs
 * heap storage by length; the Kotlin port keeps a single [ByteArray] backing.
 */
fun tsExternalScannerStateInit(data: ByteArray): ExternalScannerState =
    ExternalScannerState(data.copyOf())

fun tsExternalScannerStateData(self: ExternalScannerState): ByteArray = self.bytes

fun tsExternalScannerStateEq(self: ExternalScannerState, buffer: ByteArray, length: UInt): Boolean {
    if (self.length != length) return false
    val n = length.toInt()
    for (i in 0 until n) {
        if (self.bytes[i] != buffer[i]) return false
    }
    return true
}

fun tsSubtreeArrayCopy(self: SubtreeArray, dest: SubtreeArray) {
    dest.clear()
    dest.addAll(self)
}

fun tsSubtreeArrayClear(self: SubtreeArray) {
    self.clear()
}

fun tsSubtreeArrayDelete(self: SubtreeArray) {
    self.clear()
}

/**
 * Move every trailing extra subtree off the end of [self] onto [destination], then reverse
 * [destination] so it reads in original order.
 */
fun tsSubtreeArrayRemoveTrailingExtras(self: SubtreeArray, destination: SubtreeArray) {
    destination.clear()
    while (self.isNotEmpty()) {
        val last = self.last()
        if (tsSubtreeExtra(last)) {
            self.removeAt(self.size - 1)
            destination.add(last)
        } else {
            break
        }
    }
    tsSubtreeArrayReverse(destination)
}

fun tsSubtreeArrayReverse(self: SubtreeArray) {
    val limit = self.size / 2
    for (i in 0 until limit) {
        val reverseIndex = self.size - 1 - i
        val swap = self[i]
        self[i] = self[reverseIndex]
        self[reverseIndex] = swap
    }
}

fun tsSubtreePoolNew(capacity: UInt): SubtreePool =
    SubtreePool(freeTrees = ArrayList(capacity.toInt()), treeStack = ArrayList())

/**
 * Does the subtree fit in the inline 24-byte representation? Faithful port of the size check
 * the C runtime uses to decide whether a newly created subtree can use [Subtree.Inline].
 */
fun tsSubtreeCanInline(padding: Length, size: Length, lookaheadBytes: UInt): Boolean =
    padding.bytes < TS_MAX_INLINE_TREE_LENGTH &&
        padding.extent.row < 16u &&
        padding.extent.column < TS_MAX_INLINE_TREE_LENGTH &&
        size.bytes < TS_MAX_INLINE_TREE_LENGTH &&
        size.extent.row == 0u &&
        size.extent.column < TS_MAX_INLINE_TREE_LENGTH &&
        lookaheadBytes < 16u

/**
 * Construct a leaf subtree. Picks the inline representation when it fits the
 * 24-byte budget (small symbol, modest padding/size/lookahead, no external tokens); otherwise
 * builds a heap representation with the same fields. Faithful port of the `ts_subtree_new_leaf`
 * dispatch in lib/src/subtree.c.
 */
fun tsSubtreeNewLeaf(
    symbol: TSSymbol,
    padding: Length,
    size: Length,
    lookaheadBytes: UInt,
    parseState: TSStateId,
    hasExternalTokens: Boolean,
    dependsOnColumn: Boolean,
    isKeyword: Boolean,
    language: TSLanguage,
): Subtree {
    val metadata = tsLanguageSymbolMetadata(language, symbol)
    val extra = symbol == TS_BUILTIN_SYM_END

    val isInline = symbol <= UByte.MAX_VALUE.toUShort() &&
        !hasExternalTokens &&
        tsSubtreeCanInline(padding, size, lookaheadBytes)

    return if (isInline) {
        Subtree.Inline(
            symbol = symbol.toUByte(),
            parseState = parseState,
            visible = metadata.visible,
            named = metadata.named,
            extra = extra,
            hasChanges = false,
            isMissing = false,
            isKeyword = isKeyword,
            paddingBytes = padding.bytes.toUByte(),
            paddingRows = padding.extent.row.toUByte(),
            paddingColumns = padding.extent.column.toUByte(),
            lookaheadBytes = lookaheadBytes.toUByte(),
            sizeBytes = size.bytes.toUByte(),
        )
    } else {
        Subtree.Heap(
            children = emptyList(),
            symbol = symbol,
            parseState = parseState,
            padding = padding,
            size = size,
            lookaheadBytes = lookaheadBytes,
            errorCost = 0u,
            childCount = 0u,
            visible = metadata.visible,
            named = metadata.named,
            extra = extra,
            fragileLeft = false,
            fragileRight = false,
            hasChanges = false,
            hasExternalTokens = hasExternalTokens,
            hasExternalScannerStateChange = false,
            dependsOnColumn = dependsOnColumn,
            isMissing = false,
            isKeyword = isKeyword,
            branch = Subtree.HeapBranch.NonTerminal(
                visibleChildCount = 0u,
                namedChildCount = 0u,
                visibleDescendantCount = 0u,
                dynamicPrecedence = 0,
                repeatDepth = 0u,
                productionId = 0u,
                firstLeafSymbol = 0u,
                firstLeafParseState = 0u,
            ),
        )
    }
}

/**
 * Re-assign a subtree's symbol and update its visibility/named flags from the language's
 * symbol metadata. Inline subtrees enforce the symbol <= UByte.MAX_VALUE constraint that the C
 * runtime guards with `ts_assert`.
 */
fun tsSubtreeSetSymbol(self: Subtree, symbol: TSSymbol, language: TSLanguage): Subtree {
    val metadata = tsLanguageSymbolMetadata(language, symbol)
    return when (self) {
        is Subtree.Inline -> {
            check(symbol < UByte.MAX_VALUE.toUShort()) {
                "inline subtree symbol $symbol exceeds UByte range"
            }
            Subtree.Inline(
                symbol = symbol.toUByte(),
                parseState = self.parseState,
                visible = metadata.visible,
                named = metadata.named,
                extra = self.extra,
                hasChanges = self.hasChanges,
                isMissing = self.isMissing,
                isKeyword = self.isKeyword,
                paddingBytes = self.paddingBytes,
                paddingRows = self.paddingRows,
                paddingColumns = self.paddingColumns,
                lookaheadBytes = self.lookaheadBytes,
                sizeBytes = self.sizeBytes,
            )
        }
        is Subtree.Heap -> {
            self.symbol = symbol
            self.visible = metadata.visible
            // Heap.named is val; rebuild with the new named flag if it differs from the existing one.
            if (self.named == metadata.named) self else self.copy(named = metadata.named)
        }
    }
}

/**
 * Construct an error leaf. Builds via [tsSubtreeNewLeaf] with the builtin error symbol, then
 * sets fragile_left/right and stamps the [lookaheadChar] onto the ErrorTerminal branch.
 */
fun tsSubtreeNewError(
    lookaheadChar: Int,
    padding: Length,
    size: Length,
    bytesScanned: UInt,
    parseState: TSStateId,
    language: TSLanguage,
): Subtree {
    val base = tsSubtreeNewLeaf(
        symbol = TS_BUILTIN_SYM_ERROR,
        padding = padding,
        size = size,
        lookaheadBytes = bytesScanned,
        parseState = parseState,
        hasExternalTokens = false,
        dependsOnColumn = false,
        isKeyword = false,
        language = language,
    )
    return when (base) {
        is Subtree.Inline -> error("ts_builtin_sym_error never fits the inline budget")
        is Subtree.Heap -> base.copy(
            fragileLeft = true,
            fragileRight = true,
            branch = Subtree.HeapBranch.ErrorTerminal(lookaheadChar = lookaheadChar),
        )
    }
}

/**
 * Subtree.Heap.copy helper that synthesizes a new instance with the requested field overrides.
 * Mirrors the data-class copy() semantics that Subtree.Heap doesn't get because it's a regular
 * class (necessary because two of its fields — symbol/visible/extra — are var, so data-class
 * copy() would expose those vars in the generated copy signature).
 */
private fun Subtree.Heap.copy(
    children: List<Subtree> = this.children,
    symbol: TSSymbol = this.symbol,
    parseState: TSStateId = this.parseState,
    padding: Length = this.padding,
    size: Length = this.size,
    lookaheadBytes: UInt = this.lookaheadBytes,
    errorCost: UInt = this.errorCost,
    childCount: UInt = this.childCount,
    visible: Boolean = this.visible,
    named: Boolean = this.named,
    extra: Boolean = this.extra,
    fragileLeft: Boolean = this.fragileLeft,
    fragileRight: Boolean = this.fragileRight,
    hasChanges: Boolean = this.hasChanges,
    hasExternalTokens: Boolean = this.hasExternalTokens,
    hasExternalScannerStateChange: Boolean = this.hasExternalScannerStateChange,
    dependsOnColumn: Boolean = this.dependsOnColumn,
    isMissing: Boolean = this.isMissing,
    isKeyword: Boolean = this.isKeyword,
    branch: Subtree.HeapBranch = this.branch,
): Subtree.Heap = Subtree.Heap(
    children = children,
    symbol = symbol,
    parseState = parseState,
    padding = padding,
    size = size,
    lookaheadBytes = lookaheadBytes,
    errorCost = errorCost,
    childCount = childCount,
    visible = visible,
    named = named,
    extra = extra,
    fragileLeft = fragileLeft,
    fragileRight = fragileRight,
    hasChanges = hasChanges,
    hasExternalTokens = hasExternalTokens,
    hasExternalScannerStateChange = hasExternalScannerStateChange,
    dependsOnColumn = dependsOnColumn,
    isMissing = isMissing,
    isKeyword = isKeyword,
    branch = branch,
)

/**
 * Subtree-vs-subtree comparison used by the parser for sorting and merging tied parses. The C
 * runtime expressed this with an explicit work stack (`pool->tree_stack`) so deep trees don't
 * blow the call stack; the Kotlin port keeps the same iterative shape with a [MutableList].
 *
 * Returns -1 if [left] sorts before [right], 1 if after, 0 if structurally equal.
 */
fun tsSubtreeCompare(left: Subtree, right: Subtree): Int {
    val workStack: MutableList<Subtree> = mutableListOf(left, right)
    while (workStack.isNotEmpty()) {
        val rightTop = workStack.removeAt(workStack.size - 1)
        val leftTop = workStack.removeAt(workStack.size - 1)
        val result = when {
            tsSubtreeSymbol(leftTop) < tsSubtreeSymbol(rightTop) -> -1
            tsSubtreeSymbol(rightTop) < tsSubtreeSymbol(leftTop) -> 1
            tsSubtreeChildCount(leftTop) < tsSubtreeChildCount(rightTop) -> -1
            tsSubtreeChildCount(rightTop) < tsSubtreeChildCount(leftTop) -> 1
            else -> 0
        }
        if (result != 0) return result

        val leftChildren = tsSubtreeChildren(leftTop)
        val rightChildren = tsSubtreeChildren(rightTop)
        var i = tsSubtreeChildCount(leftTop).toInt()
        while (i > 0) {
            workStack.add(leftChildren[i - 1])
            workStack.add(rightChildren[i - 1])
            i--
        }
    }
    return 0
}

/**
 * Construct a "missing leaf" subtree. The C runtime built one via `ts_subtree_new_leaf` with
 * the requested symbol and then flipped the `is_missing` flag. The Kotlin port preserves that
 * sequence with a small immutable rebuild for the inline variant.
 */
fun tsSubtreeNewMissingLeaf(
    symbol: TSSymbol,
    padding: Length,
    lookaheadBytes: UInt,
    language: TSLanguage,
): Subtree {
    val base = tsSubtreeNewLeaf(
        symbol = symbol,
        padding = padding,
        size = Length.ZERO,
        lookaheadBytes = lookaheadBytes,
        parseState = 0u,
        hasExternalTokens = false,
        dependsOnColumn = false,
        isKeyword = false,
        language = language,
    )
    return when (base) {
        is Subtree.Inline -> Subtree.Inline(
            symbol = base.symbol,
            parseState = base.parseState,
            visible = base.visible,
            named = base.named,
            extra = base.extra,
            hasChanges = base.hasChanges,
            isMissing = true,
            isKeyword = base.isKeyword,
            paddingBytes = base.paddingBytes,
            paddingRows = base.paddingRows,
            paddingColumns = base.paddingColumns,
            lookaheadBytes = base.lookaheadBytes,
            sizeBytes = base.sizeBytes,
        )
        is Subtree.Heap -> base.copy(isMissing = true)
    }
}

/**
 * Walk down the subtree looking for the deepest descendant that carries external-scanner
 * state. Returns null if [tree] has no external tokens at all. Mirrors the recursive walk in
 * `ts_subtree_last_external_token` (lib/src/subtree.c).
 */
fun tsSubtreeLastExternalToken(tree: Subtree): Subtree? {
    if (!tsSubtreeHasExternalTokens(tree)) return null
    var current: Subtree = tree
    while (current is Subtree.Heap && current.childCount > 0u) {
        var found: Subtree? = null
        val children = tsSubtreeChildren(current)
        var i = children.size - 1
        while (i >= 0) {
            val child = children[i]
            if (tsSubtreeHasExternalTokens(child)) {
                found = child
                break
            }
            i--
        }
        if (found == null) return current
        current = found
    }
    return current
}
