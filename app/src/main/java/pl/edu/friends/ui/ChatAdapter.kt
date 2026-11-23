package pl.edu.friends.ui

import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import pl.edu.friends.data.ChatMessageEntity
import pl.edu.friends.databinding.ItemChatMessageBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onLikeClick: (ChatMessageEntity) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val items = mutableListOf<ChatMessageEntity>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    @Suppress("NotifyDataSetChanged")
    fun submitList(newItems: List<ChatMessageEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessageEntity) {
            val isMe = msg.sender.equals("Ty", ignoreCase = true) ||
                msg.sender.equals("Ти", ignoreCase = true) ||
                msg.sender.equals("You", ignoreCase = true)

            val theme = AppTheme.theme
            binding.layoutBubbleRow.gravity = if (isMe) Gravity.END else Gravity.START

            if (msg.hasPhoto) {
                binding.cardChatMessage.setCardBackgroundColor("#FFFFFF".toColorInt())
                binding.cardChatMessage.strokeWidth = 2
                binding.cardChatMessage.strokeColor = "#E5E7EB".toColorInt()
                binding.tvSenderName.setTextColor(theme.primaryColor)
                binding.tvMessageText.setTextColor("#111827".toColorInt())
                binding.tvMessageTime.setTextColor("#6B7280".toColorInt())
            } else if (isMe) {
                binding.cardChatMessage.setCardBackgroundColor(theme.primaryColor)
                binding.cardChatMessage.strokeWidth = 0
                binding.tvSenderName.setTextColor("#E0E7FF".toColorInt())
                binding.tvMessageText.setTextColor("#FFFFFF".toColorInt())
                binding.tvMessageTime.setTextColor("#E0E7FF".toColorInt())
            } else {
                binding.cardChatMessage.setCardBackgroundColor("#FFFFFF".toColorInt())
                binding.cardChatMessage.strokeWidth = 2
                binding.cardChatMessage.strokeColor = "#E5E7EB".toColorInt()
                binding.tvSenderName.setTextColor(theme.primaryColor)
                binding.tvMessageText.setTextColor("#111827".toColorInt())
                binding.tvMessageTime.setTextColor("#6B7280".toColorInt())
            }

            val youLabel = AppLocale.t("Ти", "You", "Ty")
            binding.tvSenderName.text = if (isMe) {
                if (msg.hasPhoto) "📷 $youLabel" else youLabel
            } else {
                msg.sender
            }

            binding.tvMessageText.text = msg.text
            binding.tvMessageTime.text = timeFormat.format(Date(msg.timestampMillis))

            if (msg.hasPhoto) {
                val isUri = msg.photoCaption.startsWith("content://") || msg.photoCaption.startsWith("file://")
                if (isUri) {
                    try {
                        binding.ivAttachedImage.visibility = View.VISIBLE
                        binding.ivAttachedImage.setImageURI(Uri.parse(msg.photoCaption))
                        binding.tvPhotoBadge.visibility = View.GONE
                    } catch (_: Exception) {
                        binding.ivAttachedImage.visibility = View.GONE
                        binding.tvPhotoBadge.visibility = View.VISIBLE
                        binding.tvPhotoBadge.text = AppLocale.t("📷 Фото з галереї", "📷 Gallery photo", "📷 Zdjęcie z galerii")
                    }
                } else {
                    binding.ivAttachedImage.visibility = View.GONE
                    binding.tvPhotoBadge.visibility = View.VISIBLE
                    binding.tvPhotoBadge.text = msg.photoCaption.ifEmpty {
                        AppLocale.t("📷 Фото з галереї", "📷 Gallery photo", "📷 Zdjęcie z galerii")
                    }
                }
            } else {
                binding.ivAttachedImage.visibility = View.GONE
                binding.tvPhotoBadge.visibility = View.GONE
            }

            binding.btnLikeMessage.text = if (msg.isLiked) "❤️" else "🤍"
            binding.btnLikeMessage.setOnClickListener {
                onLikeClick(msg)
            }
        }
    }
}
