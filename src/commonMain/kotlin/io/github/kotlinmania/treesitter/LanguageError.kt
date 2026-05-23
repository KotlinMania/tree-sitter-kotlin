// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * An error that occurred when trying to assign an incompatible [Language] to a [Parser].
 */
class LanguageError internal constructor(internal val version: ULong) {
    override fun equals(other: Any?): Boolean = other is LanguageError && other.version == version
    override fun hashCode(): Int = version.hashCode()
    override fun toString(): String = "LanguageError(version=$version)"
}
