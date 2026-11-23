package pl.edu.friends.util

import pl.edu.friends.data.JournalEntryEntity
import pl.edu.friends.data.QuizAnswerEntity
import pl.edu.friends.data.TimeCapsuleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class FriendshipDuration(
    val days: Long,
    val hours: Long,
    val minutes: Long
)

data class NeverMatchStats(
    val totalCount: Int,
    val bothByloCount: Int,
    val bothNeverCount: Int,
    val diffCount: Int,
    val matchPercentage: Int
)

data class GameQuestion(
    val gameType: String,
    val question: String,
    val optionA: String,
    val optionB: String
)

object BestiesLogicHelper {

    fun getMoodOptions(): List<String> = listOf(
        AppLocale.t("🤩 Повна енергії!", "🤩 Full of energy!", "🤩 Pełen energii!"),
        AppLocale.t("☕ Готовий на каву", "☕ Ready for coffee", "☕ Gotowy na kawę"),
        AppLocale.t("😂 Шукаю меми", "😂 Looking for memes", "😂 Szukam memów"),
        AppLocale.t("🍕 Хочу піцу!", "🍕 Craving pizza!", "🍕 Głodny – pizza?"),
        AppLocale.t("😴 Втомився після пар", "😴 Tired after class", "😴 Zmęczony po zajęciach"),
        AppLocale.t("🎮 Граємо ввечері!", "🎮 Gaming tonight!", "🎮 Gramy wieczorem!")
    )

    fun getHangoutIdeas(): List<String> = listOf(
        AppLocale.t("🍕 Вечір домашньої піци та марафон комедій!", "🍕 Homemade pizza night & comedy marathon!", "🍕 Wieczór z domową pizzą i maratonem komedii!"),
        AppLocale.t("🎳 Похід у боулінг або більярд усією компанією!", "🎳 Bowling or pool night with the squad!", "🎳 Wypad na kręgle lub bilard na mieście!"),
        AppLocale.t("🌅 Нічна поїздка містом під улюблений плейлист!", "🌅 Late-night city drive with your favorite playlist!", "🌅 Nocny przejazd po mieście z ulubioną playlistą!"),
        AppLocale.t("🎲 Турнір з настільних ігор та гарячий шоколад!", "🎲 Board game tournament & hot chocolate!", "🎲 Turniej gier planszowych i gorąca czekolada!"),
        AppLocale.t("📸 Фотопрогулянка та дегустація нової кав'ярні!", "📸 Photo walk & testing a new coffee shop!", "📸 Spacer fotograficzny i testowanie nowej kawiarni!"),
        AppLocale.t("🎤 Домашнє караоке з хітами 2000-х!", "🎤 Home karaoke with 2000s hits!", "🎤 Domowe karaoke z hitami z lat 2000!")
    )

    fun getDailyQuestions(): List<String> = listOf(
        AppLocale.t("Який наш найкращий спогад за цей тиждень?", "What is our best memory from this week?", "Jakie jest nasze najlepsze wspomnienie z tego tygodnia?"),
        AppLocale.t("За що ти найбільше цінуєш нашу дружбу сьогодні?", "What do you appreciate most about our friendship today?", "Za co najbardziej cenisz naszą przyjaźń dzisiaj?"),
        AppLocale.t("Куди ми обов'язково поїдемо всією компанією цього року?", "Where should our squad travel together this year?", "Gdzie koniecznie musimy pojechać całą ekipą w tym roku?"),
        AppLocale.t("Яка пісня або мем ідеально описує наш сьогоднішній настрій?", "Which song or meme describes our vibe today?", "Jaka piosenka lub mem idealnie opisuje nasz dzisiejszy klimat?"),
        AppLocale.t("Яка дрібниця зробила твій сьогоднішній день теплішим?", "What tiny moment brightened your day today?", "Jaki drobiazg poprawił Ci dziś humor?"),
        AppLocale.t("Що нового нам варто спробувати на наступних вихідних?", "What new hangout spot should we try next weekend?", "Jakie nowe miejsce odwiedzimy w następny weekend?"),
        AppLocale.t("Який фільм чи серіал ти радиш подивитися компанії ввечері?", "Which movie or show should we watch together soon?", "Jaki film lub serial obejrzymy razem wieczorem?"),
        AppLocale.t("Яка наша найсмішніша спільна історія за останній рік?", "What is our funniest shared story from this past year?", "Jaka jest nasza najzabawniejsza historia z ostatniego roku?"),
        AppLocale.t("Яку страву чи десерт ми точно повинні приготувати разом?", "What meal or dessert should we cook together next?", "Co powinniśmy razem ugotować następnym razem?"),
        AppLocale.t("За ким або за чим із нашого літа ти вже сумуєш?", "What summer moment do you already feel nostalgic about?", "Za jakim letnim momentem już tęsknisz?"),
        AppLocale.t("Яку пісню ми б співали хором у машині прямо зараз?", "Which song would we scream-sing in the car right now?", "Jaką piosenkę zaśpiewalibyśmy teraz razem w aucie?"),
        AppLocale.t("Що в твоєму плані на цей місяць викликає найбільший азарт?", "What goal on your list excites you most this month?", "Co w Twoich planach na ten miesiąc cieszy Cię najbardziej?"),
        AppLocale.t("Кому з друзів ти хочеш сказати «дякую» сьогодні і за що?", "Which friend deserves a shoutout today and why?", "Komu z ekipy chcesz dziś podziękować i za co?"),
        AppLocale.t("Яка наша спільна фотка — абсолютний шедевр?", "Which group photo of ours is an undefeated masterpiece?", "Które nasze wspólne zdjęcie to absolutne złoto?"),
        AppLocale.t("Що покладемо в нашу наступну Капсулу Часу?", "What secret note should we seal in our next Time Capsule?", "Co schowamy do naszej następnej Kapsuły Czasu?")
    )

