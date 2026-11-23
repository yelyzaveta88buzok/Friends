package pl.edu.friends.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import pl.edu.friends.R
import pl.edu.friends.data.BestiesRepository
import pl.edu.friends.data.FriendshipProfileEntity
import pl.edu.friends.data.JournalEntryEntity
import pl.edu.friends.databinding.DialogSettingsSheetBinding
import pl.edu.friends.databinding.DialogStreakCalendarBinding
import pl.edu.friends.databinding.FragmentHomeBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.BestiesLogicHelper
import pl.edu.friends.util.Lang
import pl.edu.friends.util.RoomPrefs
import pl.edu.friends.util.RoomTheme
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BestiesViewModel by activityViewModels()

    private val timerHandler = Handler(Looper.getMainLooper())
    private var friendshipStartMillis: Long = System.currentTimeMillis() - (428L * 24L * 3600_000L)
    private var latestProfile: FriendshipProfileEntity? = null
    private var currentIdeaCategory: String = "ALL"
    private var activeSwitchedTab: Int = 0

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_binding != null) {
                val now = System.currentTimeMillis()
                val duration = BestiesLogicHelper.calculateFriendshipDuration(
                    friendshipStartMillis,
                    now
                )
                val seconds = ((now - friendshipStartMillis).coerceAtLeast(0L) / 1000L) % 60L

                binding.tvTimerDays.text = duration.days.toString()
                binding.tvTimerDaysUnit.text = AppLocale.t("дн.", "days", "dni")
                binding.tvLiveClockSub.text =
                    "⏱ %02d:%02d:%02d".format(duration.hours, duration.minutes, seconds)

                timerHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.miniMoodSparkline.compactMode = true
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnOpenSettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showSettingsBottomSheet()
        }

        binding.btnSquadCountPill.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showManageFriendsRoomDialog()
        }

        binding.cardBentoTimer.setOnClickListener {
            showStartDatePicker()
        }

        binding.cardBentoStreak.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showStreakCalendarBottomSheet()
        }

        binding.cardBentoMood.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            (activity as? MainActivity)?.navigateToTab(R.id.nav_journal)
        }

        binding.btnTabQuestion.setOnClickListener { switchBottomCardTab(0) }
        binding.btnTabRoulette.setOnClickListener { switchBottomCardTab(1) }

        binding.cardBentoCapsules.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            RoomPrefs.openEnvelopesNext = true
            (activity as? MainActivity)?.navigateToTab(R.id.nav_chat)
        }
        binding.btnHomeAddCapsule.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            (activity as? MainActivity)?.openQuickCapsuleDialog()
        }

        binding.btnCheckInToday.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showAnswerDailyRitualDialog()
        }

        binding.chipIdeaAll.setOnClickListener { selectIdeaCategory("ALL") }
        binding.chipIdeaHome.setOnClickListener { selectIdeaCategory("HOME") }
        binding.chipIdeaCity.setOnClickListener { selectIdeaCategory("CITY") }
        binding.chipIdeaActive.setOnClickListener { selectIdeaCategory("ACTIVE") }

        binding.btnDrawHangoutIdea.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val list = RoomPrefs.getIdeasByCategory(requireContext(), currentIdeaCategory)
            binding.tvHangoutIdea.text = list.random()
        }

        binding.btnAddCustomIdea.setOnClickListener {
            showAddCustomIdeaDialog()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun switchBottomCardTab(tabIndex: Int) {
        activeSwitchedTab = tabIndex
        val isQ = tabIndex == 0
        binding.layoutPaneQuestion.visibility = if (isQ) View.VISIBLE else View.GONE
        binding.layoutPaneRoulette.visibility = if (isQ) View.GONE else View.VISIBLE

        val ctx = requireContext()
        val theme = AppTheme.theme
        if (isQ) {
            binding.btnTabQuestion.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
            binding.btnTabQuestion.setTextColor(theme.primaryColor)
            binding.btnTabRoulette.background = null
            binding.btnTabRoulette.setTextColor("#6B7280".toColorInt())
        } else {
            binding.btnTabRoulette.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
            binding.btnTabRoulette.setTextColor(theme.primaryColor)
            binding.btnTabQuestion.background = null
            binding.btnTabQuestion.setTextColor("#6B7280".toColorInt())
        }
    }

    private fun showSettingsBottomSheet() {
        val ctx = requireContext()
        val dialog = BottomSheetDialog(ctx)
        val sheetBinding = DialogSettingsSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        fun refreshSheetUi() {
            val lang = AppLocale.lang
            val theme = AppTheme.theme
            val profile = latestProfile
            val members = profile?.membersCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.take(BestiesRepository.MAX_FRIENDS_PER_CODE)
                ?: listOf("Ліза", "Кацпер", "Аня")
            val code = profile?.roomCode ?: "FRND-7492"

            sheetBinding.tvSettingsTitle.text = AppLocale.t("⚙️ Налаштування простору", "⚙️ Space Settings", "⚙️ Ustawienia przestrzeni")
            sheetBinding.tvSettingsLangTitle.text = AppLocale.t("🌐 Мова інтерфейсу", "🌐 App Language", "🌐 Język aplikacji")
            sheetBinding.tvSettingsThemeTitle.text = AppLocale.t("🎨 Колір теми (6)", "🎨 Theme Color (6)", "🎨 Kolor motywu (6)")
            sheetBinding.tvSettingsThemeCurrent.text = theme.title()
            sheetBinding.tvSettingsThemeCurrent.setTextColor(theme.primaryColor)

            val langBtns = listOf(
                Lang.UA to sheetBinding.btnSheetLangUa,
                Lang.EN to sheetBinding.btnSheetLangEn,
                Lang.PL to sheetBinding.btnSheetLangPl
            )
            langBtns.forEach { (itemLang, btn) ->
                if (itemLang == lang) {
                    btn.background = AppTheme.pill(ctx, theme.primaryHex, 12f)
                    btn.setTextColor("#FFFFFF".toColorInt())
                } else {
                    btn.setBackgroundResource(R.drawable.bg_timer_box)
                    btn.setTextColor("#111827".toColorInt())
                }
            }

            sheetBinding.tvSettingsCodeLabel.text = AppLocale.t(
                "🔑 Код компанії (макс. 5 друзів)",
                "🔑 Squad code (max 5 friends)",
                "🔑 Kod paczki (maks. 5 przyjaciół)"
            )
            sheetBinding.tvSettingsCodeValue.text = "$code • ${members.size}/5"
            sheetBinding.tvSettingsMembersList.text = "👥 " + members.joinToString(" • ")
            sheetBinding.btnSettingsManageFriends.text = AppLocale.t("➕ Друг (до 5)", "➕ Friend (max 5)", "➕ Dodaj (do 5)")
            sheetBinding.btnSettingsManageFriends.background = AppTheme.pill(ctx, theme.softTintHex, 12f)
            sheetBinding.btnSettingsManageFriends.setTextColor(theme.primaryColor)

            sheetBinding.tvSettingsDateTitle.text = AppLocale.t("📅 Дата початку дружби", "📅 Friendship Start Date", "📅 Data początku przyjaźni")
            sheetBinding.btnSettingsChangeDate.text = AppLocale.t("Змінити дату", "Change date", "Zmień datę")
            sheetBinding.btnSettingsChangeDate.setTextColor(theme.primaryColor)

            sheetBinding.layoutSheetThemeSwatches.removeAllViews()
            RoomTheme.entries.forEachIndexed { idx, item ->
                val isSelected = item == theme
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(item.primaryColor)
                    setStroke(dp(if (isSelected) 3 else 1), (if (isSelected) "#111827" else "#D1D5DB").toColorInt())
                }
                val swatch = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                        if (idx < RoomTheme.entries.lastIndex) marginEnd = dp(8)
                    }
                    background = circle
                    gravity = Gravity.CENTER
                    text = if (isSelected) "✓" else ""
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor("#FFFFFF".toColorInt())
                    contentDescription = item.title()
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        AppTheme.setTheme(ctx, item)
                        refreshSheetUi()
                    }
                }
                sheetBinding.layoutSheetThemeSwatches.addView(swatch)
            }
        }

        sheetBinding.btnCloseSettings.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnSheetLangUa.setOnClickListener {
            AppLocale.setLanguage(ctx, Lang.UA)
            refreshSheetUi()
        }
        sheetBinding.btnSheetLangEn.setOnClickListener {
            AppLocale.setLanguage(ctx, Lang.EN)
            refreshSheetUi()
        }
        sheetBinding.btnSheetLangPl.setOnClickListener {
            AppLocale.setLanguage(ctx, Lang.PL)
            refreshSheetUi()
        }
        sheetBinding.btnSettingsManageFriends.setOnClickListener {
            dialog.dismiss()
            showManageFriendsRoomDialog()
        }
        sheetBinding.btnSettingsChangeDate.setOnClickListener {
            dialog.dismiss()
            showStartDatePicker()
        }

        refreshSheetUi()
        dialog.show()
    }

    private fun renderBentoAvatars(members: List<String>, checkedIn: Set<String>) {
        binding.layoutBentoAvatars.removeAllViews()
        val ctx = requireContext()
        val theme = AppTheme.theme

        members.forEachIndexed { idx, name ->
            val answered = checkedIn.any { it.equals(name, ignoreCase = true) }
            val avatarEmoji = RoomPrefs.getAvatar(ctx, name, idx)
            val ringBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#FFFFFF".toColorInt())
                setStroke(dp(2), (if (answered) "#10B981" else theme.cardStrokeHex).toColorInt())
            }
            val tvAvatar = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                    if (idx > 0) marginStart = dp(-7)
                }
                background = ringBg
                gravity = Gravity.CENTER
                text = avatarEmoji
                textSize = 14f
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    showFriendAvatarDialog(name, idx, isMe = (idx == 0))
                }
            }
            binding.layoutBentoAvatars.addView(tvAvatar)
        }
    }

    private fun renderWeekStreakDots(members: List<String>, checkedIn: Set<String>) {
        binding.layoutStreakWeekDots.removeAllViews()
        val ctx = requireContext()
        val theme = AppTheme.theme
        val allToday = members.isNotEmpty() && checkedIn.size >= members.size
        for (i in 0 until 7) {
            val active = (i < 6) || allToday
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor((if (active) theme.primaryColor else "#E5E7EB".toColorInt()))
            }
            val dot = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                    if (i < 6) marginEnd = dp(5)
                }
                background = dotBg
            }
            binding.layoutStreakWeekDots.addView(dot)
        }
    }

    private fun showFriendAvatarDialog(memberName: String, idx: Int, isMe: Boolean) {
        val ctx = requireContext()
        val currentAvatar = RoomPrefs.getAvatar(ctx, memberName, idx)

        if (isMe) {
            var chosenAvatar = currentAvatar
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(10), dp(18), dp(6))
            }

            val tvLabelAv = TextView(ctx).apply {
                text = AppLocale.t("🎭 Обери свою аватарку:", "🎭 Pick your avatar:", "🎭 Wybierz swój awatar:")
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor("#111827".toColorInt())
            }
            root.addView(tvLabelAv)

            val grid = GridLayout(ctx).apply {
                columnCount = 6
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }
            val emojiViews = mutableListOf<TextView>()
            fun refreshGrid() {
                emojiViews.forEachIndexed { i, tv ->
                    val em = RoomPrefs.AVATAR_CHOICES[i]
                    tv.background = if (em == chosenAvatar) {
                        AppTheme.pill(ctx, AppTheme.theme.softTintHex, 10f, 2f, AppTheme.theme.primaryHex)
                    } else {
                        AppTheme.pill(ctx, "#F3F4F6", 10f)
                    }
                }
            }
            RoomPrefs.AVATAR_CHOICES.forEach { emoji ->
                val tv = TextView(ctx).apply {
                    text = emoji
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setPadding(dp(4), dp(6), dp(4), dp(6))
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                    setOnClickListener {
                        chosenAvatar = emoji
                        refreshGrid()
                    }
                }
                emojiViews.add(tv)
                grid.addView(tv)
            }
            refreshGrid()
            root.addView(grid)

            val tvLabelBub = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                text = AppLocale.t("💬 Короткий статус дня:", "💬 Short daily status:", "💬 Krótki status dnia:")
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor("#111827".toColorInt())
            }
            root.addView(tvLabelBub)

            val etBubble = EditText(ctx).apply {
                hint = AppLocale.t("Напр. ☕ Хочу кави!", "e.g. ☕ Coffee?", "np. ☕ Kawa?")
                setText(RoomPrefs.getCustomBubble(ctx, memberName))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundResource(R.drawable.bg_chat_input)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }
            root.addView(etBubble)

            AlertDialog.Builder(ctx)
                .setTitle("$currentAvatar $memberName")
                .setView(root)
                .setPositiveButton(AppLocale.t("Зберегти ✨", "Save ✨", "Zapisz ✨")) { _, _ ->
                    RoomPrefs.setAvatar(ctx, memberName, chosenAvatar)
                    RoomPrefs.setCustomBubble(ctx, memberName, etBubble.text?.toString().orEmpty())
                    latestProfile?.let { bindProfileData(it) }
                    Toast.makeText(
                        ctx,
                        AppLocale.t("✨ Аватарку та статус оновлено!", "✨ Avatar & status updated!", "✨ Zaktualizowano awatar i status!"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
                .show()
        } else {
            AlertDialog.Builder(ctx)
                .setTitle("$currentAvatar $memberName")
                .setMessage(
                    AppLocale.t(
                        "Помахати $memberName у чаті чи змінити аватарку?",
                        "Wave to $memberName in chat or customize their avatar?",
                        "Pomachać do $memberName na czacie lub zmienić awatar?"
                    )
                )
                .setPositiveButton(AppLocale.t("👋 Помахати (+10 XP)", "👋 Wave (+10 XP)", "👋 Pomachaj (+10 XP)")) { _, _ ->
                    viewModel.addQuickActionXp()
                    viewModel.sendChatMessage(
                        sender = "Ty",
                        text = AppLocale.t(
                            "👋 Агов, $memberName! Як твій день? Заходь у чат ✨",
                            "👋 Hey $memberName! How's your day going? ✨",
                            "👋 Hej $memberName! Jak mija dzień? Wpadaj na czat ✨"
                        )
                    )
                    Toast.makeText(
                        ctx,
                        AppLocale.t("👋 Вітання для $memberName надіслано!", "👋 Wave sent to $memberName!", "👋 Wysłano pozdrowienie dla $memberName!"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNeutralButton(AppLocale.t("🎭 Аватарка", "🎭 Avatar", "🎭 Awatar")) { _, _ ->
                    showChooseFriendAvatarDialog(memberName)
                }
                .setNegativeButton(AppLocale.t("Закрити", "Close", "Zamknij"), null)
                .show()
        }
    }

    private fun showChooseFriendAvatarDialog(memberName: String) {
        val ctx = requireContext()
        val choices = RoomPrefs.AVATAR_CHOICES.toTypedArray()
        AlertDialog.Builder(ctx)
            .setTitle(AppLocale.t("🎭 Аватарка для $memberName", "🎭 Avatar for $memberName", "🎭 Awatar dla $memberName"))
            .setItems(choices) { _, which ->
                RoomPrefs.setAvatar(ctx, memberName, choices[which])
                latestProfile?.let { bindProfileData(it) }
            }
            .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
            .show()
    }

    private fun renderRitualAvatarsRow(members: List<String>, checkedIn: Set<String>) {
        binding.layoutRitualAvatarsRow.removeAllViews()
        val ctx = requireContext()
        val theme = AppTheme.theme

        members.forEachIndexed { idx, name ->
            val answered = checkedIn.any { it.equals(name, ignoreCase = true) }
            val avatar = RoomPrefs.getAvatar(ctx, name, idx)
            val chip = TextView(ctx).apply {
                background = if (answered) {
                    AppTheme.pill(ctx, "#ECFDF5", 999f, 1f, "#6EE7B7")
                } else {
                    AppTheme.pill(ctx, theme.pageBgHex, 999f, 1f, theme.cardStrokeHex)
                }
                setPadding(dp(10), dp(5), dp(10), dp(5))
                text = "$avatar $name ${if (answered) "✅" else "⏳"}"
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor("#111827".toColorInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx < members.lastIndex) marginEnd = dp(6)
                }
            }
            binding.layoutRitualAvatarsRow.addView(chip)
        }
    }

    private fun showAnswerDailyRitualDialog() {
        val question = binding.tvRitualQuestionText.text.toString()
        val input = EditText(requireContext()).apply {
            hint = AppLocale.t("Твоя відповідь...", "Your answer...", "Twoja odpowiedź...")
            minLines = 3
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_chat_input)
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(input)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(AppLocale.t("🔥 Питання дня (+15 XP)", "🔥 Question of the Day (+15 XP)", "🔥 Pytanie dnia (+15 XP)"))
            .setMessage(
                "$question\n\n" + AppLocale.t(
                    "🔒 Відповіді відкриються, коли всі друзі дадуть відповідь!",
                    "🔒 Answers unlock when friends respond!",
                    "🔒 Odpowiedzi odblokują się, gdy znajomi odpowiedzą!"
                )
            )
            .setView(container)
            .setPositiveButton(AppLocale.t("Надіслати 💌", "Send 💌", "Wyślij 💌")) { _, _ ->
                val ans = input.text?.toString()?.trim().orEmpty()
                viewModel.checkInToday()
                if (ans.isNotEmpty()) {
                    viewModel.sendChatMessage(
                        sender = "Ty",
                        text = "🔥 $question\n💬 $ans"
                    )
                }
                Toast.makeText(
                    requireContext(),
                    AppLocale.t(
                        "🔥 Всі друзі відповіли! Серію вогників продовжено (+15 XP)",
                        "🔥 All friends answered! Streak extended (+15 XP)",
                        "🔥 Wszyscy odpowiedzieli! Seria przedłużona (+15 XP)"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
            .show()
    }

    private fun showStreakCalendarBottomSheet() {
        val profile = latestProfile ?: return
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = DialogStreakCalendarBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        val theme = AppTheme.theme

        val members = profile.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .take(BestiesRepository.MAX_FRIENDS_PER_CODE)
        val checkedIn = profile.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        sheetBinding.tvModalStreakNumber.text = profile.streakDays.toString()
        sheetBinding.tvModalStreakNumber.setTextColor(theme.primaryColor)
        sheetBinding.tvModalStreakLabel.text = AppLocale.t("Днів поспіль", "Days in a row", "Dni z rzędu")
        sheetBinding.tvFreezeSectionTitle.text = AppLocale.t("Заморозка серії", "Streak Freeze", "Zamrożenie serii")
        sheetBinding.tvFreezeAvailable.text = AppLocale.t("ДОСТУПНО: 3", "AVAILABLE: 3", "DOSTĘPNE: 3")
        sheetBinding.tvFreezeAvailable.setTextColor(theme.primaryColor)
        sheetBinding.tvFreezeDesc.text = AppLocale.t(
            "Заморозка застосовується автоматично, якщо ви пропускаєте серію. Одна нова на місяць!",
            "Streak freeze is applied automatically if you miss a day. One new freeze per month!",
            "Zamrożenie jest stosowane automatycznie w razie pominięcia dnia. Jedno nowe na miesiąc!"
        )
        sheetBinding.tvCalendarSectionTitle.text = AppLocale.t("Календар", "Calendar", "Kalendarz")
        sheetBinding.tvCalLegendHint.text = AppLocale.t(
            "🔥 Всі друзі відповіли  •  🟡 Частина відповіла\nНатисни на день, щоб переглянути питання",
            "🔥 All friends answered  •  🟡 Partial\nTap any day to view its question & answers",
            "🔥 Wszyscy odpowiedzieli  •  🟡 Część odpowiedziała\nDotknij dnia, aby zobaczyć szczegóły"
        )
        sheetBinding.tvSelectedDayBadge.background = AppTheme.pill(requireContext(), theme.primaryHex)

        sheetBinding.btnCloseStreakModal.setOnClickListener { dialog.dismiss() }

        sheetBinding.layoutStreakPartnersRow.removeAllViews()
        members.forEachIndexed { idx, name ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setBackgroundResource(R.drawable.bg_timer_box)
                layoutParams = LinearLayout.LayoutParams(dp(105), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(10)
                }
            }
            val avatarCircle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(theme.softTintColor)
                setStroke(dp(2), theme.primaryColor)
            }
            val tvInitial = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { bottomMargin = dp(6) }
                background = avatarCircle
                gravity = Gravity.CENTER
                text = RoomPrefs.getAvatar(requireContext(), name, idx)
                textSize = 20f
            }
            val tvName = TextView(requireContext()).apply {
                text = name
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor("#111827".toColorInt())
            }
            val tvFire = TextView(requireContext()).apply {
                text = "${profile.streakDays} 🔥"
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(theme.primaryColor)
            }

            card.addView(tvInitial)
            card.addView(tvName)
            card.addView(tvFire)
            sheetBinding.layoutStreakPartnersRow.addView(card)
        }

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

        var calYear = todayYear
        var calMonth = todayMonth
        var selectedDay = todayDay

        val monthsUa = listOf("Січ", "Лют", "Бер", "Кві", "Тра", "Чер", "Лип", "Сер", "Вер", "Жов", "Лис", "Гру")
        val monthsEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthsPl = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        val dowLabels = when (AppLocale.lang) {
            Lang.UA -> listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "НД")
            Lang.EN -> listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
            Lang.PL -> listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")
        }
        for (i in 0 until sheetBinding.layoutCalDowRow.childCount.coerceAtMost(7)) {
            (sheetBinding.layoutCalDowRow.getChildAt(i) as? TextView)?.text = dowLabels[i]
        }

        fun getMonthName(m: Int): String = when (AppLocale.lang) {
            Lang.UA -> monthsUa[m]
            Lang.EN -> monthsEn[m]
            Lang.PL -> monthsPl[m]
        }

        fun updateSelectedDayDetail(day: Int, status: String) {
            val dateLabel = "$day ${getMonthName(calMonth)} $calYear"
            sheetBinding.tvSelectedDayDate.text = dateLabel
            val isToday = (calYear == todayYear && calMonth == todayMonth && day == todayDay)
            sheetBinding.tvSelectedDayBadge.text = when {
                isToday -> AppLocale.t("Сьогодні", "Today", "Dzisiaj")
                status == "both" -> AppLocale.t("🔥 Серія", "🔥 Streak", "🔥 Seria")
                status == "one" -> AppLocale.t("🟡 Частково", "🟡 Partial", "🟡 Częściowo")
                else -> AppLocale.t("Без серії", "No streak", "Brak serii")
            }

            val questions = BestiesLogicHelper.getDailyQuestions()
            val qForDay = questions[day % questions.size]
            sheetBinding.tvSelectedDayQuestion.text = qForDay

            val sampleAnswers = listOf(
                AppLocale.t("Вечірній кіномарафон і піца з друзями! 🍕", "Movie marathon and pizza night with friends! 🍕", "Wieczorny maraton filmowy i pizza! 🍕"),
                AppLocale.t("Спонтанна поїздка та розмови до ночі 🌆", "Spontaneous trip and late-night talks 🌆", "Spontaniczny wypad i rozmowy do nocy 🌆"),
                AppLocale.t("Спільна кава перед парами і багато сміху ☕", "Morning coffee before class and lots of laughs ☕", "Wspólna kawa przed zajęciami i dużo śmiechu ☕")
            )

            val lines = members.mapIndexed { idx, member ->
                val memberDone = when (status) {
                    "both" -> true
                    "one" -> if (isToday) checkedIn.any { it.equals(member, ignoreCase = true) } else (idx == 0)
                    else -> false
                }
                if (memberDone) {
                    "✅ $member: «${sampleAnswers[idx % sampleAnswers.size]}»"
                } else {
                    "⏳ $member: " + AppLocale.t("Ще не відповів(-ла)", "Hasn't answered yet", "Jeszcze nie odpowiedział(a)")
                }
            }
            sheetBinding.tvSelectedDayWhoCheckedIn.text = lines.joinToString("\n")
        }

        fun renderMonthGrid() {
            sheetBinding.tvCalMonthLabel.text = "${getMonthName(calMonth)} $calYear"
            sheetBinding.gridCalendarDays.removeAllViews()

            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, calYear)
                set(Calendar.MONTH, calMonth)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val firstDow = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val totalCells = firstDow + daysInMonth
            for (cellIdx in 0 until totalCells) {
                val dayNum = if (cellIdx < firstDow) null else (cellIdx - firstDow + 1)

                val cellLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(2), dp(6), dp(2), dp(6))
                    minimumHeight = dp(46)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(cellIdx % 7, 1f)
                        rowSpec = GridLayout.spec(cellIdx / 7)
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                }

                if (dayNum != null) {
                    val isFuture = (calYear > todayYear) ||
                        (calYear == todayYear && calMonth > todayMonth) ||
                        (calYear == todayYear && calMonth == todayMonth && dayNum > todayDay)

                    val isCurrentMonth = (calYear == todayYear && calMonth == todayMonth)
                    val isToday = isCurrentMonth && (dayNum == todayDay)
                    val allCheckedToday = checkedIn.size >= members.size

                    val status = when {
                        isFuture -> "future"
                        isToday -> if (allCheckedToday) "both" else "one"
                        isCurrentMonth && (todayDay - dayNum) in 1..profile.streakDays -> "both"
                        isCurrentMonth && (todayDay - dayNum) == profile.streakDays + 1 -> "one"
                        !isCurrentMonth && (dayNum % 3 != 0) -> "both"
                        !isCurrentMonth && (dayNum % 3 == 0) -> "one"
                        else -> "none"
                    }

                    val isSelected = (dayNum == selectedDay)
                    if (isSelected) {
                        cellLayout.background = AppTheme.pill(requireContext(), theme.softTintHex, 12f)
                    }

                    val tvDayNum = TextView(requireContext()).apply {
                        text = dayNum.toString()
                        textSize = 13f
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(
                            when {
                                isFuture -> "#CCCCCC".toColorInt()
                                isSelected -> theme.primaryColor
                                status == "none" -> "#9CA3AF".toColorInt()
                                else -> "#111827".toColorInt()
                            }
                        )
                    }

                    val tvIcon = TextView(requireContext()).apply {
                        text = when (status) {
                            "both" -> "🔥"
                            "one" -> "🟡"
                            else -> " "
                        }
                        textSize = 12f
                        gravity = Gravity.CENTER
                    }

                    cellLayout.addView(tvDayNum)
                    cellLayout.addView(tvIcon)

                    if (!isFuture) {
                        cellLayout.setOnClickListener {
                            selectedDay = dayNum
                            renderMonthGrid()
                            updateSelectedDayDetail(dayNum, status)
                        }
                    }
                }

                sheetBinding.gridCalendarDays.addView(cellLayout)
            }

            val selStatus = if (calYear == todayYear && calMonth == todayMonth && selectedDay == todayDay) {
                if (checkedIn.size >= members.size) "both" else "one"
            } else {
                "both"
            }
            updateSelectedDayDetail(selectedDay.coerceIn(1, daysInMonth), selStatus)
        }

        sheetBinding.btnPrevMonth.setOnClickListener {
            if (calMonth == 0) {
                calMonth = 11
                calYear -= 1
            } else {
                calMonth -= 1
            }
            selectedDay = 1
            renderMonthGrid()
        }

        sheetBinding.btnNextMonth.setOnClickListener {
            if (calMonth == 11) {
                calMonth = 0
                calYear += 1
            } else {
                calMonth += 1
            }
            selectedDay = 1
            renderMonthGrid()
        }

        renderMonthGrid()
        dialog.show()
    }

    private fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = friendshipStartMillis
        }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }
                val newMillis = chosen.timeInMillis.coerceAtMost(System.currentTimeMillis())
                viewModel.updateFriendshipStartDate(newMillis)
                Toast.makeText(
                    requireContext(),
                    AppLocale.t("📅 Дату початку дружби оновлено!", "📅 Friendship start date updated!", "📅 Zaktualizowano datę początku przyjaźni!"),
                    Toast.LENGTH_SHORT
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun showManageFriendsRoomDialog() {
        val profile = latestProfile ?: return
        val members = profile.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val input = EditText(requireContext()).apply {
            hint = AppLocale.t("Ім'я нового друга (напр. Макс)", "New friend's name (e.g. Max)", "Imię przyjaciela (np. Maks)")
            setPadding(40, 30, 40, 30)
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 8)
            addView(input)
        }

        val currentListText = members.mapIndexed { i, name -> "${i + 1}. $name" }.joinToString("\n")
        val msg = AppLocale.t(
            "Код вашої компанії: ${profile.roomCode}\nМаксимум одночасно може приєднатися 5 друзів.\n\nЗараз у компанії (${members.size}/5):\n$currentListText",
            "Your squad code: ${profile.roomCode}\nMaximum 5 friends can join one code.\n\nCurrently in squad (${members.size}/5):\n$currentListText",
            "Kod Waszej paczki: ${profile.roomCode}\nMaksymalnie do jednego kodu może dołączyć 5 przyjaciół.\n\nObecnie w ekipie (${members.size}/5):\n$currentListText"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(AppLocale.t("👥 Компанія друзів (до 5 осіб)", "👥 Friends Squad (max 5)", "👥 Grupa przyjaciół (maks. 5)"))
            .setMessage(msg)
            .setView(container)
            .setPositiveButton(AppLocale.t("Приєднати друга", "Add friend", "Dodaj przyjaciela")) { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isEmpty()) return@setPositiveButton
                if (members.size >= BestiesRepository.MAX_FRIENDS_PER_CODE) {
                    Toast.makeText(
                        requireContext(),
                        AppLocale.t("⚠️ Досягнуто ліміт: максимум 5 друзів на один код!", "⚠️ Limit reached: max 5 friends per code!", "⚠️ Osiągnięto limit: maksymalnie 5 przyjaciół na jeden kod!"),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    viewModel.addFriendToRoom(newName) { added ->
                        if (added) {
                            Toast.makeText(
                                requireContext(),
                                AppLocale.t("🎉 $newName приєднався(-лася) до коду ${profile.roomCode}! (+25 XP)", "🎉 $newName joined code ${profile.roomCode}! (+25 XP)", "🎉 $newName dołączył(a) do kodu ${profile.roomCode}! (+25 XP)"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNeutralButton(AppLocale.t("Видалити останнього", "Remove last", "Usuń ostatniego")) { _, _ ->
                if (members.size > 1) {
                    viewModel.removeFriendFromRoom(members.last())
                }
            }
            .setNegativeButton(AppLocale.t("Закрити", "Close", "Zamknij"), null)
            .show()
    }

    private fun selectIdeaCategory(category: String) {
        currentIdeaCategory = category
        val ctx = requireContext()
        val theme = AppTheme.theme
        val chips = listOf(
            "ALL" to binding.chipIdeaAll,
            "HOME" to binding.chipIdeaHome,
            "CITY" to binding.chipIdeaCity,
            "ACTIVE" to binding.chipIdeaActive
        )
        chips.forEach { (key, chip) ->
            val active = (key == category)
            if (active) {
                chip.background = AppTheme.pill(ctx, theme.primaryHex)
                chip.setTextColor("#FFFFFF".toColorInt())
            } else {
                chip.setBackgroundResource(R.drawable.bg_timer_box)
                chip.setTextColor(ctx.getColor(R.color.text_primary))
            }
        }
        binding.tvHangoutIdea.text = RoomPrefs.getIdeasByCategory(ctx, category).first()
    }

    private fun showAddCustomIdeaDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = AppLocale.t("Напр. Пікнік на набережній або фільм 🎬", "e.g. Sunset picnic or movie marathon 🎬", "np. Piknik lub maraton filmowy 🎬")
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_chat_input)
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(input)
        }
        AlertDialog.Builder(ctx)
            .setTitle(AppLocale.t("➕ Додати свою ідею для компанії", "➕ Add Custom Squad Idea", "➕ Dodaj własny pomysł"))
            .setView(box)
            .setPositiveButton(AppLocale.t("Додати ✨", "Add ✨", "Dodaj ✨")) { _, _ ->
                val txt = input.text?.toString()?.trim().orEmpty()
                if (txt.isNotEmpty()) {
                    RoomPrefs.addCustomIdea(ctx, currentIdeaCategory, txt)
                    binding.tvHangoutIdea.text = "⭐ $txt"
                    Toast.makeText(
                        ctx,
                        AppLocale.t("⭐ Ідею додано до вашої рулетки!", "⭐ Idea added to roulette!", "⭐ Dodano pomysł do ruletki!"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(AppLocale.t("Скасувати", "Cancel", "Anuluj"), null)
            .show()
    }

    private fun renderLocalizedTexts() {
        binding.tvHomeHeaderTitle.text = AppLocale.t("Друзі", "Friends", "Przyjaciele")
        binding.tvTimerEyebrow.text = AppLocale.t("РАЗОМ ✏️", "TOGETHER ✏️", "RAZEM ✏️")
        binding.tvHomeCapsulesTitle.text = AppLocale.t("Капсули часу", "Time Capsules", "Kapsuły czasu")
        binding.btnHomeAddCapsule.text = AppLocale.t("+ Нова", "+ New", "+ Nowa")
        binding.tvBentoStreakLabel.text = AppLocale.t("Вогник", "Streak", "Seria")
        binding.tvBentoStreakCalendarLink.text = AppLocale.t("Календар ›", "Calendar ›", "Kalendarz ›")
        binding.tvBentoMoodLabel.text = AppLocale.t("Настрій", "Mood", "Nastrój")

        binding.btnTabQuestion.text = AppLocale.t("🔥 Питання", "🔥 Question", "🔥 Pytanie")
        binding.btnTabRoulette.text = AppLocale.t("🎲 Рулетка", "🎲 Roulette", "🎲 Ruletka")
        binding.tvRitualQuestionText.text = BestiesLogicHelper.getDailyQuestions().first()
        binding.tvQuestionXpHint.text = AppLocale.t(
            "🔒 Відповіді відкриються разом",
            "🔒 Answers unlock together",
            "🔒 Odpowiedzi otworzą się razem"
        )
        binding.btnCheckInToday.text = AppLocale.t("Відповісти", "Answer", "Odpowiedz")

        binding.chipIdeaAll.text = AppLocale.t("✨ Всі", "✨ All", "✨ Wszystkie")
        binding.chipIdeaHome.text = AppLocale.t("🏠 Вдома", "🏠 Home", "🏠 W domu")
        binding.chipIdeaCity.text = AppLocale.t("☕ У місті", "☕ City", "☕ Na mieście")
        binding.chipIdeaActive.text = AppLocale.t("🎉 Активно", "🎉 Active", "🎉 Aktywnie")
        binding.btnDrawHangoutIdea.text = AppLocale.t("Крутити ще 🎲", "Spin again 🎲", "Kręć ponownie 🎲")
        binding.btnAddCustomIdea.text = AppLocale.t("➕ Своя", "➕ Custom", "➕ Własny")

        latestProfile?.let { bindProfileData(it) }
        bindCapsulesSummary(viewModel.allCapsules.value.orEmpty())
        applyTheme(AppTheme.theme)
    }

    private fun applyTheme(theme: RoomTheme) {
        val ctx = requireContext()
        binding.homeRootScroll.setBackgroundColor(theme.pageBgColor)
        binding.progressXp.setIndicatorColor(theme.primaryColor)
        binding.cardBentoTimer.background = AppTheme.bentoHeroDrawable(
            ctx,
            theme.softTintHex,
            "#FFFFFF",
            theme.cardStrokeHex,
            22f
        )
        binding.tvTimerEyebrow.setTextColor(theme.primaryColor)
        binding.tvLiveClockSub.background = AppTheme.pill(ctx, "#FFFFFF", 999f, 1f, theme.cardStrokeHex)
        binding.tvLiveClockSub.setTextColor(theme.primaryColor)
        binding.tvHomeCapsuleIcon.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.btnHomeAddCapsule.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
        binding.btnHomeAddCapsule.setTextColor(theme.primaryColor)
        binding.tvBentoMoodScore.setTextColor(theme.primaryColor)
        binding.miniMoodSparkline.setTheme(theme)
        binding.btnCheckInToday.backgroundTintList = ColorStateList.valueOf(theme.primaryColor)
        binding.btnDrawHangoutIdea.backgroundTintList = ColorStateList.valueOf(theme.primaryColor)

        switchBottomCardTab(activeSwitchedTab)
        selectIdeaCategory(currentIdeaCategory)
        latestProfile?.let { profile ->
            val members = profile.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(BestiesRepository.MAX_FRIENDS_PER_CODE)
            val checkedIn = profile.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            renderBentoAvatars(members, checkedIn)
            renderWeekStreakDots(members, checkedIn)
        }
    }

    private fun bindProfileData(profile: FriendshipProfileEntity) {
        friendshipStartMillis = profile.startDateMillis
        val levelTitle = BestiesLogicHelper.getLocalizedLevelTitle(profile.xpPoints)
        binding.tvFriendshipLevel.text = "${levelTitle.substringBefore(":")} • ${profile.xpPoints} XP"
        binding.progressXp.progress = profile.xpPoints.coerceIn(20, 500)

        val dUnit = AppLocale.t("дн.", "d", "dni")
        binding.tvStreakDays.text = "🔥 ${profile.streakDays} $dUnit"

        val members = profile.membersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .take(BestiesRepository.MAX_FRIENDS_PER_CODE)
        val checkedIn = profile.checkedInMembersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        binding.btnSquadCountPill.text = "👥 ${members.size}/5"
        binding.tvBentoSquadCount.text = AppLocale.t(
            "${members.size}/5 у компанії",
            "${members.size}/5 in squad",
            "${members.size}/5 w ekipie"
        )
        binding.tvBentoAvatarsHint.text = members.joinToString(" • ") + " ›"
        binding.tvBentoMoodSummary.text = "${profile.myMood} • ${profile.friendMood}"

        renderBentoAvatars(members, checkedIn)
        renderWeekStreakDots(members, checkedIn)
        renderRitualAvatarsRow(members, checkedIn)

        timerHandler.removeCallbacks(timerRunnable)
        timerRunnable.run()
    }

    private fun bindMoodMiniChart(entries: List<JournalEntryEntity>) {
        val recent = entries.distinctBy { it.dateLabel }.take(7).reversed()
        val fallback = listOf(
            DayWarmthPoint("Пн", 4f, 4f),
            DayWarmthPoint("Вт", 5f, 4f),
            DayWarmthPoint("Ср", 3.5f, 4f),
            DayWarmthPoint("Чт", 4.5f, 5f),
            DayWarmthPoint("Пт", 5f, 4f),
            DayWarmthPoint("Сб", 4.5f, 5f),
            DayWarmthPoint("Нд", 5f, 5f)
        )
        val wavePoints = if (recent.isEmpty()) {
            fallback
        } else {
            val missing = (7 - recent.size).coerceAtLeast(0)
            val padded = fallback.take(missing).toMutableList()
            recent.forEach { item ->
                padded.add(
                    DayWarmthPoint(
                        dayLabel = item.dateLabel.take(5),
                        myScore = item.myMoodScore.coerceIn(1, 5).toFloat(),
                        friendScore = item.friendMoodScore.coerceIn(1, 5).toFloat()
                    )
                )
            }
            padded
        }
        binding.miniMoodSparkline.setData(wavePoints, animate = false)
        val avg = wavePoints.map { (it.myScore + it.friendScore) / 2.0 }.average()
        val topEmoji = when {
            avg >= 4.5 -> "🤩"
            avg >= 3.5 -> "😊"
            else -> "😐"
        }
        binding.tvBentoMoodScore.text = "${String.format(Locale.US, "%.1f", avg)} $topEmoji"
    }

    private fun bindCapsulesSummary(capsules: List<pl.edu.friends.data.TimeCapsuleEntity>) {
        val now = System.currentTimeMillis()
        val unlockedCount = capsules.count { BestiesLogicHelper.isCapsuleUnlocked(it, now) }
        val lockedCount = capsules.size - unlockedCount
        binding.tvHomeCapsulesStatus.text = if (capsules.isEmpty()) {
            AppLocale.t(
                "Створіть першу капсулу у майбутнє ✨",
                "Create your first time capsule ✨",
                "Utwórz pierwszą kapsułę czasu ✨"
            )
        } else {
            AppLocale.t(
                "🔓 $unlockedCount відкрито • 🔒 $lockedCount закрито",
                "🔓 $unlockedCount open • 🔒 $lockedCount sealed",
                "🔓 $unlockedCount otwarte • 🔒 $lockedCount zamknięte"
            )
        }
    }

    private fun observeViewModel() {
        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                latestProfile = profile
                bindProfileData(profile)
            }
        }

        viewModel.allCapsules.observe(viewLifecycleOwner) { capsules ->
            bindCapsulesSummary(capsules)
        }

        viewModel.allJournalEntries.observe(viewLifecycleOwner) { entries ->
            bindMoodMiniChart(entries)
        }

        AppLocale.currentLang.observe(viewLifecycleOwner) {
            renderLocalizedTexts()
        }

        AppTheme.currentTheme.observe(viewLifecycleOwner) { theme ->
            applyTheme(theme)
        }
    }

    override fun onResume() {
        super.onResume()
        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        _binding = null
    }
}
