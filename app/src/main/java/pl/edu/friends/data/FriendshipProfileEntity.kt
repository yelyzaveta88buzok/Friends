package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friendship_profile")
data class FriendshipProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val friendName: String = "",
    val startDateMillis: Long,
    val xpPoints: Int = 0,
    val streakDays: Int = 0,
    val myMood: String = "",
    val friendMood: String = "",
    val roomCode: String = "FRND-7492",
    val membersCsv: String = "",
    val checkedInMembersCsv: String = ""
)
