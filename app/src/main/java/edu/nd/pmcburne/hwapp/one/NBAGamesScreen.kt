package edu.nd.pmcburne.hwapp.one

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.nd.pmcburne.hwapp.one.data.OddsFormat
import edu.nd.pmcburne.hwapp.one.data.Sportsbooks
import edu.nd.pmcburne.hwapp.one.data.UserPreferences
import edu.nd.pmcburne.hwapp.one.data.UserPreferencesRepository
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ----------------------------
// API MODELS
// ----------------------------

data class OddsGame(
    val id: String,
    val home_team: String,
    val away_team: String,
    val commence_time: String,
    val bookmakers: List<Bookmaker> = emptyList()
)

data class Bookmaker(
    val key: String,
    val title: String,
    val last_update: String? = null,
    val markets: List<Market> = emptyList()
)

data class Market(
    val key: String,
    val last_update: String? = null,
    val outcomes: List<Outcome> = emptyList()
)

data class Outcome(
    val name: String,
    val price: Double? = null,
    val point: Double? = null
)

data class ScoreGame(
    val id: String,
    val home_team: String,
    val away_team: String,
    val commence_time: String,
    val completed: Boolean,
    val scores: List<TeamScore>? = null,
    val last_update: String? = null
)

data class TeamScore(
    val name: String,
    val score: String
)

// ----------------------------
// UI MODELS
// ----------------------------

data class TeamOdds(
    val name: String,
    val moneyline: String
)

data class NBAGame(
    val id: String,
    val awayTeam: TeamOdds,
    val homeTeam: TeamOdds,
    val awayScore: String? = null,
    val homeScore: String? = null,
    val status: String,
    val time: String,
    val sportsbook: String,
    val lastUpdate: String? = null
)

// ----------------------------
// RETROFIT
// ----------------------------

interface OddsApiService {
    @GET("v4/sports/basketball_nba/odds")
    suspend fun getNbaOdds(
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "us",
        @Query("markets") markets: String = "h2h",
        @Query("oddsFormat") oddsFormat: String = "american"
    ): List<OddsGame>

    @GET("v4/sports/basketball_nba/scores")
    suspend fun getNbaScores(
        @Query("apiKey") apiKey: String
    ): List<ScoreGame>
}

object RetrofitClient {
    private const val BASE_URL = "https://api.the-odds-api.com/"

    val apiService: OddsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OddsApiService::class.java)
    }
}

// ----------------------------
// VIEWMODEL
// ----------------------------

