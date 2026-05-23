// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

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
