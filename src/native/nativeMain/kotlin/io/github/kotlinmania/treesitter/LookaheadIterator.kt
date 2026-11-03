package io.github.kotlinmania.treesitter

import cnames.structs.TSLookaheadIterator
import io.github.kotlinmania.treesitter.internal.*
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbol
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbolName
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNew
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNext
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorReset
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorResetState
import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner
import io.github.kotlinmania.treesitter.lib.LookaheadIterator as KtLookaheadIterator

/**
 * A class that is used to look up valid symbols in a specific parse state.
 *
 * Lookahead iterators can be useful to generate suggestions and improve syntax
 * error diagnostics. To get symbols valid in an `ERROR` node, use the lookahead
 * iterator on its first leaf node state. For `MISSING` nodes, a lookahead
 * iterator created on the previous non-extra leaf node may be appropriate.
 */
@OptIn(ExperimentalForeignApi::class)
actual class LookaheadIterator
    @Throws(IllegalArgumentException::class)
    internal constructor(
        language: Language,
        private var state: UShort,
    ) : AbstractIterator<LookaheadIterator.Symbol>() {
        private var currentLanguage: Language = language

        private val ktIterator: KtLookaheadIterator? =
            language.ktLanguage?.let {
                tsLookaheadIteratorNew(it, state)
                    ?: throw IllegalArgumentException("State $state is not valid for $language")
            }

        private val self: CPointer<TSLookaheadIterator>? =
            if (ktIterator != null) {
                null
            } else {
                ts_lookahead_iterator_new(language.self, state)
                    ?: throw IllegalArgumentException("State $state is not valid for $language")
            }

        /** The current language of the lookahead iterator. */
        actual val language: Language
            get() =
                if (ktIterator != null) {
                    currentLanguage
                } else {
                    Language(ts_lookahead_iterator_language(self)!!)
                }

        @Suppress("unused")
        @OptIn(ExperimentalNativeApi::class)
        private val cleaner = self?.let { createCleaner(it, ::ts_lookahead_iterator_delete) }

        /**
         * The current symbol ID.
         *
         * The ID of the `ERROR` symbol is equal to `UShort.MAX_VALUE`.
         */
        actual val currentSymbol: UShort
            get() = ktIterator?.let(::tsLookaheadIteratorCurrentSymbol) ?: ts_lookahead_iterator_current_symbol(self)

        /**
         * The current symbol name.
         *
         * Newly created lookahead iterators will contain the `ERROR` symbol.
         */
        actual val currentSymbolName: String
            get() =
                ktIterator?.let { tsLookaheadIteratorCurrentSymbolName(it).orEmpty() }
                    ?: ts_lookahead_iterator_current_symbol_name(self)!!.toKString()

        /**
         * Reset the lookahead iterator the given [state] and, optionally, another [language].
         *
         * @return `true` if the iterator was reset successfully or `false` if it failed.
         */
        actual fun reset(state: UShort, language: Language?): Boolean {
            val iter = ktIterator
            if (iter != null) {
                val ok =
                    if (language == null) {
                        tsLookaheadIteratorResetState(iter, state)
                    } else {
                        val ktLang = language.ktLanguage ?: return false
                        tsLookaheadIteratorReset(iter, ktLang, state).also {
                            if (it) currentLanguage = language
                        }
                    }
                if (ok) this.state = state
                return ok
            }
            val ok =
                if (language == null) {
                    ts_lookahead_iterator_reset_state(self, state)
                } else {
                    ts_lookahead_iterator_reset(self, language.self, state)
                }
            if (ok) this.state = state
            return ok
        }

        /** Advance the lookahead iterator to the next symbol. */
        actual override fun next() = super.next()

        /** Iterate over the symbol IDs. */
        actual fun symbols(): Sequence<UShort> {
            reset(state)
            return sequence {
                while (stepNext()) {
                    yield(currentSymbol)
                }
            }
        }

        /** Iterate over the symbol names. */
        actual fun symbolNames(): Sequence<String> {
            reset(state)
            return sequence {
                while (stepNext()) {
                    yield(currentSymbolName)
                }
            }
        }

        actual override fun computeNext() =
            if (stepNext()) {
                setNext(Symbol(currentSymbol, currentSymbolName))
            } else {
                done()
            }

        operator fun iterator() = apply { reset(state) }

        private fun stepNext(): Boolean =
            ktIterator?.let(::tsLookaheadIteratorNext) ?: ts_lookahead_iterator_next(self)

        /** A class that pairs a symbol ID with its name. */
        actual data class Symbol actual constructor(
            actual val id: UShort,
            actual val name: String,
        )
    }
