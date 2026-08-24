package io.github.kotlinmania.treesitter

actual class QueryCursor internal constructor() {
    @Deprecated("Use the progressCallback in Query.invoke()")
    actual var timeoutMicros: ULong = 0u
    actual var matchLimit: UInt = UInt.MAX_VALUE
    actual var maxStartDepth: UInt = UInt.MAX_VALUE
    actual var byteRange: UIntRange = UInt.MIN_VALUE..UInt.MAX_VALUE
    actual var pointRange: PointRange = Point.MIN..Point.MAX
    actual val didExceedMatchLimit: Boolean get() = false

    actual fun matches(predicate: QueryPredicate.(QueryMatch) -> Boolean): Sequence<QueryMatch> = emptySequence()

    actual fun captures(
        predicate: QueryPredicate.(QueryMatch) -> Boolean,
    ): Sequence<Pair<UInt, QueryMatch>> = emptySequence()
}
