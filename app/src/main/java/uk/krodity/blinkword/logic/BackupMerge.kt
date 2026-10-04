package uk.krodity.blinkword.logic

import uk.krodity.blinkword.data.ReadingDay

/**
 * Merges backed-up reading days into what's already on the device, taking the
 * higher figure per day. Restoring onto an empty library gives back exactly
 * what was saved; restoring onto a device that has also been read on keeps the
 * better day; and re-importing the same file twice changes nothing.
 */
fun mergeReadingDays(existing: List<ReadingDay>, incoming: List<ReadingDay>): List<ReadingDay> {
    val byDate = existing.associateBy { it.date }

    return incoming.map { day ->
        val current = byDate[day.date] ?: return@map day
        ReadingDay(
            date = day.date,
            wordsRead = maxOf(current.wordsRead, day.wordsRead),
            millisRead = maxOf(current.millisRead, day.millisRead),
            maxWpm = maxOf(current.maxWpm, day.maxWpm),
        )
    }
}

/**
 * Reading position from a backup only wins when it's further along, so
 * restoring an older backup never rewinds a book you've since read more of.
 */
fun mergeProgress(currentIndex: Int, backupIndex: Int): Int = maxOf(currentIndex, backupIndex)
