package io.github.kotlinmania.treesitter

actual class Language
    @Throws(IllegalArgumentException::class)
    actual constructor(
        language: Any,
    ) {
        actual val abiVersion: UInt get() = throw UnsupportedOperationException("Tree-sitter is not supported on Web")

        @Deprecated("version is deprecated", ReplaceWith("abiVersion"), DeprecationLevel.ERROR)
        actual val version: UInt get() = abiVersion

        actual val symbolCount: UInt get() = 0u
        actual val stateCount: UInt get() = 0u
        actual val fieldCount: UInt get() = 0u
        actual val name: String? get() = null
        actual val metadata: Metadata? get() = null

        @OptIn(ExperimentalUnsignedTypes::class)
        actual val supertypes: UShortArray get() = ushortArrayOf()

        actual fun copy(): Language = this

        actual fun symbolName(symbol: UShort): String? = null

        actual fun symbolForName(name: String, isNamed: Boolean): UShort = 0u

        @OptIn(ExperimentalUnsignedTypes::class)
        actual fun subtypes(supertype: UShort): UShortArray = ushortArrayOf()

        actual fun isNamed(symbol: UShort): Boolean = false

        actual fun isVisible(symbol: UShort): Boolean = false

        actual fun isSupertype(symbol: UShort): Boolean = false

        actual fun fieldNameForId(id: UShort): String? = null

        actual fun fieldIdForName(name: String): UShort = 0u

        actual fun nextState(state: UShort, symbol: UShort): UShort = 0u

        @Throws(IllegalArgumentException::class)
        actual fun lookaheadIterator(state: UShort): LookaheadIterator = throw UnsupportedOperationException("Tree-sitter is not supported on Web")

        @Throws(QueryError::class)
        @Deprecated("Use the Query constructor instead")
        actual fun query(source: String): Query = throw UnsupportedOperationException("Tree-sitter is not supported on Web")

        actual override fun equals(other: Any?): Boolean = other is Language

        actual override fun hashCode(): Int = 0

        actual class Metadata internal actual constructor(
            actual val semanticVersion: Triple<UShort, UShort, UShort>,
        ) {
            actual override fun toString(): String = "${semanticVersion.first}.${semanticVersion.second}.${semanticVersion.third}"
        }
    }
