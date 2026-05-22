// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

class LanguageRef internal constructor(private val language: Language) {
    fun version(): UInt = language.abiVersion

    companion object {
        fun from(language: Language): LanguageRef = LanguageRef(language)
    }
}
