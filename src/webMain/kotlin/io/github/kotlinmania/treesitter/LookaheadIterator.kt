package io.github.kotlinmania.treesitter

actual class LookaheadIterator internal constructor(
    actual val language: Language,
) : AbstractIterator<LookaheadIterator.Symbol>() {
    actual val currentSymbol: UShort get() = UShort.MAX_VALUE
    actual val currentSymbolName: String get() = "ERROR"

    actual fun reset(state: UShort, language: Language?): Boolean = false
    actual override fun next(): Symbol = super.next()
    actual fun symbols(): Sequence<UShort> = emptySequence()
    actual fun symbolNames(): Sequence<String> = emptySequence()
    actual override fun computeNext() {
        done()
    }

    actual class Symbol actual constructor(actual val id: UShort, actual val name: String) {
        actual operator fun component1(): UShort = id
        actual operator fun component2(): String = name
    }
}
