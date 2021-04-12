package io.github.durun.nitron.util

import org.joda.time.DateTime
import java.util.*

fun String.parseToDateTime(): DateTime {
    val (day, month, year) = this.split(':').map { it.toInt() }
    val hour = 0
    val min = 0
    check(day in 1..31) { "Day must be in 1..31" }
    check(month in 1..12) { "Month must be in 1..12" }
    return DateTime(year, month, day, hour, min)
}

infix fun Date.isExclusiveIn(range: ClosedRange<DateTime>): Boolean {
    val start = range.start.toDate()
    val end = range.endInclusive.toDate()
    return this.after(start) && this.before(end)
}