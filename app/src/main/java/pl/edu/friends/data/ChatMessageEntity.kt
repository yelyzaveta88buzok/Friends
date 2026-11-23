package pl.edu.friends.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val text: String,
    val hasPhoto: Boolean = false,
    val photoCaption: String = "",
    val isLiked: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)
