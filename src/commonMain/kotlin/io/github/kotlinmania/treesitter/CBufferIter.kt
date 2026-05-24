// port-lint: source util.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.treesitter

import kotlin.native.HiddenFromObjC

@HiddenFromObjC
class CBufferIter<T> internal constructor(
    private val items: List<T>,
    private val release: (() -> Unit)? = null,
) : Iterator<T>, AutoCloseable {

    private var index: Int = 0
    private var closed: Boolean = false

    val count: Int get() = items.size

    override fun hasNext(): Boolean = !closed && index < items.size

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        val value = items[index]
        index += 1
        return value
    }

    fun sizeHint(): Pair<Int, Int?> {
        val remaining = if (closed) 0 else items.size - index
        return remaining to remaining
    }

    override fun close() {
        if (!closed) {
            closed = true
            release?.invoke()
        }
    }

    internal fun drop() = close()

    internal companion object {
        fun <T> new(items: List<T>, release: (() -> Unit)? = null): CBufferIter<T> =
            CBufferIter(items, release)
    }
}
