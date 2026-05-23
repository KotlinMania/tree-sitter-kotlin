// port-lint: source lib.rs
package io.github.kotlinmania.treesitter

/**
 * A quantifier for captures.
 */
enum class CaptureQuantifier {
    Zero,
    ZeroOrOne,
    ZeroOrMore,
    One,
    OneOrMore,
}
