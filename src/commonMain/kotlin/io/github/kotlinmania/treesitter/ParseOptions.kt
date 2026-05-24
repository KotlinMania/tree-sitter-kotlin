// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A function that is called during parsing with the [ParseState].
 *
 * If the function returns `false`, parsing will halt early.
 *
 * @since 0.25.0
 */
fun interface ParseStateProgressCallback {
    operator fun invoke(state: ParseState): Boolean
}

class ParseOptions {
    var progressCallback: ParseStateProgressCallback? = null
        private set

    fun progressCallback(callback: ParseStateProgressCallback): ParseOptions {
        progressCallback = callback
        return this
    }
}
