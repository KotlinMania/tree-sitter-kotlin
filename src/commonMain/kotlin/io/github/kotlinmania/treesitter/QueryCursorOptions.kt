// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A function that is called during query execution with the [QueryCursorState].
 *
 * If the function returns `false`, the execution will halt early.
 *
 * @since 0.25.0
 */
fun interface QueryCursorStateProgressCallback {
    operator fun invoke(state: QueryCursorState): Boolean
}

class QueryCursorOptions {
    var progressCallback: QueryCursorStateProgressCallback? = null
        private set

    fun progressCallback(callback: QueryCursorStateProgressCallback): QueryCursorOptions {
        progressCallback = callback
        return this
    }
}