    fun getNeverQuestions(): List<GameQuestion> = QuestionsBankNever.getAll()

    fun getWhoQuestions(): List<GameQuestion> = QuestionsBankWho.getAll()

    fun getThisOrThatQuestions(): List<GameQuestion> = QuestionsBankThisOrThat.getAll()

    fun getDeepTalkQuestions(): List<GameQuestion> = QuestionsBankDeepTalk.getAll()

    fun getAutoFriendChatReplies(): List<String> = listOf(
        AppLocale.t("Хаха, 100%! 😂 Йдемо сьогодні на каву?", "Haha, 100%! 😂 Grabbing coffee today?", "Haha, dokładnie tak! 😂 Wpadamy dzisiaj na kawę?"),
        AppLocale.t("Звучить як ідеальний план! Я тільки за 🔥", "Sounds like a perfect plan! I'm in 🔥", "Brzmi jak idealny plan! Jestem za w 100% 🔥"),
        AppLocale.t("Топ мем! Зараз скину ще смішніший 😎", "Top meme! Sending an even funnier one now 😎", "Widziałem to! Zaraz wyślę Ci jeszcze lepszego mema 😎"),
        AppLocale.t("Найкраща компанія! Ввечері обов'язково граємо в «Я ніколи не...» 🎲", "Best squad! Let's play 'Never Have I Ever' tonight 🎲", "Najlepsza ekipa! Musimy zagrać w 'Nigdy nie...' wieczorem 🎲")
    )

    fun calculateFriendshipDuration(startDateMillis: Long, nowMillis: Long): FriendshipDuration {
        val diff = (nowMillis - startDateMillis).coerceAtLeast(0L)
        val totalMinutes = diff / (1000L * 60L)
        val days = totalMinutes / (60L * 24L)
        val hours = (totalMinutes % (60L * 24L)) / 60L
        val minutes = totalMinutes % 60L
        return FriendshipDuration(days, hours, minutes)
    }

    fun getFriendshipLevelTitle(xp: Int): String = when {
        xp >= 500 -> "Poziom 5: Legendarni Przyjaciele 👑"
        xp >= 300 -> "Poziom 4: Drużyna marzeń ⚡"
        xp >= 150 -> "Poziom 3: Nierozłączni przyjaciele 🔥"
        xp >= 50  -> "Poziom 2: Prawdziwi przyjaciele 🤝"
        else      -> "Poziom 1: Nowi znajomi 🌱"
    }

    fun getLocalizedLevelTitle(xp: Int): String = when {
        xp >= 500 -> AppLocale.t("Рівень 5: Легендарні Друзі 👑", "Level 5: Legendary Friends 👑", "Poziom 5: Legendarni Przyjaciele 👑")
        xp >= 300 -> AppLocale.t("Рівень 4: Команда мрії ⚡", "Level 4: Dream Squad ⚡", "Poziom 4: Drużyna marzeń ⚡")
        xp >= 150 -> AppLocale.t("Рівень 3: Нерозлучні друзі 🔥", "Level 3: Inseparable Friends 🔥", "Poziom 3: Nierozłączni przyjaciele 🔥")
        xp >= 50  -> AppLocale.t("Рівень 2: Справжні друзі 🤝", "Level 2: True Friends 🤝", "Poziom 2: Prawdziwi przyjaciele 🤝")
        else      -> AppLocale.t("Рівень 1: Нові знайомі 🌱", "Level 1: New Friends 🌱", "Poziom 1: Nowi znajomi 🌱")
    }

