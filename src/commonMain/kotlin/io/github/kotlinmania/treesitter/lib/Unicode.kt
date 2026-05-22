// port-lint: source lib/src/unicode.h
package io.github.kotlinmania.treesitter.lib

const val TS_DECODE_ERROR: Int = -1

/**
 * Decode the next UTF-8 code point from [string] starting at byte [offset]. Returns the number
 * of bytes consumed; the decoded code point is written to [codePoint] (index 0). On a malformed
 * sequence the code point is set to [TS_DECODE_ERROR] and the byte count is the length of the
 * malformed unit (1, 2, or 3) so the lexer can advance past it.
 */
fun tsDecodeUtf8(
    string: ByteArray,
    length: UInt,
    offset: UInt,
    codePoint: IntArray,
): UInt {
    if (offset >= length) {
        codePoint[0] = TS_DECODE_ERROR
        return 0u
    }
    val i = offset.toInt()
    val b0 = string[i].toInt() and 0xFF
    when {
        b0 < 0x80 -> {
            codePoint[0] = b0
            return 1u
        }
        b0 < 0xC2 -> {
            codePoint[0] = TS_DECODE_ERROR
            return 1u
        }
        b0 < 0xE0 -> {
            if (i + 1 >= length.toInt()) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            val b1 = string[i + 1].toInt() and 0xFF
            if (b1 and 0xC0 != 0x80) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            codePoint[0] = (b0 and 0x1F shl 6) or (b1 and 0x3F)
            return 2u
        }
        b0 < 0xF0 -> {
            if (i + 2 >= length.toInt()) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            val b1 = string[i + 1].toInt() and 0xFF
            val b2 = string[i + 2].toInt() and 0xFF
            if (b1 and 0xC0 != 0x80 || b2 and 0xC0 != 0x80) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            val cp = (b0 and 0x0F shl 12) or (b1 and 0x3F shl 6) or (b2 and 0x3F)
            if (cp < 0x800 || cp in 0xD800..0xDFFF) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            codePoint[0] = cp
            return 3u
        }
        b0 < 0xF5 -> {
            if (i + 3 >= length.toInt()) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            val b1 = string[i + 1].toInt() and 0xFF
            val b2 = string[i + 2].toInt() and 0xFF
            val b3 = string[i + 3].toInt() and 0xFF
            if (b1 and 0xC0 != 0x80 || b2 and 0xC0 != 0x80 || b3 and 0xC0 != 0x80) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            val cp = (b0 and 0x07 shl 18) or (b1 and 0x3F shl 12) or
                (b2 and 0x3F shl 6) or (b3 and 0x3F)
            if (cp !in 0x10000..0x10FFFF) {
                codePoint[0] = TS_DECODE_ERROR
                return 1u
            }
            codePoint[0] = cp
            return 4u
        }
        else -> {
            codePoint[0] = TS_DECODE_ERROR
            return 1u
        }
    }
}

private fun isHighSurrogate(unit: Int): Boolean = unit in 0xD800..0xDBFF
private fun isLowSurrogate(unit: Int): Boolean = unit in 0xDC00..0xDFFF

private fun supplementaryFromSurrogates(high: Int, low: Int): Int =
    0x10000 + ((high - 0xD800) shl 10) + (low - 0xDC00)

/**
 * Decode the next UTF-16 little-endian code point from [string] starting at byte [offset].
 * Returns the number of bytes consumed (2 or 4); the decoded code point is written to
 * [codePoint] (index 0).
 */
fun tsDecodeUtf16Le(
    string: ByteArray,
    length: UInt,
    offset: UInt,
    codePoint: IntArray,
): UInt {
    if (offset + 2u > length) {
        codePoint[0] = TS_DECODE_ERROR
        return 0u
    }
    val i = offset.toInt()
    val unit1 = (string[i].toInt() and 0xFF) or ((string[i + 1].toInt() and 0xFF) shl 8)
    if (isHighSurrogate(unit1) && offset + 4u <= length) {
        val unit2 = (string[i + 2].toInt() and 0xFF) or ((string[i + 3].toInt() and 0xFF) shl 8)
        if (isLowSurrogate(unit2)) {
            codePoint[0] = supplementaryFromSurrogates(unit1, unit2)
            return 4u
        }
    }
    codePoint[0] = unit1
    return 2u
}

/**
 * Decode the next UTF-16 big-endian code point from [string] starting at byte [offset]. Returns
 * the number of bytes consumed (2 or 4); the decoded code point is written to [codePoint]
 * (index 0).
 */
fun tsDecodeUtf16Be(
    string: ByteArray,
    length: UInt,
    offset: UInt,
    codePoint: IntArray,
): UInt {
    if (offset + 2u > length) {
        codePoint[0] = TS_DECODE_ERROR
        return 0u
    }
    val i = offset.toInt()
    val unit1 = ((string[i].toInt() and 0xFF) shl 8) or (string[i + 1].toInt() and 0xFF)
    if (isHighSurrogate(unit1) && offset + 4u <= length) {
        val unit2 = ((string[i + 2].toInt() and 0xFF) shl 8) or (string[i + 3].toInt() and 0xFF)
        if (isLowSurrogate(unit2)) {
            codePoint[0] = supplementaryFromSurrogates(unit1, unit2)
            return 4u
        }
    }
    codePoint[0] = unit1
    return 2u
}
