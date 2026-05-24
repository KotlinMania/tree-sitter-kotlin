// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A class that is used for executing a query.
 *
 * @since 0.25.0
 */
expect class QueryCursor {
    /**
     * The maximum duration in microseconds that query
     * execution should be allowed to take before halting.
     *
     * Default: `0`
     *
     * @since 0.23.0
     */
    @Deprecated("Use the progressCallback in Query.invoke()")
    var timeoutMicros: ULong

    /**
     * The maximum number of in-progress query results.
     *
     * Default: `UInt.MAX_VALUE`
     *
     * @throws [IllegalArgumentException] If the limit is set to `0`.
     */
    var matchLimit: UInt

    /**
     * The maximum start depth for the query.
     *
     * This prevents cursors from exploring children nodes at a certain depth.
     * Note that if a pattern includes many children, then they will still be checked.
     *
     * Default: `UInt.MAX_VALUE`
     */
    var maxStartDepth: UInt

    /**
     * The range of bytes in which the query will be executed.
     *
     * The query cursor will return query results that intersect with
     * the given range. This means that a result may be returned
     * even if some of its captures fall outside the specified range,
     * as long as at least part of that result overlaps with the range.
     *
     * Default: `UInt.MIN_VALUE..UInt.MAX_VALUE`
     *
     * @throws [IllegalArgumentException] If set to an invalid range.
     */
    var byteRange: UIntRange

    /**
     * The range of points in which the query will be executed.
     *
     * The query cursor will return query results that intersect with
     * the given range. This means that a result may be returned
     * even if some of its captures fall outside the specified range,
     * as long as at least part of that result overlaps with the range.
     *
     * Default: `Point.MIN..Point.MAX`
     *
     * @throws [IllegalArgumentException] If set to an invalid range.
     */
    var pointRange: ClosedRange<Point>

    /**
     * Check if the cursor exceeded its maximum number of
     * in-progress query results during its last execution.
     *
     * @see matchLimit
     */
    val didExceedMatchLimit: Boolean

    /**
     * Iterate over all the query results in the order that they were found.
     *
     * #### Example
     *
     * ```kotlin
     * query(tree.rootNode).matches {
     *      if (name != "ieq?") return@matches true
     *      val node = it[(args[0] as QueryPredicateArg.Capture).value].first()
     *      val value = (args[1] as QueryPredicateArg.Literal).value
     *      value.equals(node.text()?.toString(), ignoreCase = true)
     *  }
     * ```
     *
     * @param predicate A function that handles custom predicates.
     */
    fun matches(predicate: QueryPredicate.(QueryMatch) -> Boolean = { true }): Sequence<QueryMatch>

    /**
     * Iterate over all the individual captures in the order that they appear.
     *
     * This is useful if you don't care about _which_ pattern produced the capture.
     *
     * @param predicate A function that handles custom predicates.
     */
    fun captures(
        predicate: QueryPredicate.(QueryMatch) -> Boolean = { true }
    ): Sequence<Pair<UInt, QueryMatch>>
}
