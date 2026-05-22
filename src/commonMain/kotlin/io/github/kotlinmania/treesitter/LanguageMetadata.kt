// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * The metadata associated with a language.
 *
 * Currently, this metadata can be used to check the [Semantic Version](https://semver.org/)
 * of the language. This version information should be used to signal if a given parser might
 * be incompatible with existing queries when upgrading between major versions, or minor versions
 * if it's in zerover.
 */
data class LanguageMetadata(
    val majorVersion: UByte,
    val minorVersion: UByte,
    val patchVersion: UByte,
)
