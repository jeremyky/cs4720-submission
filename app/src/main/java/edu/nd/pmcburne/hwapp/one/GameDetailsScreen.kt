package edu.nd.pmcburne.hwapp.one

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.nd.pmcburne.hwapp.one.data.auth.AuthRepository
import edu.nd.pmcburne.hwapp.one.data.picks.Pick
import edu.nd.pmcburne.hwapp.one.data.picks.PicksRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    gameId: String,
    game: NBAGame?,
    authRepo: AuthRepository,
    picksRepo: PicksRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = authRepo.currentUser()

    val userPick by remember(gameId, currentUser?.uid) {
        if (currentUser == null) {
            kotlinx.coroutines.flow.flowOf<Pick?>(null)
        } else {
            picksRepo.observeUserPick(gameId, currentUser.uid)
        }
    }.collectAsState(initial = null)

    val communityPicks by remember(gameId) {
        picksRepo.observeCommunityPicks(gameId)
    }.collectAsState(initial = emptyList())

    var showPickSheet by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareGame(context, gameId, game) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (game != null) {
                            Text(
                                text = "${game.awayTeam.name} @ ${game.homeTeam.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Status: ${game.status}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tipoff: ${formatGameTime(game.time)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sportsbook: ${game.sportsbook}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TeamLine(
                                label = "AWAY",
                                teamName = game.awayTeam.name,
                                score = game.awayScore,
                                moneyline = game.awayTeam.moneyline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TeamLine(
                                label = "HOME",
                                teamName = game.homeTeam.name,
                                score = game.homeScore,
                                moneyline = game.homeTeam.moneyline
                            )
                        } else {
                            Text(
                                text = "Game ID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gameId,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Game data isn't loaded right now. Go back and refresh the home screen, then try again.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (game != null) {
                item {
                    MyPickSection(
                        existingPick = userPick,
                        enabled = currentUser != null,
                        onMakeOrEdit = { showPickSheet = true },
                        onDelete = {
                            val uid = currentUser?.uid ?: return@MyPickSection
                            scope.launch { picksRepo.deletePick(gameId, uid) }
                        }
                    )
                }

                item {
                    CommunityPicksSection(
                        picks = communityPicks,
                        currentUserId = currentUser?.uid,
                        awayTeam = game.awayTeam.name,
                        homeTeam = game.homeTeam.name
                    )
                }
            }

            item {
                Button(
                    onClick = { shareGame(context, gameId, game) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share game", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPickSheet && game != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showPickSheet = false },
            confirmButton = {},
            text = {
                PickSheetContent(
                    awayTeam = game.awayTeam.name,
                    homeTeam = game.homeTeam.name,
                    existingPick = userPick,
                    saving = saving,
                    errorMessage = saveError,
                    onSave = { selected, opponent, note ->
                        scope.launch {
                            saving = true
                            saveError = null
                            val result = picksRepo.upsertPick(
                                gameId = gameId,
                                userId = currentUser.uid,
                                userEmail = currentUser.email.orEmpty(),
                                selectedTeam = selected,
                                opponentTeam = opponent,
                                note = note,
                                existingCreatedAt = userPick?.createdAt
                            )
                            saving = false
                            result
                                .onSuccess { showPickSheet = false }
                                .onFailure { saveError = it.message ?: "Save failed." }
                        }
                    },
                    onCancel = { showPickSheet = false }
                )
            }
        )
    }

    LaunchedEffect(showPickSheet) {
        if (!showPickSheet) saveError = null
    }
}

@Composable
private fun CommunityPicksSection(
    picks: List<Pick>,
    currentUserId: String?,
    awayTeam: String,
    homeTeam: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Community Picks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Predictions from all NBAlytics users for this game.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (picks.isEmpty()) {
                Text(
                    text = "No picks yet — be the first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val awayCount = picks.count { it.selectedTeam == awayTeam }
            val homeCount = picks.count { it.selectedTeam == homeTeam }
            val total = picks.size
            val awayPct = if (total > 0) (awayCount * 100) / total else 0
            val homePct = if (total > 0) (homeCount * 100) / total else 0

            Text(
                text = "$awayTeam — $awayCount ($awayPct%)   ·   $homeTeam — $homeCount ($homePct%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            picks.take(10).forEachIndexed { index, p ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                CommunityPickRow(p, isCurrentUser = p.userId == currentUserId)
            }
        }
    }
}

@Composable
private fun CommunityPickRow(pick: Pick, isCurrentUser: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = pick.selectedTeam,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        val emailLabel = pick.userEmail.ifBlank { "anonymous" }
        Text(
            text = if (isCurrentUser) "$emailLabel (You)" else emailLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isCurrentUser) FontWeight.SemiBold else FontWeight.Normal
        )
        if (pick.note.isNotBlank()) {
            Text(
                text = "“${pick.note}”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyPickSection(
    existingPick: Pick?,
    enabled: Boolean,
    onMakeOrEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "My Pick",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!enabled) {
                Text(
                    text = "Sign in to make a pick on this game.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            if (existingPick == null) {
                Text(
                    text = "Tap below to predict the winner. Your pick is visible to other NBAlytics users in the Community Picks section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onMakeOrEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Make a pick")
                }
            } else {
                Text(
                    text = existingPick.selectedTeam,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (existingPick.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${existingPick.note}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onMakeOrEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickSheetContent(
    awayTeam: String,
    homeTeam: String,
    existingPick: Pick?,
    saving: Boolean,
    errorMessage: String?,
    onSave: (selectedTeam: String, opponentTeam: String, note: String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTeam by rememberSaveable { mutableStateOf(existingPick?.selectedTeam ?: "") }
    var note by rememberSaveable { mutableStateOf(existingPick?.note ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (existingPick == null) "Make a pick" else "Update pick",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Who wins?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TeamPickButton(
            team = awayTeam,
            selected = selectedTeam == awayTeam,
            onClick = { selectedTeam = awayTeam }
        )
        TeamPickButton(
            team = homeTeam,
            selected = selectedTeam == homeTeam,
            onClick = { selectedTeam = homeTeam }
        )

        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 200) note = it },
            label = { Text("Optional note (≤ 200 chars)") },
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val opponent = if (selectedTeam == homeTeam) awayTeam else homeTeam
                    onSave(selectedTeam, opponent, note.trim())
                },
                enabled = selectedTeam.isNotBlank() && !saving,
                modifier = Modifier.weight(1f)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (existingPick == null) "Save pick" else "Update")
                }
            }
        }
    }
}

@Composable
private fun TeamPickButton(team: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(team)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(team)
        }
    }
}

