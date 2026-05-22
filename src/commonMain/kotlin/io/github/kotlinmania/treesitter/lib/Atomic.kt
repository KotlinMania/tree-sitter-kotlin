// port-lint: source lib/src/atomic.h
package io.github.kotlinmania.treesitter.lib

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
fun atomicLoad(p: AtomicLong): Long = p.load()

@OptIn(ExperimentalAtomicApi::class)
fun atomicInc(p: AtomicInt): Int = p.addAndFetch(1)

@OptIn(ExperimentalAtomicApi::class)
fun atomicDec(p: AtomicInt): Int = p.addAndFetch(-1)
