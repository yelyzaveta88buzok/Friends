package pl.edu.friends.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import pl.edu.friends.databinding.DialogAddCapsuleBinding
import pl.edu.friends.databinding.FragmentChatBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.BestiesLogicHelper
import pl.edu.friends.util.RoomPrefs

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BestiesViewModel by activityViewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var capsuleAdapter: CapsuleAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var showingEnvelopes = false
    private var capsuleCount = 0

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val caption = binding.etChatMessage.text?.toString()?.trim().orEmpty()
                .ifEmpty { AppLocale.t("📷 Фото з галереї", "📷 Photo from gallery", "📷 Zdjęcie z galerii") }
            binding.etChatMessage.setText("")

            viewModel.sendChatMessage(
                sender = "Ty",
                text = caption,
                hasPhoto = true,
                photoCaption = uri.toString()
            )
            Toast.makeText(
                requireContext(),
                AppLocale.t("📷 Фото надіслано в чат!", "📷 Photo sent to chat!", "📷 Zdjęcie wysłane na czat!"),
                Toast.LENGTH_SHORT
            ).show()
            simulateFriendReply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatAdapter = ChatAdapter { msg ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.toggleLike(msg)
        }

        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.adapter = chatAdapter

        capsuleAdapter = CapsuleAdapter(
            onCapsuleClick = { capsule, unlocked ->
                if (unlocked) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("🔓 ${capsule.title}")
                        .setMessage(
                            AppLocale.t(
                                "Відкритий лист від ${capsule.author}:\n\n\"${capsule.message}\"",
                                "Opened capsule from ${capsule.author}:\n\n\"${capsule.message}\"",
                                "Otwarty list od ${capsule.author}:\n\n\"${capsule.message}\""
                            )
                        )
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle(AppLocale.t("⏳ Капсула запечатана 🔒", "⏳ Sealed Time Capsule 🔒", "⏳ Zapieczętowana kapsuła 🔒"))
                        .setMessage(
                            AppLocale.t(
                                "Ця капсула часу запечатана 🔒 Назва та текст стануть видимими тільки у день відкриття.",
                                "This time capsule is sealed 🔒 Title and secret message stay hidden until the unlock date.",
                                "Ta kapsuła czasu jest zamknięta 🔒 Tytuł i treść odkryją się dopiero w dniu otwarcia."
                            )
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
            },
            onDeleteClick = { capsule ->
                viewModel.deleteCapsule(capsule)
                Toast.makeText(
                    requireContext(),
                    AppLocale.t("Капсулу видалено", "Capsule deleted", "Usunięto kapsułę"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.rvChatCapsules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatCapsules.adapter = capsuleAdapter

        binding.btnSubtabNotes.setOnClickListener { setSubtab(envelopes = false) }
        binding.btnSubtabEnvelopes.setOnClickListener { setSubtab(envelopes = true) }

        binding.btnCreateEnvelopeCapsule.setOnClickListener {
            showSealNewEnvelopeDialog()
        }

        binding.chipSticker1.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t("✋ Даю п'ять всій нашій компанії! 🤜🤛", "✋ High-five everyone! 🤜🤛", "✋ Piątka dla całej paczki! 🤜🤛")
            )
            simulateFriendReply()
        }
        binding.chipSticker2.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t("☕ Хто сьогодні йде на каву?", "☕ Who's grabbing coffee today?", "☕ Kto wpada dzisiaj na kawę?")
            )
            simulateFriendReply()
        }
        binding.chipSticker3.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t("🤗 Обіймаю! Як проходить день?", "🤗 Sending hugs! How's your day?", "🤗 Ściskam! Jak mija dzień?")
            )
            simulateFriendReply()
        }
        binding.chipSticker4.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.sendChatMessage(
                sender = "Ty",
                text = AppLocale.t("🍕 Збираємося ввечері на піцу!", "🍕 Pizza night tonight!", "🍕 Wpadamy wieczorem na pizzę!")
            )
            simulateFriendReply()
        }

        binding.btnAttachPhoto.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnSendChat.setOnClickListener {
            val text = binding.etChatMessage.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                viewModel.sendChatMessage(sender = "Ty", text = text)
                binding.etChatMessage.setText("")
                simulateFriendReply()
            }
        }

        viewModel.allMessages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChatMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.allCapsules.observe(viewLifecycleOwner) { capsules ->
            capsuleCount = capsules.size
            capsuleAdapter.submitList(capsules)
            updateSubtabButtonsUi()
        }

        AppLocale.currentLang.observe(viewLifecycleOwner) {
            applyLocalization()
            chatAdapter.notifyDataSetChanged()
            capsuleAdapter.notifyDataSetChanged()
        }

        AppTheme.currentTheme.observe(viewLifecycleOwner) { theme ->
            binding.chatRootLayout.setBackgroundColor(theme.pageBgColor)
            binding.btnSendChat.setCardBackgroundColor(theme.primaryColor)
            binding.btnCreateEnvelopeCapsule.backgroundTintList = ColorStateList.valueOf(theme.primaryColor)
            updateSubtabButtonsUi()
            chatAdapter.notifyDataSetChanged()
            capsuleAdapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        if (RoomPrefs.openEnvelopesNext) {
            RoomPrefs.openEnvelopesNext = false
            setSubtab(envelopes = true)
        } else if (RoomPrefs.openPhotoPickerNext) {
            RoomPrefs.openPhotoPickerNext = false
            setSubtab(envelopes = false)
            handler.postDelayed({
                if (_binding != null) {
                    pickImageLauncher.launch(arrayOf("image/*"))
                }
            }, 150L)
        }
    }

    fun openPhotoFromQuickPlus() {
        setSubtab(envelopes = false)
        pickImageLauncher.launch(arrayOf("image/*"))
    }

    fun openCapsulesFromQuickPlus() {
        setSubtab(envelopes = true)
    }

    private fun setSubtab(envelopes: Boolean) {
        showingEnvelopes = envelopes
        binding.layoutNotesWall.visibility = if (envelopes) View.GONE else View.VISIBLE
        binding.layoutEnvelopesVault.visibility = if (envelopes) View.VISIBLE else View.GONE
        updateSubtabButtonsUi()
    }

    private fun updateSubtabButtonsUi() {
        val ctx = requireContext()
        val theme = AppTheme.theme

        if (!showingEnvelopes) {
            binding.btnSubtabNotes.background = AppTheme.pill(ctx, theme.primaryHex, 10f)
            binding.btnSubtabNotes.setTextColor("#FFFFFF".toColorInt())
            binding.btnSubtabEnvelopes.background = null
            binding.btnSubtabEnvelopes.setTextColor("#6B7280".toColorInt())
        } else {
            binding.btnSubtabEnvelopes.background = AppTheme.pill(ctx, theme.primaryHex, 10f)
            binding.btnSubtabEnvelopes.setTextColor("#FFFFFF".toColorInt())
            binding.btnSubtabNotes.background = null
            binding.btnSubtabNotes.setTextColor("#6B7280".toColorInt())
        }

        binding.btnSubtabNotes.text = AppLocale.t("💬 Чат і фото", "💬 Chat & Photos", "💬 Czat i zdjęcia")
        binding.btnSubtabEnvelopes.text = AppLocale.t(
            "⏳ Капсули ($capsuleCount) 🔒",
            "⏳ Capsules ($capsuleCount) 🔒",
            "⏳ Kapsuły ($capsuleCount) 🔒"
        )
    }

    private fun showSealNewEnvelopeDialog() {
        val dialogBinding = DialogAddCapsuleBinding.inflate(layoutInflater)
        dialogBinding.tilCapsuleTitle.hint = AppLocale.t(
            "Назва капсули (напр. Літо 2026)",
            "Capsule title (e.g. Summer 2026)",
            "Tytuł kapsuły (np. Lato 2026)"
        )
        dialogBinding.tilCapsuleDays.hint = AppLocale.t(
            "Через скільки днів відкрити? (0 = одразу)",
            "Unlock in how many days? (0 = now)",
            "Za ile dni odblokować? (0 = od razu)"
        )
        dialogBinding.tilCapsuleMessage.hint = AppLocale.t(
            "Таємне повідомлення у майбутнє",
            "Secret message for the future",
            "Tajna wiadomość do odczytania w przyszłości"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(AppLocale.t("✉️ Нова Капсула Часу (+30 XP)", "✉️ New Time Capsule (+30 XP)", "✉️ Nowa Kapsuła Czasu (+30 XP)"))
            .setView(dialogBinding.root)
            .setPositiveButton(AppLocale.t("Запечатати 🔒", "Seal 🔒", "Zapieczętuj 🔒")) { _, _ ->
                val title = dialogBinding.etCapsuleTitle.text?.toString()?.trim().orEmpty()
                val daysStr = dialogBinding.etCapsuleDaysUntilUnlock.text?.toString()?.trim().orEmpty()
                val message = dialogBinding.etCapsuleMessage.text?.toString()?.trim().orEmpty()

                if (title.isNotEmpty() && message.isNotEmpty()) {
                    val days = daysStr.toIntOrNull()?.coerceAtLeast(0) ?: 7
                    viewModel.addCapsule(
                        title = title,
                        message = message,
                        author = AppLocale.t("Ліза", "Liza", "Liza"),
                        daysUntilUnlock = days
                    )
                    Toast.makeText(
                        requireContext(),
                        AppLocale.t(
                            "✉️ Капсулу запечатано 🔒 Відкриття через $days дн. (+30 XP)",
                            "✉️ Capsule sealed 🔒 Unlocks in $days days (+30 XP)",
                            "✉️ Kapsuła zapieczętowana 🔒 Otwarcie za $days dni (+30 XP)"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
            .show()
    }

    private fun applyLocalization() {
        binding.tvChatPartnerName.text = AppLocale.t("Чат компанії", "Squad Chat", "Czat ekipy")
        binding.tvChatOnlineStatus.text = AppLocale.t(
            "● Друзі онлайн • Повідомлення, фото і капсули",
            "● Squad online • Messages, photos & capsules",
            "● Ekipa online • Wiadomości, zdjęcia i kapsuły"
        )
        binding.chipSticker1.text = AppLocale.t("✋ Дай п'ять", "✋ High-five", "✋ Piątka")
        binding.chipSticker2.text = AppLocale.t("☕ На каву?", "☕ Coffee?", "☕ Kawa?")
        binding.chipSticker3.text = AppLocale.t("🤗 Обіймаю", "🤗 Hugs", "🤗 Ściskam")
        binding.chipSticker4.text = AppLocale.t("🍕 На піцу!", "🍕 Pizza!", "🍕 Na pizzę!")
        binding.etChatMessage.hint = AppLocale.t(
            "Повідомлення компанії...",
            "Message your squad...",
            "Napisz do przyjaciół..."
        )
        binding.tvVaultPolaroidTitle.text = AppLocale.t(
            "⏳ Капсули Часу Компанії",
            "⏳ Squad Time Capsules",
            "⏳ Kapsuły Czasu Ekipy"
        )
        binding.tvVaultPolaroidSub.text = AppLocale.t(
            "🔒 Закриті капсули повністю приховані (і заголовок, і текст) до дати відкриття.",
            "🔒 Sealed capsules hide both title and secret message until the unlock date.",
            "🔒 Zamknięte kapsuły ukrywają tytuł i treść do daty otwarcia."
        )
        binding.btnCreateEnvelopeCapsule.text = AppLocale.t(
            "✉️ Створити капсулу часу (+30 XP)",
            "✉️ Create a Time Capsule (+30 XP)",
            "✉️ Utwórz kapsułę czasu (+30 XP)"
        )
        updateSubtabButtonsUi()
    }

    private fun simulateFriendReply() {
        binding.tvChatOnlineStatus.text = AppLocale.t(
            "✍️ Кацпер друкує...",
            "✍️ Kacper is typing...",
            "✍️ Kacper pisze..."
        )
        binding.tvChatOnlineStatus.setTextColor(AppTheme.theme.primaryColor)
        handler.postDelayed({
            if (_binding != null) {
                val reply = BestiesLogicHelper.getAutoFriendChatReplies().random()
                viewModel.sendChatMessage(sender = "Кацпер", text = reply)
                binding.tvChatOnlineStatus.text = AppLocale.t(
                    "● Друзі онлайн • Повідомлення, фото і капсули",
                    "● Squad online • Messages, photos & capsules",
                    "● Ekipa online • Wiadomości, zdjęcia i kapsuły"
                )
                binding.tvChatOnlineStatus.setTextColor("#10B981".toColorInt())
            }
        }, 1400L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
