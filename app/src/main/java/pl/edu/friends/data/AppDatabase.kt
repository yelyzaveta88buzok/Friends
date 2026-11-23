package pl.edu.friends.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FriendshipProfileEntity::class,
        TimeCapsuleEntity::class,
        ChatMessageEntity::class,
        QuizAnswerEntity::class,
        JournalEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bestiesDao(): BestiesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "besties_friends_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(PrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            populateSampleData()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            populateSampleData()
        }

        private fun populateSampleData() {
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.bestiesDao()
                    val now = System.currentTimeMillis()
                    val dayMs = 24L * 60L * 60L * 1000L

                    dao.saveProfile(
                        FriendshipProfileEntity(
                            id = 1,
                            friendName = "Кацпер",
                            startDateMillis = now - (428L * dayMs) - (5L * 3600_000L),
                            xpPoints = 220,
                            streakDays = 14,
                            myMood = "🤩 Повна енергії!",
                            friendMood = "☕ Готовий на каву",
                            roomCode = "FRND-7492",
                            membersCsv = "Ліза,Кацпер,Аня",
                            checkedInMembersCsv = "Ліза,Кацпер"
                        )
                    )

                    dao.insertCapsule(
                        TimeCapsuleEntity(
                            title = "Спогад з нашої першої поїздки в гори 🏔️",
                            message = "Пам'ятаєте, як ми заблукали і сміялися дві години поспіль? Найкраща компанія!",
                            author = "Кацпер",
                            unlockAtMillis = now - (2L * dayMs),
                            createdAtMillis = now - (30L * dayMs)
                        )
                    )
                    dao.insertCapsule(
                        TimeCapsuleEntity(
                            title = "Капсула на захист диплому 🎓",
                            message = "Відкриваємо після захисту інженерки! Йдемо святкувати всією компанією!",
                            author = "Ліза",
                            unlockAtMillis = now + (14L * dayMs),
                            createdAtMillis = now - dayMs
                        )
                    )
                    dao.insertCapsule(
                        TimeCapsuleEntity(
                            title = "Плани на літо 2027 ✈️",
                            message = "Наш секретний список мрій на наступне літо!",
                            author = "Аня",
                            unlockAtMillis = now + (60L * dayMs),
                            createdAtMillis = now
                        )
                    )

                    dao.insertMessage(
                        ChatMessageEntity(
                            sender = "Кацпер",
                            text = "Привіт! Вже бачили нашу відкриту капсулу часу з поїздки в гори? 🔥",
                            hasPhoto = false,
                            isLiked = true,
                            timestampMillis = now - 3600_000L
                        )
                    )
                    dao.insertMessage(
                        ChatMessageEntity(
                            sender = "Ty",
                            text = "Так!! Такий теплий спогад, треба обов'язково повторити виїзд!",
                            hasPhoto = true,
                            photoCaption = "📷 Фото з галереї: Селфі на вершині гори",
                            isLiked = true,
                            timestampMillis = now - 1800_000L
                        )
                    )
                    dao.insertMessage(
                        ChatMessageEntity(
                            sender = "Аня",
                            text = "Збираємося сьогодні ввечері на піцу і граємо в «Я ніколи не...»? 🍕🎲",
                            hasPhoto = false,
                            isLiked = false,
                            timestampMillis = now - 600_000L
                        )
                    )

                    dao.insertQuizAnswer(
                        QuizAnswerEntity(
                            gameType = "NEVER",
                            questionText = "Я ніколи не просиджував(-ла) всю ніч за розмовами чи іграми з друзями.",
                            myAnswer = "BYŁO!",
                            friendAnswer = "BYŁO!",
                            timestampMillis = now - 4 * dayMs
                        )
                    )
                    dao.insertQuizAnswer(
                        QuizAnswerEntity(
                            gameType = "NEVER",
                            questionText = "Я ніколи не їздив(-ла) у спонтанну поїздку, заплановану за 15 хвилин.",
                            myAnswer = "BYŁO!",
                            friendAnswer = "BYŁO!",
                            timestampMillis = now - 3 * dayMs
                        )
                    )
                    dao.insertQuizAnswer(
                        QuizAnswerEntity(
                            gameType = "NEVER",
                            questionText = "Я ніколи випадково не розповідав(-ла) чужий секрет.",
                            myAnswer = "NIGDY",
                            friendAnswer = "NIGDY",
                            timestampMillis = now - 2 * dayMs
                        )
                    )
                    dao.insertQuizAnswer(
                        QuizAnswerEntity(
                            gameType = "NEVER",
                            questionText = "Я ніколи не запізнювався(-лася) на зустріч з друзями більше ніж на 30 хвилин.",
                            myAnswer = "NIGDY",
                            friendAnswer = "BYŁO!",
                            timestampMillis = now - dayMs
                        )
                    )

                    val sampleDays = listOf(
                        Triple("05.11.2026", 4, 4),
                        Triple("06.11.2026", 5, 4),
                        Triple("07.11.2026", 4, 5),
                        Triple("08.11.2026", 4, 4),
                        Triple("09.11.2026", 5, 5),
                        Triple("10.11.2026", 5, 4),
                        Triple("11.11.2026", 5, 5)
                    )
                    val sampleTexts = listOf(
                        "Ранкова кава перед парами і купа смішних мемів ☕😂",
                        "Вечір настільних ігор — ніхто не хотів здаватися в Монополії! 🎲",
                        "Спонтанна прогулянка вечірнім містом під улюблений плейлист 🌆",
                        "Готували разом домашню піцу, вийшло смачніше ніж у ресторані 🍕",
                        "Дивилися старі комедії до другої ночі 🎬✨",
                        "Підтримали одне одного перед складним заліком в університеті 🤝",
                        "Найкращий тиждень з нашою п'ятіркою друзів! 🔥👑"
                    )
                    val authors = listOf("Ліза", "Кацпер", "Аня", "Ліза", "Кацпер", "Аня", "Ліза")
                    sampleDays.forEachIndexed { idx, (dateStr, myScore, friendScore) ->
                        dao.insertJournalEntry(
                            JournalEntryEntity(
                                dateLabel = dateStr,
                                questionText = authors[idx],
                                myAnswer = sampleTexts[idx],
                                friendAnswer = "❤️ Вподобано друзями",
                                myMoodScore = myScore,
                                friendMoodScore = friendScore,
                                sharedNote = "📸 Спільний спогад компанії",
                                timestampMillis = now - ((6 - idx) * dayMs)
                            )
                        )
                    }
                }
            }
        }
    }
}
