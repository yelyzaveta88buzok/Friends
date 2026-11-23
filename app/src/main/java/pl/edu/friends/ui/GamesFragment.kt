package pl.edu.friends.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import pl.edu.friends.R
import pl.edu.friends.data.BestiesRepository
import pl.edu.friends.data.QuizAnswerEntity
import pl.edu.friends.databinding.FragmentGamesBinding
import pl.edu.friends.util.AppLocale
import pl.edu.friends.util.AppTheme
import pl.edu.friends.util.BestiesLogicHelper
import pl.edu.friends.util.GameQuestion
import pl.edu.friends.util.RoomPrefs
import pl.edu.friends.util.RoomTheme

class GamesFragment : Fragment() {

    private var _binding: FragmentGamesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BestiesViewModel by activityViewModels()

    private var currentGameType = "NEVER"
    private var currentIndex = 0
    private var currentFilter = "ALL"
    private var cachedNeverAnswers: List<QuizAnswerEntity> = emptyList()
    private var squadMembers: List<String> = listOf("Ліза", "Кацпер", "Аня")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardTopicNever.setOnClickListener { openGame("NEVER") }
        binding.cardTopicWho.setOnClickListener { openGame("WHO") }
        binding.cardTopicThisThat.setOnClickListener { openGame("THIS_OR_THAT") }
        binding.cardTopicDeepTalk.setOnClickListener { openGame("DEEP_TALK") }

        binding.btnBackToGamesHub.setOnClickListener {
            binding.layoutGamePlay.visibility = View.GONE
            binding.layoutGamesHub.visibility = View.VISIBLE
        }

        binding.btnFilterAll.setOnClickListener { applyFilter("ALL") }
        binding.btnFilterStarted.setOnClickListener { applyFilter("POPULAR") }
        binding.btnFilterNew.setOnClickListener { applyFilter("DEEP") }

        binding.cardNeverStatsBanner.setOnClickListener {
            showNeverStatsDialog()
        }

