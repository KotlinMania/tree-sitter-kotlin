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
internal fun durationFromMicros(micros: ULong): Duration = micros.toLong().microseconds

internal fun durationToMicros(self: Duration): ULong = self.inWholeMicroseconds.toULong()

internal fun clockNull(): ComparableTimeMark? = null

internal fun clockNow(): ComparableTimeMark? = TimeSource.Monotonic.markNow()

internal fun clockAfter(base: ComparableTimeMark?, duration: Duration): ComparableTimeMark? = base?.plus(duration)

internal fun clockIsNull(self: ComparableTimeMark?): Boolean = self == null

internal fun clockIsGt(self: ComparableTimeMark?, other: ComparableTimeMark?): Boolean =
    self != null && other != null && self > other
