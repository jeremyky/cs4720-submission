package edu.nd.pmcburne.hwapp.one

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ----------------------------
// API MODELS (ESPN)
// ----------------------------

data class EspnStandingsResponse(
    val seasonDisplayName: String? = null,
    val children: List<EspnGroup> = emptyList()
)

data class EspnGroup(
    val name: String? = null,
    val abbreviation: String? = null,
    val standings: EspnStandings? = null
)

data class EspnStandings(
    val entries: List<EspnEntry> = emptyList()
)

data class EspnEntry(
    val team: EspnTeam? = null,
    val stats: List<EspnStat> = emptyList()
)

data class EspnTeam(
    val displayName: String? = null,
    val abbreviation: String? = null,
    val location: String? = null,
    val name: String? = null
)

data class EspnStat(
    val name: String? = null,
    val displayValue: String? = null,
    val value: Double? = null
)

// ----------------------------
// RETROFIT
// ----------------------------

interface EspnStandingsApiService {
    @GET("apis/v2/sports/basketball/nba/standings")
    suspend fun getNbaStandings(
        @Query("level") level: Int = 2,
        @Query("region") region: String = "us",
        @Query("lang") lang: String = "en",
        @Query("contentorigin") contentOrigin: String = "espn"
    ): EspnStandingsResponse
}

object EspnRetrofitClient {
    private const val BASE_URL = "https://site.web.api.espn.com/"

    val service: EspnStandingsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EspnStandingsApiService::class.java)
    }
}

// ----------------------------
// UI MODELS
// ----------------------------

data class TeamStanding(
    val rank: Int,
    val teamName: String,
    val wins: String,
    val losses: String,
    val winPct: String,
    val gamesBack: String
)

data class ConferenceStandings(
    val conferenceName: String,
    val teams: List<TeamStanding>
)

// ----------------------------
// VIEWMODEL
// ----------------------------

class StandingsViewModel : ViewModel() {
    var conferences by mutableStateOf<List<ConferenceStandings>>(emptyList())
    var seasonLabel by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun fetchStandings() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = EspnRetrofitClient.service.getNbaStandings()
                seasonLabel = response.seasonDisplayName
                conferences = response.children.mapNotNull { group ->
                    val groupName = group.name ?: return@mapNotNull null
                    val entries = group.standings?.entries.orEmpty()
                    if (entries.isEmpty()) return@mapNotNull null

                    val ranked = entries
                        .map { entry ->
                            val statsByName = entry.stats.associateBy { it.name }
                            val winsValue = statsByName["wins"]?.value ?: 0.0
                            val pctValue = statsByName["winPercent"]?.value ?: 0.0
                            Triple(entry, winsValue, pctValue) to statsByName
                        }
                        .sortedWith(
                            compareByDescending<Pair<Triple<EspnEntry, Double, Double>, Map<String?, EspnStat>>> { it.first.third }
                                .thenByDescending { it.first.second }
                        )

                    val teams = ranked.mapIndexed { index, (triple, statsByName) ->
                        val entry = triple.first
                        TeamStanding(
                            rank = index + 1,
                            teamName = entry.team?.displayName
                                ?: entry.team?.name
                                ?: "Unknown",
                            wins = statsByName["wins"]?.displayValue ?: "—",
                            losses = statsByName["losses"]?.displayValue ?: "—",
                            winPct = statsByName["winPercent"]?.displayValue ?: "—",
                            gamesBack = statsByName["gamesBehind"]?.displayValue ?: "—"
                        )
                    }
                    ConferenceStandings(groupName, teams)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load standings: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

// ----------------------------
// UI
// ----------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(onBack: () -> Unit) {
    val viewModel: StandingsViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (viewModel.conferences.isEmpty() && viewModel.errorMessage == null) {
            viewModel.fetchStandings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchStandings() },
                        enabled = !viewModel.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                viewModel.isLoading && viewModel.conferences.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                viewModel.errorMessage != null && viewModel.conferences.isEmpty() -> {
                    ElevatedCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Could not load standings",
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
                            FilledTonalButton(onClick = { viewModel.fetchStandings() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                viewModel.conferences.isEmpty() -> {
                    ElevatedCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "No standings available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ESPN returned no standings data. Try Refresh.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            StandingsHeader(seasonLabel = viewModel.seasonLabel)
                        }
                        viewModel.conferences.forEach { conf ->
                            item {
                                Text(
                                    text = conf.conferenceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }
                            item {
                                ConferenceCard(conference = conf)
                            }
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
private fun StandingsHeader(seasonLabel: String?) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = seasonLabel ?: "NBA Standings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Source: ESPN public standings API",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConferenceCard(conference: ConferenceStandings) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            StandingsTableHeader()
            HorizontalDivider()
            conference.teams.forEach { team ->
                StandingRow(team = team)
            }
        }
    }
}

@Composable
private fun StandingsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Team",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "W",
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "L",
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "PCT",
            modifier = Modifier.width(50.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "GB",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StandingRow(team: TeamStanding) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = team.rank.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = team.teamName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = team.wins,
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = team.losses,
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = team.winPct,
            modifier = Modifier.width(50.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = team.gamesBack,
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