class NBAViewModel(
    private val preferencesRepo: UserPreferencesRepository? = null
) : ViewModel() {
    var games by mutableStateOf<List<NBAGame>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showOnlyFavorites by mutableStateOf(false)
        private set
    var favoriteTeams by mutableStateOf<Set<String>>(emptySet())
        private set

    val displayedGames: List<NBAGame>
        get() = if (showOnlyFavorites) {
            games.filter { it.awayTeam.name in favoriteTeams || it.homeTeam.name in favoriteTeams }
        } else {
            games
        }

    private val apiKey = BuildConfig.ODDS_API_KEY

    init {
        if (preferencesRepo != null) {
            viewModelScope.launch {
                var lastFetchInputs: Pair<String, OddsFormat>? = null
                preferencesRepo.preferencesFlow.collect { prefs ->
                    showOnlyFavorites = prefs.showOnlyFavoriteTeams
                    favoriteTeams = prefs.favoriteTeams

                    val fetchInputs = prefs.preferredSportsbookKey to prefs.oddsFormat
                    if (games.isNotEmpty() && lastFetchInputs != null && lastFetchInputs != fetchInputs) {
                        fetchGames()
                    }
                    lastFetchInputs = fetchInputs
                }
            }
        }
    }

    fun fetchGames() {
        if (apiKey.isBlank()) {
            errorMessage = "Missing ODDS_API_KEY in local.properties"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val prefs: UserPreferences =
                    preferencesRepo?.preferencesFlow?.first() ?: UserPreferences()

                val oddsResponse = RetrofitClient.apiService.getNbaOdds(apiKey)

                val scoresResponse = try {
                    RetrofitClient.apiService.getNbaScores(apiKey)
                } catch (_: Exception) {
                    emptyList()
                }

                val scoresById = scoresResponse.associateBy { it.id }

                games = oddsResponse.map { oddsGame ->
                    val scoreGame = scoresById[oddsGame.id]
                    val bookmaker = pickBookmaker(
                        bookmakers = oddsGame.bookmakers,
                        preferredKey = prefs.preferredSportsbookKey
                    )

                    val h2hMarket = bookmaker?.markets?.find { it.key == "h2h" }

                    val awayMoneyline = formatOdds(
                        h2hMarket?.outcomes?.find { it.name == oddsGame.away_team }?.price,
                        prefs.oddsFormat
                    )
                    val homeMoneyline = formatOdds(
                        h2hMarket?.outcomes?.find { it.name == oddsGame.home_team }?.price,
                        prefs.oddsFormat
                    )

                    val awayScore = scoreGame?.scores
                        ?.find { it.name == oddsGame.away_team }
                        ?.score

                    val homeScore = scoreGame?.scores
                        ?.find { it.name == oddsGame.home_team }
                        ?.score

                    NBAGame(
                        id = oddsGame.id,
                        awayTeam = TeamOdds(oddsGame.away_team, awayMoneyline),
                        homeTeam = TeamOdds(oddsGame.home_team, homeMoneyline),
                        awayScore = awayScore,
                        homeScore = homeScore,
                        status = gameStatus(scoreGame),
                        time = oddsGame.commence_time,
                        sportsbook = bookmaker?.title ?: "No book available",
                        lastUpdate = bookmaker?.last_update ?: scoreGame?.last_update
                    )
                }.sortedWith(compareBy<NBAGame>({ statusRank(it.status) }, { it.time }))

            } catch (e: Exception) {
                errorMessage = "Failed to load NBA games/odds: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun pickBookmaker(
        bookmakers: List<Bookmaker>,
        preferredKey: String
    ): Bookmaker? {
        bookmakers.find { it.key == preferredKey }?.let { return it }
        val fallbackOrder = Sportsbooks.ALL.map { it.key }
        for (key in fallbackOrder) {
            val found = bookmakers.find { it.key == key }
            if (found != null) return found
        }
        return bookmakers.firstOrNull()
    }

    private fun gameStatus(scoreGame: ScoreGame?): String {
        return when {
            scoreGame == null -> "UPCOMING"
            scoreGame.completed -> "FINAL"
            scoreGame.scores != null -> "LIVE"
            else -> "UPCOMING"
        }
    }

    private fun statusRank(status: String): Int {
        return when (status) {
            "LIVE" -> 0
            "UPCOMING" -> 1
            "FINAL" -> 2
            else -> 3
        }
    }
}

internal fun formatOdds(price: Double?, format: OddsFormat): String {
    if (price == null) return "N/A"
    return when (format) {
        OddsFormat.AMERICAN -> {
            val value = price.toInt()
            if (value > 0) "+$value" else value.toString()
        }
        OddsFormat.DECIMAL -> {
            val decimal = if (price >= 100.0) {
                1.0 + price / 100.0
            } else if (price <= -100.0) {
                1.0 + 100.0 / -price
            } else {
                1.0
            }
            String.format(Locale.US, "%.2f", decimal)
        }
    }
}

class NBAViewModelFactory(
    private val preferencesRepo: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NBAViewModel(preferencesRepo) as T
    }
}

