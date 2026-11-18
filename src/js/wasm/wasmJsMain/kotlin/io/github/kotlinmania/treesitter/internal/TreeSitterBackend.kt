package io.github.kotlinmania.treesitter.internal

private val sharedBackend: TreeSitterBackend = PureKotlinTreeSitterBackend()

internal actual fun getTreeSitterBackend(): TreeSitterBackend = sharedBackend
