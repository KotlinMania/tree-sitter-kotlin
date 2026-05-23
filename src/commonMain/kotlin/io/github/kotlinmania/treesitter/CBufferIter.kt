// port-lint: source util.rs
package io.github.kotlinmania.treesitter

class CBufferIter<T> internal constructor(
    private val items: List<T>,
) : Iterator<T> {

    private var index: Int = 0

    val count: Int get() = items.size

    override fun hasNext(): Boolean = index < items.size

    override fun next(): T {
        if (index >= items.size) throw NoSuchElementException()
        val value = items[index]
        index += 1
        return value
    }

    fun sizeHint(): Pair<Int, Int?> {
        val remaining = items.size - index
        return remaining to remaining
    }
}
