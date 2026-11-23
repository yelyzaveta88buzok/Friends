package pl.edu.friends.util

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class Lang(val code: String, val flagLabel: String) {
    UA("UA", "🇺🇦 UA"),
    EN("EN", "🇬🇧 EN"),
    PL("PL", "🇵🇱 PL")
}

data class GameTopicInfo(
    val gameType: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val badgeType: String,
    val cardBgHex: String,
    val accentHex: String
)

object AppLocale {

    private const val PREFS_NAME = "friends_locale_prefs"
    private const val KEY_LANG = "app_lang"

    @Volatile
    private var backingLang: Lang = Lang.UA

    private val _currentLang = MutableLiveData(Lang.UA)
    val currentLang: LiveData<Lang> = _currentLang

    val lang: Lang get() = backingLang

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANG, Lang.UA.code)
        val parsed = Lang.entries.firstOrNull { it.code == savedCode || it.name == savedCode } ?: Lang.UA
        setLanguage(parsed)
    }

    fun setLanguage(context: Context, newLang: Lang) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, newLang.code)
            .apply()
        setLanguage(newLang)
    }

    fun setLanguage(newLang: Lang) {
        backingLang = newLang
        try {
            _currentLang.value = newLang
        } catch (_: RuntimeException) {
            try {
                _currentLang.postValue(newLang)
            } catch (_: RuntimeException) {
            }
        }
    }

    fun t(ua: String, en: String, pl: String): String = when (lang) {
        Lang.UA -> ua
        Lang.EN -> en
        Lang.PL -> pl
    }

    fun getGameTopics(): List<GameTopicInfo> = listOf(
        GameTopicInfo(
            gameType = "NEVER",
            emoji = "🔥",
            title = t("Я ніколи не...", "Never Have I Ever", "Nigdy nie..."),
            desc = t("Дізнайтеся найшаленіші історії одне про одного та перевірте відсоток збігів!", "Discover the craziest stories and check your match %!", "Odkryjcie szalone historie i sprawdźcie procent zgodności!"),
            badgeType = t("ХІТ ВЕЧІРОК • +20 XP", "PARTY HIT • +20 XP", "HIT IMPREZ • +20 XP"),
            cardBgHex = "#FFF0F3",
            accentHex = "#FF6B6B"
        ),
        GameTopicInfo(
            gameType = "WHO",
            emoji = "😂",
            title = t("Хто з нас частіше?", "Who's Most Likely To?", "Kto z nas częściej?"),
            desc = t("Голосуйте, хто з вашої компанії швидше потрапить у кумедну ситуацію", "Vote who in your squad is most likely to do something funny", "Głosujcie, kto z ekipy częściej wpada w zabawne sytuacje"),
            badgeType = t("ВЕСЕЛА ВІКТОРИНА • +20 XP", "FUN QUIZ • +20 XP", "ZABAWNY QUIZ • +20 XP"),
            cardBgHex = "#F3E5F5",
            accentHex = "#7C4DFF"
        ),
        GameTopicInfo(
            gameType = "THIS_OR_THAT",
            emoji = "🎲",
            title = t("Це чи те? (This or That)", "This or That", "To czy tamto"),
            desc = t("Швидкий вибір між двома варіантами: перевірте, наскільки ви схожі!", "Quick choice between two options: see if you think alike!", "Szybki wybór między dwiema opcjami: sprawdźcie zgodność!"),
            badgeType = t("БЛІЦ-ВИБІР • +20 XP", "BLITZ CHOICE • +20 XP", "SZYBKI WYBÓR • +20 XP"),
            cardBgHex = "#E8F5E9",
            accentHex = "#2E7D32"
        ),
        GameTopicInfo(
            gameType = "DEEP_TALK",
            emoji = "🌙",
            title = t("Розмови у темряві", "Deep Talk & Memories", "Rozmowy po zmroku"),
            desc = t("Щирі питання про дружбу, мрії, підтримку та спільні спогади", "Sincere questions about friendship, dreams and memories", "Szczere pytania o przyjaźń, marzenia i wspólne wspomnienia"),
            badgeType = t("ГЛИБОКІ ПИТАННЯ • +20 XP", "DEEP TALK • +20 XP", "GŁĘBOKIE PYTANIA • +20 XP"),
            cardBgHex = "#FFF4D8",
            accentHex = "#E65100"
        )
    )
}
