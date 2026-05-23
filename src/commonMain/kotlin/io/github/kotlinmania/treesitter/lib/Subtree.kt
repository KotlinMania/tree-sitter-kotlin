// port-lint: source lib/src/subtree.h
package io.github.kotlinmania.treesitter.lib

const val TS_TREE_STATE_NONE: UShort = UShort.MAX_VALUE

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
