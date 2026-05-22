// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

class ParseOptions {
    var progressCallback: ((ParseState) -> Boolean)? = null
        private set

    fun progressCallback(callback: (ParseState) -> Boolean): ParseOptions {
        progressCallback = callback
        return this
    }
}
