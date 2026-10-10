package io.github.kotlinmania.treesitter

actual class Parser actual constructor() {
    actual constructor(language: Language) : this() {
        this.language = language
    }

    actual var language: Language? = null

    @set:Throws(IllegalArgumentException::class)
    actual var includedRanges: List<Range> = emptyList()

    @Deprecated("Use the progressCallback in parse()")
    actual var timeoutMicros: ULong = 0u

    @get:Deprecated("Don't call the logger directly.", level = DeprecationLevel.HIDDEN)
    actual var logger: LogFunction? = null

    @Throws(IllegalStateException::class)
    actual fun parse(source: String, encoding: InputEncoding, oldTree: Tree?): Tree =
        throw UnsupportedOperationException("Tree-sitter is not supported on Web")

    @Throws(IllegalStateException::class)
    actual fun parse(
        encoding: InputEncoding,
        oldTree: Tree?,
        progressCallback: ParseProgressCallback?,
        readCallback: ParseReadCallback,
    ): Tree = throw UnsupportedOperationException("Tree-sitter is not supported on Web")

    actual fun reset() {}

    override fun toString(): String = "Parser(language=$language)"

    actual enum class LogType { LEX, PARSE }
}
