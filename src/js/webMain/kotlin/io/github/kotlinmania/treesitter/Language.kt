package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.lib.TSLanguage
import io.github.kotlinmania.treesitter.lib.tsLanguageAbiVersion
import io.github.kotlinmania.treesitter.lib.tsLanguageFieldCount
import io.github.kotlinmania.treesitter.lib.tsLanguageFieldIdForName
import io.github.kotlinmania.treesitter.lib.tsLanguageFieldNameForId
import io.github.kotlinmania.treesitter.lib.tsLanguageName
import io.github.kotlinmania.treesitter.lib.tsLanguageNextState
import io.github.kotlinmania.treesitter.lib.tsLanguageStateCount
import io.github.kotlinmania.treesitter.lib.tsLanguageSubtypes
import io.github.kotlinmania.treesitter.lib.tsLanguageSupertypes
import io.github.kotlinmania.treesitter.lib.tsLanguageSymbolCount
import io.github.kotlinmania.treesitter.lib.tsLanguageSymbolForName
import io.github.kotlinmania.treesitter.lib.tsLanguageSymbolName

actual class Language
    @Throws(IllegalArgumentException::class)
    actual constructor(
        language: Any,
    ) {
        internal val ktLanguage: TSLanguage
        internal val self: Long

        init {
            when (val unwrapped = unwrapLanguageInput(language)) {
                is Language -> {
                    ktLanguage = unwrapped.ktLanguage
                    self = unwrapped.self
                }

                is TSLanguage -> {
                    ktLanguage = unwrapped
                    self = registerTsLanguage(unwrapped)
                }

                is Long -> {
                    if (unwrapped <= 0L) {
                        throw IllegalArgumentException("Invalid language: $language")
                    }
                    val registered =
                        lookupTsLanguage(unwrapped)
                            ?: throw IllegalArgumentException("Invalid language: $language")
                    ktLanguage = registered
                    self = unwrapped
                }

                else -> {
                    throw IllegalArgumentException("Invalid language: $language")
                }
            }
            checkLanguageAbiVersion(abiVersion)
        }

        actual val abiVersion: UInt
            get() = tsLanguageAbiVersion(ktLanguage)

        @Deprecated("version is deprecated", ReplaceWith("abiVersion"), DeprecationLevel.ERROR)
        actual val version: UInt
            get() = abiVersion

        actual val symbolCount: UInt
            get() = tsLanguageSymbolCount(ktLanguage)

        actual val stateCount: UInt
            get() = tsLanguageStateCount(ktLanguage)

        actual val fieldCount: UInt
            get() = tsLanguageFieldCount(ktLanguage)

        actual val name: String?
            get() = tsLanguageName(ktLanguage)

        actual val metadata: Metadata?
            get() = tsLanguageMetadataToPublic(ktLanguage)

        @OptIn(ExperimentalUnsignedTypes::class)
        actual val supertypes: UShortArray
            get() = tsLanguageSupertypes(ktLanguage)

        actual fun copy(): Language = Language(ktLanguage)

        actual fun symbolName(symbol: UShort): String? = tsLanguageSymbolName(ktLanguage, symbol)

        actual fun symbolForName(name: String, isNamed: Boolean): UShort =
            tsLanguageSymbolForName(ktLanguage, name, isNamed)

        @OptIn(ExperimentalUnsignedTypes::class)
        actual fun subtypes(supertype: UShort): UShortArray = tsLanguageSubtypes(ktLanguage, supertype)

        actual fun isNamed(symbol: UShort): Boolean = tsLanguageIsNamed(ktLanguage, symbol)

        actual fun isVisible(symbol: UShort): Boolean = tsLanguageIsVisible(ktLanguage, symbol)

        actual fun isSupertype(symbol: UShort): Boolean = tsLanguageIsSupertype(ktLanguage, symbol)

        actual fun fieldNameForId(id: UShort): String? = tsLanguageFieldNameForId(ktLanguage, id)

        actual fun fieldIdForName(name: String): UShort = tsLanguageFieldIdForName(ktLanguage, name)

        actual fun nextState(state: UShort, symbol: UShort): UShort =
            tsLanguageNextState(ktLanguage, state, symbol)

        @Throws(IllegalArgumentException::class)
        actual fun lookaheadIterator(state: UShort): LookaheadIterator = LookaheadIterator(this, state)

        @Throws(QueryError::class)
        @Deprecated("Use the Query constructor instead")
        actual fun query(source: String): Query = Query(this, source)

        actual override fun equals(other: Any?): Boolean =
            this === other || (other is Language && self == other.self)

        actual override fun hashCode(): Int = self.hashCode()

        override fun toString(): String = "Language(id=0x${self.toString(16)}, abiVersion=$abiVersion)"

        @ConsistentCopyVisibility
        actual data class Metadata internal actual constructor(
            actual val semanticVersion: Triple<UShort, UShort, UShort>,
        ) {
            actual override fun toString(): String =
                buildString {
                    append("Metadata(semanticVersion=\"")
                    append(semanticVersion.first)
                    append('.')
                    append(semanticVersion.second)
                    append('.')
                    append(semanticVersion.third)
                    append("\")")
                }
        }
    }
