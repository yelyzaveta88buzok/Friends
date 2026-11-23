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
    version = 4,
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
            initializeEmptyProfile()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            initializeEmptyProfile()
        }

        private fun initializeEmptyProfile() {
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.bestiesDao()
                    dao.saveProfile(
                        FriendshipProfileEntity(
                            id = 1,
                            friendName = "",
                            startDateMillis = System.currentTimeMillis(),
                            xpPoints = 0,
                            streakDays = 0,
                            myMood = "",
                            friendMood = "",
                            roomCode = "FRND-7492",
                            membersCsv = "",
                            checkedInMembersCsv = ""
                        )
                    )
                }
            }
        }
    }
}
