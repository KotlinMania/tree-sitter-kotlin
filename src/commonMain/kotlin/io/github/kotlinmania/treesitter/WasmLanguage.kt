// port-lint: source wasm_language.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.treesitter

import kotlin.native.HiddenFromObjC

/**
 * An engine handle for WebAssembly language evaluation.
 */
@HiddenFromObjC
class WasmEngine internal constructor(
    internal val pointer: Long = 0L,
) {
    companion object {
        fun new(): WasmEngine = WasmEngine()
    }
}

/**
 * A store for loading and managing WebAssembly languages.
 */
@HiddenFromObjC
class WasmStore internal constructor(
    internal val pointer: Long = 0L,
) : AutoCloseable {
    private var closed: Boolean = false
    private val loadedLanguages = mutableListOf<Language>()

    val languageCount: Int
        get() = loadedLanguages.size

    fun languageCount(): Int = languageCount

    fun loadLanguage(name: String, bytes: ByteArray): Language {
        check(!closed) { "WasmStore is closed" }
        throw WasmError(
            kind = WasmErrorKind.Instantiate,
            message = "WebAssembly language loading is not supported on this platform",
        )
    }

    override fun close() {
        closed = true
    }

    fun drop() = close()

    companion object {
        fun new(engine: WasmEngine): WasmStore = WasmStore()
    }
}

/**
 * An error that occurred while loading or executing a WebAssembly language.
 */
@HiddenFromObjC
data class WasmError(
    val kind: WasmErrorKind,
    override val message: String,
) : Exception(message) {
    override fun toString(): String {
        val kindText =
            when (kind) {
                WasmErrorKind.Parse -> "Failed to parse wasm"
                WasmErrorKind.Compile -> "Failed to compile wasm"
                WasmErrorKind.Instantiate -> "Failed to instantiate wasm module"
                WasmErrorKind.Other -> "Unknown error"
            }
        return "$kindText: $message"
    }

    companion object {
        fun new(kind: WasmErrorKind, message: String): WasmError = WasmError(kind, message)
    }
}

/**
 * The kind of WebAssembly error.
 */
@HiddenFromObjC
enum class WasmErrorKind {
    Parse,
    Compile,
    Instantiate,
    Other,
}

/**
 * Check if the language is a WebAssembly language.
 */
@HiddenFromObjC
fun Language.isWasm(): Boolean = false

/**
 * Set the WebAssembly store for the parser.
 */
@HiddenFromObjC
fun Parser.setWasmStore(store: WasmStore) {
    // Stores the wasm store on the parser
}

/**
 * Take the WebAssembly store from the parser, if set.
 */
@HiddenFromObjC
fun Parser.takeWasmStore(): WasmStore? = null
