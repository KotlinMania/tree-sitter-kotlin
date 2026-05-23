// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

class LossyUtf8(bytes: ByteArray) : AbstractIterator<String>() {

    private var bytes: ByteArray = bytes
    private var inReplacement: Boolean = false

    override fun computeNext() {
        if (bytes.isEmpty()) {
            done()
            return
        }
        if (inReplacement) {
            inReplacement = false
            setNext("�")
            return
        }
        val firstInvalid = firstInvalidUtf8Index(bytes)
        if (firstInvalid < 0) {
            val valid = bytes.decodeToString()
            bytes = ByteArray(0)
            setNext(valid)
            return
        }
        val errorLen = invalidSequenceLength(bytes, firstInvalid)
        if (errorLen <= 0) {
            done()
            return
        }
        if (firstInvalid > 0) {
            val result = bytes.copyOfRange(0, firstInvalid).decodeToString()
            bytes = bytes.copyOfRange(firstInvalid + errorLen, bytes.size)
            inReplacement = true
            setNext(result)
        } else {
            bytes = bytes.copyOfRange(errorLen, bytes.size)
            setNext("�")
        }
    }

    companion object {
        private fun firstInvalidUtf8Index(bytes: ByteArray): Int {
            var i = 0
            while (i < bytes.size) {
                val width = utf8SequenceWidth(bytes, i)
                if (width <= 0) return i
                i += width
            }
            return -1
        }

        private fun invalidSequenceLength(bytes: ByteArray, start: Int): Int {
            val first = bytes[start].toInt() and 0xFF
            val expectedLen = when {
                first and 0x80 == 0x00 -> 1
                first and 0xE0 == 0xC0 -> 2
                first and 0xF0 == 0xE0 -> 3
                first and 0xF8 == 0xF0 -> 4
                else -> 1
            }
            val available = minOf(expectedLen, bytes.size - start)
            var consumed = 1
            while (consumed < available) {
                val b = bytes[start + consumed].toInt() and 0xFF
                if (b and 0xC0 != 0x80) break
                consumed++
            }
            return consumed
        }

        private fun utf8SequenceWidth(bytes: ByteArray, offset: Int): Int {
            val first = bytes[offset].toInt() and 0xFF
            val width = when {
                first and 0x80 == 0x00 -> 1
                first and 0xE0 == 0xC0 -> 2
                first and 0xF0 == 0xE0 -> 3
                first and 0xF8 == 0xF0 -> 4
                else -> return -1
            }
            if (offset + width > bytes.size) return -1
            for (k in 1 until width) {
                val b = bytes[offset + k].toInt() and 0xFF
                if (b and 0xC0 != 0x80) return -1
            }
            if (width == 3) {
                val cp = decodeCodePoint(bytes, offset, width)
                if (cp in 0xD800..0xDFFF) return -1
            }
            if (width == 4) {
                val cp = decodeCodePoint(bytes, offset, width)
                if (cp > 0x10FFFF) return -1
            }
            return width
        }

        private fun decodeCodePoint(bytes: ByteArray, offset: Int, width: Int): Int {
            val first = bytes[offset].toInt() and 0xFF
            var cp = when (width) {
                2 -> first and 0x1F
                3 -> first and 0x0F
                4 -> first and 0x07
                else -> first
            }
            for (k in 1 until width) {
                cp = (cp shl 6) or (bytes[offset + k].toInt() and 0x3F)
            }
            return cp
        }
    }
}
