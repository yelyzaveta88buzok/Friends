package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friendship_profile")
data class FriendshipProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val friendName: String = "Кацпер",
    val startDateMillis: Long,
    val xpPoints: Int = 220,
    val streakDays: Int = 14,
    val myMood: String = "🤩 Повна енергії!",
    val friendMood: String = "☕ Готовий на каву",
    val roomCode: String = "FRND-7492",
    val membersCsv: String = "Ліза,Кацпер,Аня",
    val checkedInMembersCsv: String = "Ліза,Кацпер,Аня"
)
