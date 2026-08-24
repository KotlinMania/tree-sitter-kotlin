package io.github.kotlinmania.treesitter

actual class Query @Throws(QueryError::class) actual constructor(language: Language, source: String) {
    actual val patternCount: UInt get() = 0u
    @Deprecated("captureCount is deprecated.", ReplaceWith("captureNames.size"))
    actual val captureCount: UInt get() = 0u
    actual val captureNames: List<String> get() = emptyList()
    actual val stringValues: List<String> get() = emptyList()

    actual operator fun invoke(node: Node, progressCallback: QueryProgressCallback?): QueryCursor =
        throw UnsupportedOperationException("Tree-sitter is not supported on Web")

    actual fun settings(index: UInt): Map<String, String?> = emptyMap()

    @Throws(IndexOutOfBoundsException::class)
    actual fun assertions(index: UInt): Map<String, Pair<String?, Boolean>> = emptyMap()

    @Throws(IndexOutOfBoundsException::class)
    actual fun disablePattern(index: UInt) {}

    actual fun disableCapture(name: String) {}

    actual fun startByteForPattern(index: UInt): UInt = 0u

    @Throws(IndexOutOfBoundsException::class)
    actual fun endByteForPattern(index: UInt): UInt = 0u

    @Throws(IndexOutOfBoundsException::class)
    actual fun isPatternRooted(index: UInt): Boolean = false

    @Throws(IndexOutOfBoundsException::class)
    actual fun isPatternNonLocal(index: UInt): Boolean = false

    @Throws(IndexOutOfBoundsException::class)
    actual fun isPatternGuaranteedAtStep(offset: UInt): Boolean = false
}
