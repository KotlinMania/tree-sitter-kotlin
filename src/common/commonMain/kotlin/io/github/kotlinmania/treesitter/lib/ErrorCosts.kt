// port-lint: source lib/src/error_costs.h
package io.github.kotlinmania.treesitter.lib

internal const val ERROR_STATE: UShort = 0u
internal const val ERROR_COST_PER_RECOVERY: Int = 500
internal const val ERROR_COST_PER_MISSING_TREE: Int = 110
internal const val ERROR_COST_PER_SKIPPED_TREE: Int = 100
internal const val ERROR_COST_PER_SKIPPED_LINE: Int = 30
internal const val ERROR_COST_PER_SKIPPED_CHAR: Int = 1
