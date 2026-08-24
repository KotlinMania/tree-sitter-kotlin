// port-lint: source lib/src/reduce_action.h
package io.github.kotlinmania.treesitter.lib

internal data class ReduceAction(
    val count: UInt,
    val symbol: UShort,
    val dynamicPrecedence: Int,
    val productionId: UShort,
)

internal typealias ReduceActionSet = MutableList<ReduceAction>

internal fun tsReduceActionSetAdd(self: ReduceActionSet, newAction: ReduceAction) {
    for (action in self) {
        if (action.symbol == newAction.symbol && action.count == newAction.count) return
    }
    self.add(newAction)
}
