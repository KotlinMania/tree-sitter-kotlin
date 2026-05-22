// port-lint: source lib/src/point.h
package io.github.kotlinmania.treesitter.lib

/**
 * A position in a multi-line text document, in terms of rows and columns. Rows and columns
 * are zero-based.
 */
data class Point(val row: UInt, val column: UInt) {
    companion object {
        val ZERO: Point = Point(0u, 0u)
        val MAX: Point = Point(UInt.MAX_VALUE, UInt.MAX_VALUE)
    }
}

fun pointAdd(a: Point, b: Point): Point =
    if (b.row > 0u) Point(a.row + b.row, b.column)
    else Point(a.row, a.column + b.column)

fun pointSub(a: Point, b: Point): Point =
    if (a.row > b.row) Point(a.row - b.row, a.column)
    else Point(0u, if (a.column >= b.column) a.column - b.column else 0u)

fun pointLte(a: Point, b: Point): Boolean =
    a.row < b.row || (a.row == b.row && a.column <= b.column)

fun pointLt(a: Point, b: Point): Boolean =
    a.row < b.row || (a.row == b.row && a.column < b.column)

fun pointGt(a: Point, b: Point): Boolean =
    a.row > b.row || (a.row == b.row && a.column > b.column)

fun pointGte(a: Point, b: Point): Boolean =
    a.row > b.row || (a.row == b.row && a.column >= b.column)

fun pointEq(a: Point, b: Point): Boolean =
    a.row == b.row && a.column == b.column