    fun isCapsuleUnlocked(capsule: TimeCapsuleEntity, nowMillis: Long): Boolean {
        return nowMillis >= capsule.unlockAtMillis
    }

    fun formatCapsuleStatus(capsule: TimeCapsuleEntity, nowMillis: Long): String {
        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(capsule.unlockAtMillis))
        if (isCapsuleUnlocked(capsule, nowMillis)) {
            return AppLocale.t(
                "🔓 Відкрито ($dateStr) • Від: ${capsule.author}",
                "🔓 Unlocked ($dateStr) • From: ${capsule.author}",
                "🔓 Odblokowana ($dateStr) • Autor: ${capsule.author}"
            )
        }
        val diffMs = capsule.unlockAtMillis - nowMillis
        val daysLeft = ceil(diffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt().coerceAtLeast(1)
        return AppLocale.t(
            "🔒 Відкриється: $dateStr (ще $daysLeft дн.) • Від: ${capsule.author}",
            "🔒 Unlocks: $dateStr (in $daysLeft days) • From: ${capsule.author}",
            "🔒 Odblokowanie: $dateStr (za $daysLeft dni) • Autor: ${capsule.author}"
        )
    }

    fun calculateNeverMatchStats(answers: List<QuizAnswerEntity>): NeverMatchStats {
        if (answers.isEmpty()) return NeverMatchStats(0, 0, 0, 0, 0)
        var bothBylo = 0
        var bothNever = 0
        var diff = 0
        for (a in answers) {
            val myBylo = a.myAnswer.contains("BYŁO", ignoreCase = true) ||
                a.myAnswer.contains("БУЛО", ignoreCase = true) ||
                a.myAnswer.contains("HAVE", ignoreCase = true)
            val friendBylo = a.friendAnswer.contains("BYŁO", ignoreCase = true) ||
                a.friendAnswer.contains("БУЛО", ignoreCase = true) ||
                a.friendAnswer.contains("HAVE", ignoreCase = true)
            when {
                myBylo && friendBylo -> bothBylo++
                !myBylo && !friendBylo -> bothNever++
                else -> diff++
            }
        }
        val matches = bothBylo + bothNever
        val pct = ((matches.toDouble() / answers.size.toDouble()) * 100.0).roundToInt()
        return NeverMatchStats(answers.size, bothBylo, bothNever, diff, pct)
    }

    fun evaluateMoodInsight(entries: List<JournalEntryEntity>): String {
        if (entries.isEmpty()) {
            return AppLocale.t(
                "Додайте перший запис у журнал, щоб побачити графік настроїв!",
                "Add your first journal entry to see the mood chart!",
                "Dodaj pierwszy wpis w dzienniku, aby zobaczyć analizę Waszego nastroju!"
            )
        }
        val recent = entries.take(7)
        val avgMood = recent.map { (it.myMoodScore + it.friendMoodScore) / 2.0 }.average()
        return when {
            avgMood >= 4.5 -> AppLocale.t(
                "Середній настрій: %.1f/5 🤩 Ваш зв'язок зараз на висоті! Чудовий тиждень!".format(avgMood),
                "Average mood: %.1f/5 🤩 Your friendship energy is at its peak!".format(avgMood),
                "Średnia nastroju: %.1f/5 🤩 Wasza energia jest na szczycie!".format(avgMood)
            )
            avgMood >= 3.5 -> AppLocale.t(
                "Середній настрій: %.1f/5 😊 Ви в стабільному балансі! Час на спільну каву.".format(avgMood),
                "Average mood: %.1f/5 😊 Stable balance! Time for a hangout.".format(avgMood),
                "Średnia nastroju: %.1f/5 😊 Dobry, stabilny tydzień!".format(avgMood)
            )
            else -> AppLocale.t(
                "Середній настрій: %.1f/5 💙 Підтримайте одне одного або відправте смішний мем!".format(avgMood),
                "Average mood: %.1f/5 💙 Support each other or send a funny meme!".format(avgMood),
                "Średnia nastroju: %.1f/5 💙 Warto wesprzeć przyjaciela!".format(avgMood)
            )
        }
    }

    fun moodEmoji(score: Int): String = when (score) {
        5 -> "😍"
        4 -> "😊"
        3 -> "😐"
        2 -> "😔"
        else -> "😢"
    }

    fun moodLabel(score: Int): String = when (score) {
        5 -> AppLocale.t("Чудово", "Awesome", "Świetnie")
        4 -> AppLocale.t("Добре", "Good", "Dobrze")
        3 -> AppLocale.t("Нормально", "Okay", "Normalnie")
        2 -> AppLocale.t("Сумно", "Sad", "Smutno")
        else -> AppLocale.t("Погано", "Bad", "Źle")
    }
}
