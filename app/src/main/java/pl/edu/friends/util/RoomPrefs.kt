package pl.edu.friends.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class WindowMode {
    AUTO, MORNING, DAY, SUNSET, NIGHT
}

data class WindowSkyVisual(
    val emoji: String,
    val topHex: String,
    val bottomHex: String,
    val label: String
)

object RoomPrefs {
    private const val PREFS_NAME = "friends_room_prefs"
    private const val KEY_WINDOW_MODE = "window_mode"
    private const val KEY_CUSTOM_IDEAS = "custom_hangout_ideas"
    private const val KEY_AVATAR_PREFIX = "avatar_"
    private const val KEY_BUBBLE_PREFIX = "bubble_"
    private const val KEY_MOOD_DATE = "daily_mood_date"
    private const val KEY_MOOD_COUNT = "daily_mood_count"
    private const val KEY_GAME_IDX_PREFIX = "game_q_idx_"

    const val MAX_DAILY_MOOD_REACTIONS = 5

    var openEnvelopesNext: Boolean = false

    var openPhotoPickerNext: Boolean = false

    val AVATAR_CHOICES = listOf(
        "😎", "🦊", "🐼", "🐱", "🦁", "🐸",
        "🐰", "🐻", "🦄", "🐨", "🐯", "🐣"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAvatar(context: Context, memberName: String, fallbackIndex: Int): String {
        val defaultEmoji = AVATAR_CHOICES[fallbackIndex.coerceAtLeast(0) % AVATAR_CHOICES.size]
        val key = KEY_AVATAR_PREFIX + memberName.trim().lowercase()
        return prefs(context).getString(key, defaultEmoji) ?: defaultEmoji
    }

    fun setAvatar(context: Context, memberName: String, emoji: String) {
        val key = KEY_AVATAR_PREFIX + memberName.trim().lowercase()
        prefs(context).edit().putString(key, emoji).apply()
    }

    fun getCustomBubble(context: Context, memberName: String): String {
        val key = KEY_BUBBLE_PREFIX + memberName.trim().lowercase()
        return prefs(context).getString(key, "").orEmpty()
    }

    fun setCustomBubble(context: Context, memberName: String, text: String) {
        val key = KEY_BUBBLE_PREFIX + memberName.trim().lowercase()
        prefs(context).edit().putString(key, text.trim()).apply()
    }

    fun getWindowMode(context: Context): WindowMode {
        val raw = prefs(context).getString(KEY_WINDOW_MODE, WindowMode.AUTO.name)
        return WindowMode.entries.firstOrNull { it.name == raw } ?: WindowMode.AUTO
    }

    fun cycleWindowMode(context: Context): WindowMode {
        val entries = WindowMode.entries
        val current = getWindowMode(context)
        val next = entries[(entries.indexOf(current) + 1) % entries.size]
        prefs(context).edit().putString(KEY_WINDOW_MODE, next.name).apply()
        return next
    }

    fun resolveWindowVisual(context: Context): WindowSkyVisual {
        val mode = getWindowMode(context)
        val effectiveMode = if (mode == WindowMode.AUTO) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..10 -> WindowMode.MORNING
                in 11..16 -> WindowMode.DAY
                in 17..20 -> WindowMode.SUNSET
                else -> WindowMode.NIGHT
            }
        } else {
            mode
        }

