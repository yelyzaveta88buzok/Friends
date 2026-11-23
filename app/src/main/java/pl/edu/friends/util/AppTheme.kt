package pl.edu.friends.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class RoomTheme(
    val emoji: String,
    val ua: String,
    val en: String,
    val pl: String,
    val primaryHex: String,
    val primaryDarkHex: String,
    val secondaryHex: String,
    val pageBgHex: String,
    val headerBgHex: String,
    val softTintHex: String,
    val cardStrokeHex: String,
    val waveBgHex: String,
    val waveGridHex: String
) {
    INDIGO(
        emoji = "🔵",
        ua = "🔵 Індиго",
        en = "🔵 Indigo",
        pl = "🔵 Indygo",
        primaryHex = "#4F46E5",
        primaryDarkHex = "#312E81",
        secondaryHex = "#818CF8",
        pageBgHex = "#F3F4F6",
        headerBgHex = "#FFFFFF",
        softTintHex = "#EEF2FF",
        cardStrokeHex = "#E5E7EB",
        waveBgHex = "#F8FAFC",
        waveGridHex = "#E2E8F0"
    ),
    SAGE(
        emoji = "🌿",
        ua = "🌿 Шавлія",
        en = "🌿 Sage",
        pl = "🌿 Szałwia",
        primaryHex = "#059669",
        primaryDarkHex = "#064E3B",
        secondaryHex = "#34D399",
        pageBgHex = "#F2F5F3",
        headerBgHex = "#FFFFFF",
        softTintHex = "#ECFDF5",
        cardStrokeHex = "#E2E8F0",
        waveBgHex = "#F7FAF8",
        waveGridHex = "#D1FAE5"
    ),
    ROSE(
        emoji = "🌸",
        ua = "🌸 Роза",
        en = "🌸 Rose",
        pl = "🌸 Róża",
        primaryHex = "#E11D48",
        primaryDarkHex = "#881337",
        secondaryHex = "#FB7185",
        pageBgHex = "#F5F3F4",
        headerBgHex = "#FFFFFF",
        softTintHex = "#FFF1F2",
        cardStrokeHex = "#E5E7EB",
        waveBgHex = "#FDF8F9",
        waveGridHex = "#FFE4E6"
    ),
    OCEAN(
        emoji = "🌊",
        ua = "🌊 Океан",
        en = "🌊 Ocean",
        pl = "🌊 Ocean",
        primaryHex = "#0284C7",
        primaryDarkHex = "#0C4A6E",
        secondaryHex = "#38BDF8",
        pageBgHex = "#F1F5F9",
        headerBgHex = "#FFFFFF",
        softTintHex = "#E0F2FE",
        cardStrokeHex = "#E2E8F0",
        waveBgHex = "#F8FAFC",
        waveGridHex = "#BAE6FD"
    ),
    VIOLET(
        emoji = "🟣",
        ua = "🟣 Аметист",
        en = "🟣 Violet",
        pl = "🟣 Ametyst",
        primaryHex = "#7C3AED",
        primaryDarkHex = "#4C1D95",
        secondaryHex = "#A78BFA",
        pageBgHex = "#F4F3F7",
        headerBgHex = "#FFFFFF",
        softTintHex = "#F3E8FF",
        cardStrokeHex = "#E5E7EB",
        waveBgHex = "#FAF8FF",
        waveGridHex = "#E9D5FF"
    ),
    AMBER(
        emoji = "🟠",
        ua = "🟠 Пісок",
        en = "🟠 Warm Sand",
        pl = "🟠 Piasek",
        primaryHex = "#D97706",
        primaryDarkHex = "#78350F",
        secondaryHex = "#F59E0B",
        pageBgHex = "#F5F3EE",
        headerBgHex = "#FFFFFF",
        softTintHex = "#FEF3C7",
        cardStrokeHex = "#E7E5E4",
        waveBgHex = "#FCFBF7",
        waveGridHex = "#FDE68A"
    );

    fun title(): String = AppLocale.t(ua, en, pl)

    val primaryColor: Int get() = primaryHex.toColorInt()
    val primaryDarkColor: Int get() = primaryDarkHex.toColorInt()
    val secondaryColor: Int get() = secondaryHex.toColorInt()
    val pageBgColor: Int get() = pageBgHex.toColorInt()
    val headerBgColor: Int get() = headerBgHex.toColorInt()
    val softTintColor: Int get() = softTintHex.toColorInt()
    val cardStrokeColor: Int get() = cardStrokeHex.toColorInt()
    val scrapbookBgColor: Int get() = pageBgColor
    val waveBgColor: Int get() = waveBgHex.toColorInt()
    val waveGridColor: Int get() = waveGridHex.toColorInt()
}

object AppTheme {
    private const val PREFS_NAME = "friends_theme_prefs"
    private const val KEY_THEME = "room_theme"

    private val _currentTheme = MutableLiveData(RoomTheme.INDIGO)
    val currentTheme: LiveData<RoomTheme> = _currentTheme

    val theme: RoomTheme get() = _currentTheme.value ?: RoomTheme.INDIGO

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_THEME, RoomTheme.INDIGO.name)
        val parsed = RoomTheme.entries.firstOrNull { it.name == savedName } ?: RoomTheme.INDIGO
        _currentTheme.value = parsed
    }

    fun setTheme(context: Context, newTheme: RoomTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, newTheme.name)
            .apply()
        _currentTheme.value = newTheme
    }

    private fun dp(context: Context, value: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ).toInt()

    fun verticalGradient(topHex: String, bottomHex: String): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topHex.toColorInt(), bottomHex.toColorInt())
        )

    fun bentoHeroDrawable(
        context: Context,
        topHex: String,
        bottomHex: String,
        strokeHex: String,
        radiusDp: Float = 22f
    ): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(topHex.toColorInt(), bottomHex.toColorInt())
    ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radiusDp).toFloat()
        setStroke(dp(context, 1f).coerceAtLeast(1), strokeHex.toColorInt())
    }

    fun pill(
        context: Context,
        fillHex: String,
        radiusDp: Float = 999f,
        strokeDp: Float = 0f,
        strokeHex: String = "#00000000"
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radiusDp).toFloat()
        setColor(fillHex.toColorInt())
        if (strokeDp > 0f) {
            setStroke(dp(context, strokeDp).coerceAtLeast(1), strokeHex.toColorInt())
        }
    }
}
