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
typealias TsDuration = Duration

/**
 * A monotonic clock reading captured at a single point in time. The C runtime stored this as
 * a platform-specific counter value (Windows `LARGE_INTEGER`, POSIX `timespec`); here it is
 * a [ComparableTimeMark] from the platform's monotonic [TimeSource].
 */
typealias TsClock = ComparableTimeMark?

fun durationFromMicros(micros: ULong): TsDuration = micros.toLong().microseconds

fun durationToMicros(self: TsDuration): ULong = self.inWholeMicroseconds.toULong()

fun clockNull(): TsClock = null

fun clockNow(): TsClock = TimeSource.Monotonic.markNow()

fun clockAfter(base: TsClock, duration: TsDuration): TsClock = base?.plus(duration)

fun clockIsNull(self: TsClock): Boolean = self == null

fun clockIsGt(self: TsClock, other: TsClock): Boolean =
    self != null && other != null && self > other
