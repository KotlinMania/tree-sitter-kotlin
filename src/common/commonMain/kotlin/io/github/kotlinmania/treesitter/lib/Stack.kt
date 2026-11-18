// port-lint: source lib/src/stack.h
package io.github.kotlinmania.treesitter.lib

internal const val STACK_VERSION_NONE: UInt = UInt.MAX_VALUE

internal data class StackSlice(
    val subtrees: List<Subtree>,
    val version: UInt,
)

internal data class StackSummaryEntry(
    val position: Length,
    val depth: UInt,
    val state: UShort,
)
