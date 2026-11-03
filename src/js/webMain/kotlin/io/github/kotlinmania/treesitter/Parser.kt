package io.github.kotlinmania.treesitter

import io.github.kotlinmania.treesitter.internal.getTreeSitterBackend

actual class Parser actual constructor() {
    actual constructor(language: Language) : this() {
        this.language = language
    }

    private val backend = getTreeSitterBackend()
    private val self = backend.createParser()

    actual var language: Language? = null
        set(value) {
            field = value
            if (value != null) {
                backend.setLanguage(self, value.self)
            }
        }

    @set:Throws(IllegalArgumentException::class)
    actual var includedRanges: List<Range> = emptyList()

    @Deprecated("Use the progressCallback in parse()")
    actual var timeoutMicros: ULong = 0u
        set(value) {
            field = value
            backend.setTimeoutMicros(self, value)
        }

    @get:Deprecated("Don't call the logger directly.", level = DeprecationLevel.HIDDEN)
    actual var logger: LogFunction? = null

    @Throws(IllegalStateException::class)
    actual fun parse(source: String, encoding: InputEncoding, oldTree: Tree?): Tree {
        val lang = language ?: throw IllegalStateException("Parser has no language assigned")
        val treeId = backend.parseString(self, lang, oldTree?.self ?: 0L, source)
        return Tree(treeId, source, lang)
    }

    @Throws(IllegalStateException::class)
    actual fun parse(
        encoding: InputEncoding,
        oldTree: Tree?,
        progressCallback: ParseProgressCallback?,
        readCallback: ParseReadCallback,
    ): Tree {
        val lang = language ?: throw IllegalStateException("Parser has no language assigned")
        val sb = StringBuilder()
        var offset = 0u
        var row = 0u
        var col = 0u
        while (true) {
            val chunk = readCallback(offset, Point(row, col)) ?: break
            if (chunk.isEmpty()) break
            sb.append(chunk)
            offset += chunk.length.toUInt()
            val lines = chunk.split('\n')
            if (lines.size > 1) {
                row += (lines.size - 1).toUInt()
                col = lines.last().length.toUInt()
            } else {
                col += chunk.length.toUInt()
            }
            if (progressCallback != null && !progressCallback(offset, false)) {
                throw IllegalStateException("Parsing halted by progress callback")
            }
        }
        val source = sb.toString()
        val treeId = backend.parseString(self, lang, oldTree?.self ?: 0L, source)
        return Tree(treeId, source, lang)
    }

    actual fun reset() {
        backend.resetParser(self)
    }

    override fun toString(): String = "Parser(language=$language)"

    actual enum class LogType { LEX, PARSE }
}
