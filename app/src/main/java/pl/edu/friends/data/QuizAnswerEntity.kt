package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_answers")
data class QuizAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameType: String,
    val questionText: String,
    val myAnswer: String,
    val friendAnswer: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