        val visual = when (effectiveMode) {
            WindowMode.MORNING -> WindowSkyVisual(
                emoji = "🌅✨",
                topHex = "#FB7185",
                bottomHex = "#FDE68A",
                label = AppLocale.t("Ранок", "Morning", "Ranek")
            )
            WindowMode.DAY -> WindowSkyVisual(
                emoji = "☀️☁️",
                topHex = "#0284C7",
                bottomHex = "#BAE6FD",
                label = AppLocale.t("День", "Day", "Dzień")
            )
            WindowMode.SUNSET -> WindowSkyVisual(
                emoji = "🌇✨",
                topHex = "#6D28D9",
                bottomHex = "#FB923C",
                label = AppLocale.t("Захід", "Sunset", "Zachód")
            )
            else -> WindowSkyVisual(
                emoji = "🌙⭐",
                topHex = "#1E1B4B",
                bottomHex = "#4338CA",
                label = AppLocale.t("Ніч", "Night", "Noc")
            )
        }
        return if (mode == WindowMode.AUTO) {
            visual.copy(label = "${visual.label} • Авто")
        } else {
            visual
        }
    }

    fun windowDrawable(context: Context, topHex: String, bottomHex: String, strokeColor: Int): GradientDrawable {
        val dm = context.resources.displayMetrics
        val topR = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, dm)
        val botR = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, dm)
        val strokePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm).toInt().coerceAtLeast(1)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topHex.toColorInt(), bottomHex.toColorInt())
        ).apply {
            cornerRadii = floatArrayOf(topR, topR, topR, topR, botR, botR, botR, botR)
            setStroke(strokePx, strokeColor)
        }
    }

    fun getCustomIdeas(context: Context): List<Pair<String, String>> {
        val raw = prefs(context).getString(KEY_CUSTOM_IDEAS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lines()
            .mapNotNull { line ->
                val idx = line.indexOf('|')
                if (idx > 0 && idx < line.lastIndex) {
                    Pair(line.substring(0, idx), line.substring(idx + 1))
                } else {
                    null
                }
            }
    }

    fun addCustomIdea(context: Context, category: String, text: String) {
        val cleanText = text.trim().replace("\n", " ").replace("|", "-")
        if (cleanText.isEmpty()) return
        val cat = if (category == "HOME" || category == "CITY" || category == "ACTIVE") category else "HOME"
        val updated = getCustomIdeas(context).toMutableList().apply {
            add(0, cat to cleanText)
        }
        val encoded = updated.take(30).joinToString("\n") { "${it.first}|${it.second}" }
        prefs(context).edit().putString(KEY_CUSTOM_IDEAS, encoded).apply()
    }

    fun getIdeasByCategory(context: Context, category: String): List<String> {
        val homeIdeas = listOf(
            AppLocale.t("🍕 Вечір домашньої піци та марафон комедій!", "🍕 Homemade pizza night & comedy marathon!", "🍕 Wieczór z domową pizzą i maratonem komedii!"),
            AppLocale.t("🎲 Турнір з настільних ігор та гарячий шоколад!", "🎲 Board game tournament & hot chocolate!", "🎲 Turniej gier planszowych i gorąca czekolada!"),
            AppLocale.t("🎤 Домашнє караоке з улюбленими хітами 2000-х!", "🎤 Home karaoke with 2000s hits!", "🎤 Domowe karaoke z hitami z lat 2000!")
        )
        val cityIdeas = listOf(
            AppLocale.t("☕ Дегустація нової кав'ярні та прогулянка парком!", "☕ Testing a new coffee shop & park walk!", "☕ Testowanie nowej kawiarni i spacer w parku!"),
            AppLocale.t("📸 Фотопрогулянка центром на полароїд або телефон!", "📸 City photo walk with Polaroid snaps!", "📸 Spacer fotograficzny po centrum!"),
            AppLocale.t("🌮 Стріт-фуд тур і пошук найсмачніших десертів!", "🌮 Street-food tour & dessert hunt!", "🌮 Street-food tour i polowanie na desery!")
        )
        val activeIdeas = listOf(
            AppLocale.t("🎳 Похід у боулінг або більярд усією компанією!", "🎳 Bowling or pool night with the squad!", "🎳 Wypad na kręgle lub bilard całą ekipą!"),
            AppLocale.t("🌅 Нічна поїздка містом під спільний плейлист!", "🌅 Late-night city drive with our playlist!", "🌅 Nocny przejazd po mieście z naszą playlistą!"),
            AppLocale.t("🚴 Велопрогулянка або пікнік біля води на заході сонця!", "🚴 Sunset bike ride or picnic by the water!", "🚴 Wycieczka rowerowa lub piknik o zachodzie słońca!")
        )

        val custom = getCustomIdeas(context)
            .filter { category == "ALL" || it.first == category }
            .map { "⭐ ${it.second}" }

        val builtIn = when (category) {
            "HOME" -> homeIdeas
            "CITY" -> cityIdeas
            "ACTIVE" -> activeIdeas
            else -> homeIdeas + cityIdeas + activeIdeas
        }
        return custom + builtIn
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getTodayMoodCount(context: Context): Int {
        val sp = prefs(context)
        val today = todayDateKey()
        val savedDate = sp.getString(KEY_MOOD_DATE, "")
        if (savedDate != today) {
            sp.edit().putString(KEY_MOOD_DATE, today).putInt(KEY_MOOD_COUNT, 0).apply()
            return 0
        }
        return sp.getInt(KEY_MOOD_COUNT, 0).coerceIn(0, MAX_DAILY_MOOD_REACTIONS)
    }

    fun incrementTodayMoodCount(context: Context): Int {
        val current = getTodayMoodCount(context)
        val next = (current + 1).coerceAtMost(MAX_DAILY_MOOD_REACTIONS)
        prefs(context).edit()
            .putString(KEY_MOOD_DATE, todayDateKey())
            .putInt(KEY_MOOD_COUNT, next)
            .apply()
        return next
    }

    fun getGameQuestionIndex(context: Context, gameType: String): Int =
        prefs(context).getInt(KEY_GAME_IDX_PREFIX + gameType, 0).coerceAtLeast(0)

    fun setGameQuestionIndex(context: Context, gameType: String, index: Int) {
        prefs(context).edit()
            .putInt(KEY_GAME_IDX_PREFIX + gameType, index.coerceAtLeast(0))
            .apply()
    }
}