@Composable
private fun TeamLine(
    label: String,
    teamName: String,
    score: String?,
    moneyline: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Score: ${score ?: "--"}    Moneyline: $moneyline",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun shareGame(context: Context, gameId: String, game: NBAGame?) {
    val message = buildShareMessage(gameId, game)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "NBAlytics — NBA game")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share game"))
}

internal fun buildShareMessage(gameId: String, game: NBAGame?): String {
    if (game == null) {
        return "Check out this NBA game on NBAlytics (id: $gameId)."
    }

    val matchup = "${game.awayTeam.name} @ ${game.homeTeam.name}"
    val scoreLine = if (game.awayScore != null || game.homeScore != null) {
        "Score: ${game.awayScore ?: "--"} - ${game.homeScore ?: "--"}"
    } else {
        null
    }
    val oddsLine = "Odds: ${game.awayTeam.name} ${game.awayTeam.moneyline}, " +
        "${game.homeTeam.name} ${game.homeTeam.moneyline} (${game.sportsbook})"
    val statusLine = "Status: ${game.status}"

    return buildString {
        appendLine("NBAlytics — $matchup")
        appendLine(statusLine)
        if (scoreLine != null) appendLine(scoreLine)
        appendLine(oddsLine)
        append("(game id: $gameId)")
    }.trim()
}
