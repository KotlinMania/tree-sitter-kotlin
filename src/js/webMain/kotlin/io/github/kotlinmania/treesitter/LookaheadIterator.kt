package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbol
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbolName
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNew
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNext
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorReset
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorResetState
import io.github.kotlinmania.treesitter.lib.LookaheadIterator as KtLookaheadIterator

actual class LookaheadIterator
    @Throws(IllegalArgumentException::class)
    internal constructor(
        language: Language,
        private var state: UShort,
    ) : AbstractIterator<LookaheadIterator.Symbol>() {
        private var currentLanguage: Language = language

        private val ktIterator: KtLookaheadIterator =
            tsLookaheadIteratorNew(language.ktLanguage, state)
                ?: throw IllegalArgumentException("State $state is not valid for $language")

        actual val language: Language
            get() = currentLanguage

        actual val currentSymbol: UShort
            get() = tsLookaheadIteratorCurrentSymbol(ktIterator)

        actual val currentSymbolName: String
            get() = tsLookaheadIteratorCurrentSymbolName(ktIterator).orEmpty()

        actual fun reset(state: UShort, language: Language?): Boolean {
            val ok =
                if (language == null) {
                    tsLookaheadIteratorResetState(ktIterator, state)
                } else {
                    tsLookaheadIteratorReset(ktIterator, language.ktLanguage, state).also {
                        if (it) currentLanguage = language
                    }
                }
            if (ok) this.state = state
            return ok
        }

        actual override fun next(): Symbol = super.next()

        actual fun symbols(): Sequence<UShort> {
            reset(state)
            return sequence {
                while (stepNext()) {
                    yield(currentSymbol)
                }
            }
        }

        actual fun symbolNames(): Sequence<String> {
            reset(state)
            return sequence {
                while (stepNext()) {
                    yield(currentSymbolName)
                }
            }
        }

        actual override fun computeNext() {
            if (stepNext()) {
                setNext(Symbol(currentSymbol, currentSymbolName))
            } else {
                done()
            }
        }

        operator fun iterator() = apply { reset(state) }

        private fun stepNext(): Boolean = tsLookaheadIteratorNext(ktIterator)

        actual data class Symbol actual constructor(
            actual val id: UShort,
            actual val name: String,
        )
    }
