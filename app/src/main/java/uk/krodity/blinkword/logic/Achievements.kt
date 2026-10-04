package uk.krodity.blinkword.logic

enum class AchievementId { FIRST_STEPS, SPEED_DEMON, BOOKWORM, STREAK_MASTER, MARATHON, CENTURION }

data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

/** Every achievement, in display order, with its unlocked state resolved from [stats]. */
fun achievements(stats: ReadingStats): List<Achievement> = listOf(
    Achievement(
        id = AchievementId.FIRST_STEPS,
        title = "First Steps",
        description = "Read your first words",
        unlocked = stats.allTimeWords > 0,
    ),
    Achievement(
        id = AchievementId.SPEED_DEMON,
        title = "Speed Demon",
        description = "Read at 600 WPM or faster",
        unlocked = stats.maxWpm >= 600,
    ),
    Achievement(
        id = AchievementId.BOOKWORM,
        title = "Bookworm",
        description = "Finish a book",
        unlocked = stats.booksFinished >= 1,
    ),
    Achievement(
        id = AchievementId.STREAK_MASTER,
        title = "Streak Master",
        description = "Read 7 days in a row",
        unlocked = stats.bestStreak >= 7,
    ),
    Achievement(
        id = AchievementId.MARATHON,
        title = "Marathon",
        description = "Read 10,000 words in one day",
        unlocked = stats.bestDayWords >= 10_000,
    ),
    Achievement(
        id = AchievementId.CENTURION,
        title = "Centurion",
        description = "Read 100,000 words in total",
        unlocked = stats.allTimeWords >= 100_000,
    ),
)
