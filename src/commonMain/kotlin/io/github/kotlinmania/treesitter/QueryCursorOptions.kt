// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

class QueryCursorOptions {
    var progressCallback: ((QueryCursorState) -> Boolean)? = null
        private set

    fun progressCallback(callback: (QueryCursorState) -> Boolean): QueryCursorOptions {
        progressCallback = callback
        return this
    }
}
