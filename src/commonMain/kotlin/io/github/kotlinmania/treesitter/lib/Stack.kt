// port-lint: source lib/src/stack.h
package io.github.kotlinmania.treesitter.lib

internal typealias StackVersion = UInt

internal val STACK_VERSION_NONE: StackVersion = UInt.MAX_VALUE

internal data class StackSlice(
    val subtrees: List<Subtree>,
    val version: StackVersion,
)

internal typealias StackSliceArray = List<StackSlice>

internal data class StackSummaryEntry(
    val position: Length,
    val depth: UInt,
    val state: TSStateId,
)

internal typealias StackSummary = List<StackSummaryEntry>