// ----------------------------
// UI
// ----------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBAGamesScreen(
    viewModel: NBAViewModel = viewModel(),
    onGameClick: (NBAGame) -> Unit = {},
    onTeamClick: (String) -> Unit = {},
    onOpenStandings: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        viewModel.fetchGames()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NBA Markets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live games and moneylines",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStandings) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Standings"
                        )
                    }
                    IconButton(onClick = onOpenFavorites) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorites"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.fetchGames() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                viewModel.isLoading && viewModel.games.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                viewModel.errorMessage != null && viewModel.games.isEmpty() -> {
                    ElevatedCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Could not load games",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(onClick = { viewModel.fetchGames() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                viewModel.showOnlyFavorites && viewModel.displayedGames.isEmpty() -> {
                    ElevatedCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "No favorite games",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (viewModel.favoriteTeams.isEmpty()) {
                                    "You haven't added any favorite teams yet. Tap a team name on a game card or use the star icon to manage favorites, then come back here."
                                } else {
                                    "None of today's games include your favorite teams. Turn off \"Show only favorite teams\" in Settings to see all games."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    val visibleGames = viewModel.displayedGames
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            SummaryHeader(gameCount = visibleGames.size)
                        }

                        items(visibleGames) { game ->
                            GameCard(
                                game = game,
                                onGameClick = { onGameClick(game) },
                                onTeamClick = onTeamClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHeader(gameCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today’s board",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$gameCount games available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = gameCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GameCard(
    game: NBAGame,
    onGameClick: () -> Unit = {},
    onTeamClick: (String) -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGameClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatGameTime(game.time),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.sportsbook,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = game.status)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            TeamMarketRow(
                label = "AWAY",
                teamName = game.awayTeam.name,
                score = game.awayScore,
                moneyline = game.awayTeam.moneyline,
                onTeamClick = onTeamClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            TeamMarketRow(
                label = "HOME",
                teamName = game.homeTeam.name,
                score = game.homeScore,
                moneyline = game.homeTeam.moneyline,
                onTeamClick = onTeamClick
            )

            if (game.lastUpdate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Updated ${formatGameTime(game.lastUpdate)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TeamMarketRow(
    label: String,
    teamName: String,
    score: String?,
    moneyline: String,
    onTeamClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onTeamClick(teamName) }
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ScorePill(score = score)
        Spacer(modifier = Modifier.width(10.dp))
        OddsPill(text = moneyline)
    }
}

@Composable
fun ScorePill(score: String?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score ?: "--",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun OddsPill(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier
                .width(78.dp)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val background = when (status) {
        "LIVE" -> MaterialTheme.colorScheme.errorContainer
        "FINAL" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val content = when (status) {
        "LIVE" -> MaterialTheme.colorScheme.onErrorContainer
        "FINAL" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == "LIVE") {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(content)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = status,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ----------------------------
// HELPERS
// ----------------------------

fun formatGameTime(raw: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        input.timeZone = TimeZone.getTimeZone("UTC")

        val output = SimpleDateFormat("EEE h:mm a", Locale.US)
        output.timeZone = TimeZone.getDefault()

        output.format(input.parse(raw)!!)
    } catch (_: Exception) {
        raw
    }
}

@Preview(showBackground = true)
@Composable
fun GameCardPreview() {
    HWStarterRepoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryHeader(gameCount = 8)

            GameCard(
                game = NBAGame(
                    id = "1",
                    awayTeam = TeamOdds("Los Angeles Lakers", "+120"),
                    homeTeam = TeamOdds("Boston Celtics", "-140"),
                    awayScore = "98",
                    homeScore = "104",
                    status = "LIVE",
                    time = "2026-04-17T23:30:00Z",
                    sportsbook = "DraftKings",
                    lastUpdate = "2026-04-17T23:45:00Z"
                )
            )

            GameCard(
                game = NBAGame(
                    id = "2",
                    awayTeam = TeamOdds("Phoenix Suns", "+155"),
                    homeTeam = TeamOdds("Denver Nuggets", "-180"),
                    awayScore = null,
                    homeScore = null,
                    status = "UPCOMING",
                    time = "2026-04-18T01:00:00Z",
                    sportsbook = "FanDuel",
                    lastUpdate = "2026-04-17T22:50:00Z"
                )
            )
        }
    }
}