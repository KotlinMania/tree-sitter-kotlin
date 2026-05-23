// port-lint: source lib/src/stack.h
package io.github.kotlinmania.treesitter.lib

typealias StackVersion = UInt

val STACK_VERSION_NONE: StackVersion = UInt.MAX_VALUE

data class StackSlice(val subtrees: List<Subtree>, val version: StackVersion)

typealias StackSliceArray = List<StackSlice>

data class StackSummaryEntry(
    val position: Length,
    val depth: UInt,
    val state: TSStateId,
)

typealias StackSummary = List<StackSummaryEntry>
