// port-lint: source lib/src/length.h
package io.github.kotlinmania.treesitter.lib

data class Length(val bytes: UInt, val extent: Point) {
    companion object {
        val UNDEFINED: Length = Length(0u, Point(0u, 1u))
        val MAX: Length = Length(UInt.MAX_VALUE, Point(UInt.MAX_VALUE, UInt.MAX_VALUE))
        val ZERO: Length = Length(0u, Point(0u, 0u))
    }
}

fun lengthIsUndefined(length: Length): Boolean =
    length.bytes == 0u && length.extent.column != 0u

fun lengthMin(len1: Length, len2: Length): Length =
    if (len1.bytes < len2.bytes) len1 else len2

fun lengthAdd(len1: Length, len2: Length): Length =
    Length(len1.bytes + len2.bytes, pointAdd(len1.extent, len2.extent))

fun lengthSub(len1: Length, len2: Length): Length {
    val bytes = if (len1.bytes >= len2.bytes) len1.bytes - len2.bytes else 0u
    return Length(bytes, pointSub(len1.extent, len2.extent))
}

fun lengthSaturatingSub(len1: Length, len2: Length): Length =
    if (len1.bytes > len2.bytes) lengthSub(len1, len2) else Length.ZERO
