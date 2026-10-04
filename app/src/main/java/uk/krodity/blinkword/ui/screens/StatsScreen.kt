package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.logic.Achievement
import uk.krodity.blinkword.logic.AchievementId
import uk.krodity.blinkword.logic.DayBar
import uk.krodity.blinkword.logic.ReadingStats
import uk.krodity.blinkword.logic.formatCompactNumber
import uk.krodity.blinkword.logic.formatDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    stats: ReadingStats,
    achievements: List<Achievement>,
    dailyGoal: Int,
    onSetDailyGoal: (Int) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 16.dp),
            )

            TodayCard(
                stats = stats,
                dailyGoal = dailyGoal,
                onEditGoal = { showGoalDialog = true },
            )

            Spacer(modifier = Modifier.height(16.dp))
            StreakCard(current = stats.currentStreak, best = stats.bestStreak)

            Spacer(modifier = Modifier.height(16.dp))
            WeekCard(week = stats.week, weekWords = stats.weekWords)

            Spacer(modifier = Modifier.height(16.dp))
            AllTimeCard(stats)

            Spacer(modifier = Modifier.height(16.dp))
            AchievementsCard(achievements)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            current = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = {
                onSetDailyGoal(it)
                showGoalDialog = false
            },
        )
    }
}

@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun TodayCard(stats: ReadingStats, dailyGoal: Int, onEditGoal: () -> Unit) {
    StatsCard {
        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GoalRing(
                words = stats.todayWords,
                goal = dailyGoal,
                onEditGoal = onEditGoal,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = Icons.Filled.Schedule,
                tint = TimeTint,
                value = formatDuration(stats.todayMillis),
                label = "Reading",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.Bolt,
                tint = SpeedTint,
                value = stats.todayWpm.toString(),
                label = "Avg WPM",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tint = BooksTint,
                value = stats.booksFinished.toString(),
                label = "Books",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalRing(words: Int, goal: Int, onEditGoal: () -> Unit) {
    val fraction = if (goal <= 0) 0f else (words.toFloat() / goal).coerceIn(0f, 1f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            if (fraction > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatCompactNumber(words),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "words today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(fraction * 100).roundToInt()}% of ${formatCompactNumber(goal)} goal",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier.clickable(onClick = onEditGoal),
            )
        }
    }
}

@Composable
private fun StreakCard(current: Int, best: Int) {
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(StreakTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = StreakTint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Current streak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = current.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (current == 1) "day" else "days",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Best: $best ${if (best == 1) "day" else "days"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCard(week: List<DayBar>, weekWords: Int) {
    StatsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Words read per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${formatCompactNumber(weekWords)} words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WeekChart(week)
    }
}

@Composable
private fun WeekChart(week: List<DayBar>) {
    val max = (week.maxOfOrNull { it.words } ?: 0).coerceAtLeast(1)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        week.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(track),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (day.words > 0) {
                        // Floor the height so a day with any reading is still a visible mark.
                        val fraction = (day.words.toFloat() / max).coerceIn(0.04f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun AllTimeCard(stats: ReadingStats) {
    StatsCard {
        Text("All time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = Icons.AutoMirrored.Filled.Notes,
                tint = MaterialTheme.colorScheme.primary,
                value = formatCompactNumber(stats.allTimeWords),
                label = "Words read",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.Schedule,
                tint = TimeTint,
                value = formatDuration(stats.allTimeMillis),
                label = "Time reading",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tint = BooksTint,
                value = stats.booksFinished.toString(),
                label = "Books finished",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.Speed,
                tint = SpeedTint,
                value = stats.allTimeWpm.toString(),
                label = "Avg WPM",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AchievementsCard(achievements: List<Achievement>) {
    StatsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${achievements.count { it.unlocked }} / ${achievements.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(achievements, key = { it.id.name }) { achievement ->
                AchievementBadge(achievement)
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    val tint = achievementTint(achievement.id)
    val locked = !achievement.unlocked

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (locked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else tint,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = achievementIcon(achievement.id),
                contentDescription = null,
                tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.White,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (locked) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily word goal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Words per day") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null && parsed > 0) {
                Text("Set")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val TimeTint = Color(0xFFE8833A)
private val SpeedTint = Color(0xFF7A5CFA)
private val BooksTint = Color(0xFF2E9E5B)
private val StreakTint = Color(0xFFE8833A)

private fun achievementTint(id: AchievementId): Color = when (id) {
    AchievementId.FIRST_STEPS -> Color(0xFFE05252)
    AchievementId.SPEED_DEMON -> Color(0xFF7A5CFA)
    AchievementId.BOOKWORM -> Color(0xFF2E9E5B)
    AchievementId.STREAK_MASTER -> Color(0xFFE8833A)
    AchievementId.MARATHON -> Color(0xFF3A86D6)
    AchievementId.CENTURION -> Color(0xFFC9A227)
}

private fun achievementIcon(id: AchievementId): ImageVector = when (id) {
    AchievementId.FIRST_STEPS -> Icons.AutoMirrored.Filled.MenuBook
    AchievementId.SPEED_DEMON -> Icons.Filled.Bolt
    AchievementId.BOOKWORM -> Icons.AutoMirrored.Filled.MenuBook
    AchievementId.STREAK_MASTER -> Icons.Filled.LocalFireDepartment
    AchievementId.MARATHON -> Icons.Filled.Speed
    AchievementId.CENTURION -> Icons.Filled.EmojiEvents
}