        binding.btnAnswerOption1.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleAnswer(isOptionA = true)
        }
        binding.btnAnswerOption2.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleAnswer(isOptionA = false)
        }

        binding.btnShuffleQuestion.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val deck = getCurrentDeck()
            if (deck.size > 1) {
                var nextIdx = (deck.indices).random()
                if (nextIdx == currentIndex) {
                    nextIdx = (currentIndex + 1) % deck.size
                }
                currentIndex = nextIdx
                RoomPrefs.setGameQuestionIndex(requireContext(), currentGameType, currentIndex)
                renderCurrentQuestion()
            }
        }

        binding.btnNextQuestion.setOnClickListener {
            val deck = getCurrentDeck()
            currentIndex = (currentIndex + 1) % deck.size
            RoomPrefs.setGameQuestionIndex(requireContext(), currentGameType, currentIndex)
            renderCurrentQuestion()
        }

        viewModel.neverGameAnswers.observe(viewLifecycleOwner) { answers ->
            cachedNeverAnswers = answers
            updateStatsUi(answers)
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            val parsed = profile?.membersCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.take(BestiesRepository.MAX_FRIENDS_PER_CODE)
                .orEmpty()
            if (parsed.isNotEmpty()) {
                squadMembers = parsed
                if (binding.layoutGamePlay.visibility == View.VISIBLE && currentGameType == "WHO") {
                    renderWhoFriendButtons()
                }
            }
        }

        AppLocale.currentLang.observe(viewLifecycleOwner) {
            applyLocalization()
            updateStatsUi(cachedNeverAnswers)
            if (binding.layoutGamePlay.visibility == View.VISIBLE) {
                renderCurrentQuestion()
            }
        }

        AppTheme.currentTheme.observe(viewLifecycleOwner) { theme ->
            applyTheme(theme)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun applyTheme(theme: RoomTheme) {
        val ctx = requireContext()
        binding.gamesRootScroll.setBackgroundColor(theme.pageBgColor)
        binding.tvGamesTitle.setTextColor("#111827".toColorInt())
        binding.tvGamesBadge.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
        binding.tvGamesBadge.setTextColor(theme.primaryColor)
        binding.cardNeverStatsBanner.strokeColor = theme.cardStrokeColor
        binding.tvNeverBannerSub.setTextColor(theme.primaryColor)

        listOf(
            binding.tvTopicNeverStart,
            binding.tvTopicWhoStart,
            binding.tvTopicThisThatStart,
            binding.tvTopicDeepTalkStart
        ).forEach { badge ->
            badge.background = AppTheme.pill(ctx, theme.softTintHex, 999f)
            badge.setTextColor(theme.primaryColor)
        }

        binding.btnAnswerOption1.backgroundTintList = ColorStateList.valueOf(theme.primaryColor)
        binding.btnBackToGamesHub.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.btnBackToGamesHub.setTextColor(theme.primaryColor)
        binding.btnShuffleQuestion.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.btnShuffleQuestion.setTextColor(theme.primaryColor)
        binding.tvGameCategory.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.tvGameCategory.setTextColor(theme.primaryColor)
        binding.tvPartnerAnswerInfo.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.btnNextQuestion.setTextColor(theme.primaryColor)
        binding.tvStatsHeader.setTextColor(theme.primaryColor)
        binding.tvMatchPercentage.background = AppTheme.pill(ctx, theme.softTintHex, 14f)
        binding.tvMatchPercentage.setTextColor(theme.primaryColor)
        applyFilter(currentFilter)
        if (binding.layoutGamePlay.visibility == View.VISIBLE && currentGameType == "WHO") {
            renderWhoFriendButtons()
        }
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter
        val allActive = filter == "ALL"
        val popActive = filter == "POPULAR"
        val deepActive = filter == "DEEP"
        val ctx = requireContext()
        val theme = AppTheme.theme

        if (allActive) {
            binding.btnFilterAll.background = AppTheme.pill(ctx, theme.primaryHex)
        } else {
            binding.btnFilterAll.setBackgroundResource(R.drawable.bg_timer_box)
        }
        binding.btnFilterAll.setTextColor(ctx.getColor(if (allActive) R.color.surface_white else R.color.text_secondary))

        if (popActive) {
            binding.btnFilterStarted.background = AppTheme.pill(ctx, theme.primaryHex)
        } else {
            binding.btnFilterStarted.setBackgroundResource(R.drawable.bg_timer_box)
        }
        binding.btnFilterStarted.setTextColor(ctx.getColor(if (popActive) R.color.surface_white else R.color.text_secondary))

        if (deepActive) {
            binding.btnFilterNew.background = AppTheme.pill(ctx, theme.primaryHex)
        } else {
            binding.btnFilterNew.setBackgroundResource(R.drawable.bg_timer_box)
        }
        binding.btnFilterNew.setTextColor(ctx.getColor(if (deepActive) R.color.surface_white else R.color.text_secondary))

        binding.cardTopicNever.visibility = if (allActive || popActive) View.VISIBLE else View.GONE
        binding.cardTopicWho.visibility = if (allActive || popActive) View.VISIBLE else View.GONE
        binding.cardTopicThisThat.visibility = if (allActive || deepActive) View.VISIBLE else View.GONE
        binding.cardTopicDeepTalk.visibility = if (allActive || deepActive) View.VISIBLE else View.GONE
    }

    private fun openGame(gameType: String) {
        currentGameType = gameType
        val deckSize = getCurrentDeck().size.coerceAtLeast(1)
        currentIndex = RoomPrefs.getGameQuestionIndex(requireContext(), gameType) % deckSize
        binding.layoutGamesHub.visibility = View.GONE
        binding.layoutGamePlay.visibility = View.VISIBLE
        binding.tvPartnerAnswerInfo.text = AppLocale.t(
            "Обери відповідь, щоб порівняти результат із друзями!",
            "Pick an answer to compare with your friends!",
            "Wybierz odpowiedź, aby porównać wynik z przyjaciółmi!"
        )
        renderCurrentQuestion()
    }

    private fun getCurrentDeck(): List<GameQuestion> = when (currentGameType) {
        "WHO" -> BestiesLogicHelper.getWhoQuestions()
        "THIS_OR_THAT" -> BestiesLogicHelper.getThisOrThatQuestions()
        "DEEP_TALK" -> BestiesLogicHelper.getDeepTalkQuestions()
        else -> BestiesLogicHelper.getNeverQuestions()
    }

    private fun renderCurrentQuestion() {
        val deck = getCurrentDeck()
        val item = deck[currentIndex % deck.size]

        binding.tvQuestionProgress.text = AppLocale.t(
            "Питання ${(currentIndex % deck.size) + 1} / ${deck.size}",
            "Question ${(currentIndex % deck.size) + 1} / ${deck.size}",
            "Pytanie ${(currentIndex % deck.size) + 1} / ${deck.size}"
        )

        binding.tvGameCategory.text = when (currentGameType) {
            "WHO" -> AppLocale.t("РЕЖИМ: 😂 ХТО З НАС ЧАСТІШЕ? (+20 XP)", "MODE: 😂 MOST LIKELY TO? (+20 XP)", "TRYB: 😂 KTO Z NAS? (+20 XP)")
            "THIS_OR_THAT" -> AppLocale.t("РЕЖИМ: 🎲 ЦЕ ЧИ ТЕ? (+20 XP)", "MODE: 🎲 THIS OR THAT (+20 XP)", "TRYB: 🎲 TO CZY TAMTO (+20 XP)")
            "DEEP_TALK" -> AppLocale.t("РЕЖИМ: 🌙 РОЗМОВИ У ТЕМРЯВІ (+20 XP)", "MODE: 🌙 DEEP TALKS (+20 XP)", "TRYB: 🌙 NOCNE ROZMOWY (+20 XP)")
            else -> AppLocale.t("РЕЖИМ: 🔥 Я НІКОЛИ НЕ... (+20 XP)", "MODE: 🔥 NEVER HAVE I EVER (+20 XP)", "TRYB: 🔥 NIGDY NIE... (+20 XP)")
        }

        binding.tvGameQuestion.text = item.question
        if (currentGameType == "WHO") {
            binding.layoutBinaryOptions.visibility = View.GONE
            binding.layoutWhoFriendsOptions.visibility = View.VISIBLE
            renderWhoFriendButtons()
        } else {
            binding.layoutWhoFriendsOptions.visibility = View.GONE
            binding.layoutBinaryOptions.visibility = View.VISIBLE
            binding.btnAnswerOption1.text = item.optionA
            binding.btnAnswerOption2.text = item.optionB
        }
    }

    private fun renderWhoFriendButtons() {
        binding.layoutWhoFriendsOptions.removeAllViews()
        val ctx = requireContext()
        val theme = AppTheme.theme
        val rows = squadMembers.chunked(2)

        rows.forEachIndexed { rowIdx, rowMembers ->
            val rowLayout = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (rowIdx < rows.lastIndex) bottomMargin = dp(8)
                }
            }

            rowMembers.forEachIndexed { colIdx, memberName ->
                val memberIdx = rowIdx * 2 + colIdx
                val avatar = RoomPrefs.getAvatar(ctx, memberName, memberIdx)
                val btnCard = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        if (colIdx == 0 && rowMembers.size > 1) marginEnd = dp(6)
                    }
                    background = AppTheme.pill(ctx, theme.softTintHex, 16f, 1.5f, theme.primaryHex)
                    gravity = Gravity.CENTER
                    text = "$avatar $memberName"
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(theme.primaryDarkColor)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        handleWhoVote(memberName, avatar)
                    }
                }
                rowLayout.addView(btnCard)
            }
            binding.layoutWhoFriendsOptions.addView(rowLayout)
        }
    }

    private fun handleWhoVote(chosenMember: String, chosenAvatar: String) {
        val deck = getCurrentDeck()
        val item = deck[currentIndex % deck.size]
        val ctx = requireContext()

        val voteCounts = squadMembers.associateWith { 0 }.toMutableMap()
        voteCounts[chosenMember] = 1
        squadMembers.drop(1).forEach { _ ->
            val target = if ((0..10).random() < 7) chosenMember else squadMembers.random()
            voteCounts[target] = (voteCounts[target] ?: 0) + 1
        }

        val winnerEntry = voteCounts.maxByOrNull { it.value }
        val winnerName = winnerEntry?.key ?: chosenMember
        val winnerIdx = squadMembers.indexOf(winnerName).coerceAtLeast(0)
        val winnerAvatar = RoomPrefs.getAvatar(ctx, winnerName, winnerIdx)
        val winnerVotes = winnerEntry?.value ?: 1

        viewModel.submitGameAnswer(
            gameType = "WHO",
            questionText = item.question,
            myAnswer = chosenMember,
            friendAnswer = winnerName
        )

        val yourVoteLbl = AppLocale.t("Твій голос", "Your vote", "Twój głos")
        val squadWinnerLbl = AppLocale.t("Лідер компанії", "Squad pick", "Wybór ekipy")
        val votesWord = AppLocale.t("гол.", "votes", "gł.")
        binding.tvPartnerAnswerInfo.text =
            "🗳️ $yourVoteLbl: $chosenAvatar $chosenMember\n🏆 $squadWinnerLbl: $winnerAvatar $winnerName ($winnerVotes/${squadMembers.size} $votesWord) • +20 XP"
    }

    private fun handleAnswer(isOptionA: Boolean) {
        val deck = getCurrentDeck()
        val item = deck[currentIndex % deck.size]
        val myChoice = if (isOptionA) item.optionA else item.optionB
        val friendChoice = if ((0..10).random() < 7) myChoice else (if (isOptionA) item.optionB else item.optionA)

        val cleanMy = if (currentGameType == "NEVER") {
            if (isOptionA) "BYŁO!" else "NIGDY"
        } else {
            myChoice
        }
        val cleanFriend = if (currentGameType == "NEVER") {
            if (friendChoice == item.optionA) "BYŁO!" else "NIGDY"
        } else {
            friendChoice
        }

        viewModel.submitGameAnswer(
            gameType = currentGameType,
            questionText = item.question,
            myAnswer = cleanMy,
            friendAnswer = cleanFriend
        )

        val matchLabel = if (cleanMy == cleanFriend) {
            AppLocale.t("🎉 Повний збіг у компанії!", "🎉 Full squad match!", "🎉 Pełna zgodność!")
        } else {
            AppLocale.t("😄 Думки друзів розділилися!", "😄 Different answers!", "😄 Różne odpowiedzi!")
        }

        val youWord = AppLocale.t("Ти", "You", "Ty")
        val partnerName = squadMembers.getOrElse(1) {
            squadMembers.firstOrNull() ?: AppLocale.t("Друг", "Friend", "Przyjaciel")
        }
        binding.tvPartnerAnswerInfo.text =
            "$youWord: $myChoice  •  $partnerName: $friendChoice\n$matchLabel (+20 XP)"
    }

    private fun updateStatsUi(answers: List<QuizAnswerEntity>) {
        val stats = BestiesLogicHelper.calculateNeverMatchStats(answers)
        binding.tvNeverBannerSub.text = AppLocale.t(
            "🔥 Разом БУЛО: ${stats.bothByloCount} • 🎯 Збіг: ${stats.matchPercentage}% (натисни)",
            "🔥 Both HAVE: ${stats.bothByloCount} • 🎯 Match: ${stats.matchPercentage}% (tap)",
            "🔥 Oboje BYŁO: ${stats.bothByloCount} • 🎯 Zgodność: ${stats.matchPercentage}%"
        )
        binding.tvMatchPercentage.text = AppLocale.t(
            "🎯 Збіг відповідей із друзями: ${stats.matchPercentage}%",
            "🎯 Match rate with friends: ${stats.matchPercentage}%",
            "🎯 Zgodność odpowiedzi z przyjacielem: ${stats.matchPercentage}%"
        )
        binding.tvBothByloCount.text = AppLocale.t(
            "🔥 В обох: БУЛО! — ${stats.bothByloCount} спільні шалені історії",
            "🔥 Both: I HAVE! — ${stats.bothByloCount} shared crazy stories",
            "🔥 Oboje: BYŁO! — ${stats.bothByloCount} wspólne szalone historie"
        )
        binding.tvBothNeverCount.text = AppLocale.t(
            "😇 В обох: НІКОЛИ — ${stats.bothNeverCount} питань",
            "😇 Both: NEVER — ${stats.bothNeverCount} questions",
            "😇 Oboje: NIGDY — ${stats.bothNeverCount} pytań"
        )
        binding.tvDiffAnswersCount.text = AppLocale.t(
            "⚡ Різні відповіді — ${stats.diffCount} питань",
            "⚡ Different answers — ${stats.diffCount} questions",
            "⚡ Różnice zdań — ${stats.diffCount} pytań"
        )
    }

    private fun showNeverStatsDialog() {
        val stats = BestiesLogicHelper.calculateNeverMatchStats(cachedNeverAnswers)
        val message = AppLocale.t(
            "🎯 Загальний збіг відповідей: ${stats.matchPercentage}%\n\n" +
                "🔥 В обох БУЛО!: ${stats.bothByloCount}\n" +
                "😇 В обох НІКОЛИ: ${stats.bothNeverCount}\n" +
                "⚡ Різні відповіді: ${stats.diffCount}\n\n" +
                "Всього зіграно питань у режимі «Я ніколи не...»: ${stats.totalCount}",
            "🎯 Total match rate: ${stats.matchPercentage}%\n\n" +
                "🔥 Both I HAVE!: ${stats.bothByloCount}\n" +
                "😇 Both NEVER: ${stats.bothNeverCount}\n" +
                "⚡ Different answers: ${stats.diffCount}\n\n" +
                "Total 'Never Have I Ever' questions played: ${stats.totalCount}",
            "🎯 Całkowita zgodność: ${stats.matchPercentage}%\n\n" +
                "🔥 Oboje BYŁO!: ${stats.bothByloCount}\n" +
                "😇 Oboje NIGDY: ${stats.bothNeverCount}\n" +
                "⚡ Różne odpowiedzi: ${stats.diffCount}\n\n" +
                "Łącznie rozegranych pytań: ${stats.totalCount}"
        )
        AlertDialog.Builder(requireContext())
            .setTitle(AppLocale.t("🔥 Статистика «Я ніколи не...»", "🔥 'Never Have I Ever' Stats", "🔥 Statystyki „Nigdy nie...”"))
            .setMessage(message)
            .setPositiveButton(AppLocale.t("Грати зараз 🔥", "Play now 🔥", "Graj teraz 🔥")) { _, _ ->
                openGame("NEVER")
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun applyLocalization() {
        binding.tvGamesBadge.text = AppLocale.t("🎲 +20 XP за гру", "🎲 +20 XP / game", "🎲 +20 XP / gra")
        binding.tvGamesTitle.text = AppLocale.t("Ігри компанії", "Squad Games", "Gry ekipy")
        binding.tvGamesSub.text = AppLocale.t(
            "Оберіть вікторину, щоб відповісти й порівняти результати з друзями.",
            "Choose a game to answer questions and compare results with your squad.",
            "Wybierz grę, aby odpowiedzieć na pytania i porównać wyniki z ekipą."
        )

        binding.btnFilterAll.text = AppLocale.t("✨ Всі (4)", "✨ All (4)", "✨ Wszystkie (4)")
        binding.btnFilterStarted.text = AppLocale.t("▶ Популярні", "▶ Popular", "▶ Popularne")
        binding.btnFilterNew.text = AppLocale.t("🆕 Глибокі", "🆕 Deep", "🆕 Głębokie")

        binding.tvNeverBannerTitle.text = AppLocale.t(
            "Статистика збігів «Я ніколи не...»",
            "Match Stats: 'Never Have I Ever'",
            "Statystyki zgodności „Nigdy nie...”"
        )

        val nNever = BestiesLogicHelper.getNeverQuestions().size
        val nWho = BestiesLogicHelper.getWhoQuestions().size
        val nThisThat = BestiesLogicHelper.getThisOrThatQuestions().size
        val nDeep = BestiesLogicHelper.getDeepTalkQuestions().size

        fun metaCount(count: Int): String = AppLocale.t(
            "$count питань • +20 XP",
            "$count questions • +20 XP",
            "$count pytań • +20 XP"
        )

        binding.tvTopicNeverTitle.text = AppLocale.t("Я ніколи не...", "Never Have I Ever", "Nigdy nie...")
        binding.tvTopicNeverDesc.text = AppLocale.t(
            "Перевірте, скільки божевільних історій було у вашій компанії!",
            "Discover how many crazy stories your friend group shares!",
            "Sprawdźcie, ile szalonych historii łączy Waszą paczkę!"
        )
        binding.tvTopicNeverMeta.text = metaCount(nNever)
        binding.tvTopicNeverStart.text = AppLocale.t("ПОЧАТИ ›", "START ›", "START ›")

        binding.tvTopicWhoTitle.text = AppLocale.t("Хто з нас частіше?", "Who's Most Likely To?", "Kto z nas częściej?")
        binding.tvTopicWhoDesc.text = AppLocale.t(
            "Голосуйте, хто з друзів частіше запізнюється або кидає меми!",
            "Vote on who is more likely to be late or spam memes!",
            "Głosujcie, kto z Was częściej się spóźnia lub wysyła memy!"
        )
        binding.tvTopicWhoMeta.text = metaCount(nWho)
        binding.tvTopicWhoStart.text = AppLocale.t("ПОЧАТИ ›", "START ›", "START ›")

        binding.tvTopicThisThatTitle.text = AppLocale.t("Це чи те? (Дилеми)", "This or That? (Dilemmas)", "To czy tamto? (Dylematy)")
        binding.tvTopicThisThatDesc.text = AppLocale.t(
            "Швидкий вибір між двома варіантами відпочинку та планів!",
            "Quick choice between two hangout and travel options!",
            "Szybki wybór między dwoma wariantami wspólnych planów!"
        )
        binding.tvTopicThisThatMeta.text = metaCount(nThisThat)
        binding.tvTopicThisThatStart.text = AppLocale.t("ПОЧАТИ ›", "START ›", "START ›")

        binding.tvTopicDeepTalkTitle.text = AppLocale.t("Розмови у темряві", "Late-Night Deep Talks", "Rozmowy po zmroku")
        binding.tvTopicDeepTalkDesc.text = AppLocale.t(
            "Щирі питання про спогади, довіру та спільні мрії.",
            "Heartfelt questions about memories, trust, and shared dreams.",
            "Szczere pytania o wspomnienia, zaufanie i wspólne marzenia."
        )
        binding.tvTopicDeepTalkMeta.text = metaCount(nDeep)
        binding.tvTopicDeepTalkStart.text = AppLocale.t("ПОЧАТИ ›", "START ›", "START ›")

        binding.btnBackToGamesHub.text = AppLocale.t("← Назад до списку ігор", "← Back to games", "← Powrót do listy gier")
        binding.btnShuffleQuestion.text = AppLocale.t("🔀 Випадкове", "🔀 Random", "🔀 Losowe")
        binding.btnNextQuestion.text = AppLocale.t("Наступне питання ➡️", "Next question ➡️", "Następne pytanie ➡️")
        binding.tvStatsHeader.text = AppLocale.t(
            "📊 СПІЛЬНА СТАТИСТИКА «Я НІКОЛИ НЕ...»",
            "📊 SHARED STATS: 'NEVER HAVE I EVER'",
            "📊 WSPÓLNE STATYSTYKI GRY „NIGDY NIE...”"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
