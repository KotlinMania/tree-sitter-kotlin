@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.lib.TSLanguage
import io.github.kotlinmania.treesitter.lib.TSSymbolType
import io.github.kotlinmania.treesitter.lib.tsLanguageMetadataOf
import io.github.kotlinmania.treesitter.lib.tsLanguageSymbolType
import io.github.kotlinmania.treesitterlanguage.LanguageFn
import io.github.kotlinmania.treesitterlanguage.LanguageProvider
import kotlin.native.HiddenFromObjC

private val registeredTsLanguages = mutableMapOf<Long, TSLanguage>()
private var nextTsLanguageId: Long = 0x7FFF_0000_0001L

internal fun registerTsLanguage(language: TSLanguage): Long {
    for ((id, existing) in registeredTsLanguages) {
        if (existing === language) return id
    }
    val id = nextTsLanguageId++
    registeredTsLanguages[id] = language
    return id
}

internal fun lookupTsLanguage(id: Long): TSLanguage? = registeredTsLanguages[id]

internal fun registerLanguageFn(language: TSLanguage): LanguageFn {
    val id = registerTsLanguage(language)
    return LanguageFn.fromRaw(LanguageProvider { id })
}

internal fun unwrapLanguageInput(language: Any): Any =
    when (language) {
        is LanguageFn -> language.intoRaw().call()
        is LanguageProvider -> language.call()
        else -> language
    }

internal fun checkLanguageAbiVersion(abiVersion: UInt) {
    require(abiVersion in MIN_COMPATIBLE_LANGUAGE_VERSION..LANGUAGE_VERSION) {
        "Incompatible language version $abiVersion. " +
            "Must be between $MIN_COMPATIBLE_LANGUAGE_VERSION and $LANGUAGE_VERSION."
    }
}

internal fun tsLanguageMetadataToPublic(self: TSLanguage): Language.Metadata? =
    tsLanguageMetadataOf(self)?.let {
        Language.Metadata(
            Triple(
                it.majorVersion.toUShort(),
                it.minorVersion.toUShort(),
                it.patchVersion.toUShort(),
            ),
        )
    }

internal fun tsLanguageIsNamed(self: TSLanguage, symbol: UShort): Boolean =
    tsLanguageSymbolType(self, symbol) == TSSymbolType.Regular

internal fun tsLanguageIsVisible(self: TSLanguage, symbol: UShort): Boolean =
    tsLanguageSymbolType(self, symbol) <= TSSymbolType.Anonymous

internal fun tsLanguageIsSupertype(self: TSLanguage, symbol: UShort): Boolean =
    tsLanguageSymbolType(self, symbol) == TSSymbolType.Supertype

/**
 * Construct a [Language] from a generated [LanguageFn] descriptor.
 */
@HiddenFromObjC
@Throws(IllegalArgumentException::class)
fun Language(builder: LanguageFn): Language = Language(builder.intoRaw().call())

/**
 * Convert a generated [LanguageFn] descriptor into a [Language].
 */
@HiddenFromObjC
@Throws(IllegalArgumentException::class)
fun LanguageFn.toLanguage(): Language = Language(this)

/**
 * Get the number of distinct node types in this language.
 */
fun Language.nodeKindCount(): UInt = symbolCount

/**
 * Get the number of valid states in this language.
 */
fun Language.parseStateCount(): UInt = stateCount

/**
 * Get the node type for the given numerical id.
 */
fun Language.nodeKindForId(id: UShort): String? = symbolName(id)

/**
 * Get the numerical id for the given node type.
 */
fun Language.idForNodeKind(kind: String, named: Boolean): UShort = symbolForName(kind, named)

/**
 * Check if the node for the given numerical id is named.
 */
fun Language.nodeKindIsNamed(id: UShort): Boolean = isNamed(id)

/**
 * Check if the node for the given numerical id is visible.
 */
fun Language.nodeKindIsVisible(id: UShort): Boolean = isVisible(id)

/**
 * Check if the node for the given numerical id is a supertype.
 */
fun Language.nodeKindIsSupertype(id: UShort): Boolean = isSupertype(id)

/**
 * Get the subtype symbols for the given supertype symbol.
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun Language.subtypesForSupertype(supertypeId: UShort): UShortArray = subtypes(supertypeId)
