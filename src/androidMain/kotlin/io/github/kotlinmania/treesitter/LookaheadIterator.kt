package io.github.kotlinmania.treesitter

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbol
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorCurrentSymbolName
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNew
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorNext
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorReset
import io.github.kotlinmania.treesitter.lib.tsLookaheadIteratorResetState
import io.github.kotlinmania.treesitter.lib.LookaheadIterator as KtLookaheadIterator

/**
 * A class that is used to look up valid symbols in a specific parse state.
 *
 * Lookahead iterators can be useful to generate suggestions and improve syntax
 * error diagnostics. To get symbols valid in an `ERROR` node, use the lookahead
 * iterator on its first leaf node state. For `MISSING` nodes, a lookahead
 * iterator created on the previous non-extra leaf node may be appropriate.
 *
 * __NOTE:__ If you're targeting Android SDK level < 33,
 * you must `use` or [close] the instance to free up resources.
 */
actual class LookaheadIterator
    @Throws(IllegalArgumentException::class)
    internal constructor(
        language: Language,
        private var state: UShort,
    ) : AbstractIterator<LookaheadIterator.Symbol>(),
        AutoCloseable {
        private var currentLanguage: Language = language

        private val ktIterator: KtLookaheadIterator? =
            language.ktLanguage?.let {
                tsLookaheadIteratorNew(it, state)
                    ?: throw IllegalArgumentException("State $state is not valid for $language")
            }

        private val self: Long =
            if (ktIterator != null) {
                0L
            } else {
                init(language.self, state).takeIf { it > 0L }
                    ?: throw IllegalArgumentException("State $state is not valid for $language")
            }

        init {
            if (self != 0L) {
                RefCleaner(this, CleanAction(self))
            }
        }

        /** The current language of the lookahead iterator. */
        actual val language: Language
            get() = if (ktIterator != null) currentLanguage else nativeLanguage()

        /**
         * The current symbol ID.
         *
         * The ID of the `ERROR` symbol is equal to `UShort.MAX_VALUE`.
         */
        @get:JvmName("getCurrentSymbol")
        actual val currentSymbol: UShort
            get() = ktIterator?.let(::tsLookaheadIteratorCurrentSymbol) ?: nativeCurrentSymbol()

        /**
         * The current symbol name.
         *
         * Newly created lookahead iterators will contain the `ERROR` symbol.
         */
        actual val currentSymbolName: String
            get() = ktIterator?.let { tsLookaheadIteratorCurrentSymbolName(it).orEmpty() } ?: nativeCurrentSymbolName()

        /**
         * Reset the lookahead iterator the given [state] and, optionally, another [language].
         *
         * @return `true` if the iterator was reset successfully or `false` if it failed.
         */
        @JvmName("reset")
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
            return nativeReset(state, language).also { if (it) this.state = state }
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

        override fun close() {
            if (self != 0L) {
                delete(self)
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
            ktIterator?.let(::tsLookaheadIteratorNext) ?: nativeNext()

        @FastNative
        private external fun nativeLanguage(): Language

        @FastNative
        @JvmName("nativeCurrentSymbol")
        private external fun nativeCurrentSymbol(): UShort

        @FastNative
        private external fun nativeCurrentSymbolName(): String

        @FastNative
        @JvmName("nativeReset")
        private external fun nativeReset(state: UShort, language: Language?): Boolean

        @FastNative
        private external fun nativeNext(): Boolean

        /** A class that pairs a symbol ID with its name. */
        actual data class Symbol actual constructor(
            actual val id: UShort,
            actual val name: String,
        )

        private class CleanAction(
            private val ptr: Long,
        ) : Runnable {
            override fun run() = delete(ptr)
        }

        private companion object {
            @JvmStatic
            @JvmName("init")
            @CriticalNative
            private external fun init(language: Long, state: UShort): Long

            @JvmStatic
            @CriticalNative
            private external fun delete(self: Long)
        }
    }
