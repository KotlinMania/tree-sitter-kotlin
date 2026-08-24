// port-lint: source lib/src/clock.h
package io.github.kotlinmania.treesitter.lib

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.TimeSource

/**
 * A duration used by the parser to bound elapsed work. The C runtime expressed this as a
 * `uint64_t` count of platform-specific ticks; here it is a [Duration] from kotlin.time.
 */
internal typealias TsDuration = Duration

internal typealias TsClock = ComparableTimeMark?

internal fun durationFromMicros(micros: ULong): TsDuration = micros.toLong().microseconds

internal fun durationToMicros(self: TsDuration): ULong = self.inWholeMicroseconds.toULong()

internal fun clockNull(): TsClock = null

internal fun clockNow(): TsClock = TimeSource.Monotonic.markNow()

internal fun clockAfter(base: TsClock, duration: TsDuration): TsClock = base?.plus(duration)

internal fun clockIsNull(self: TsClock): Boolean = self == null

internal fun clockIsGt(self: TsClock, other: TsClock): Boolean =
    self != null && other != null && self > other
