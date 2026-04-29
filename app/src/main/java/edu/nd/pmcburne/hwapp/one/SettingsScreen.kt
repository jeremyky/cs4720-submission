package edu.nd.pmcburne.hwapp.one

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.nd.pmcburne.hwapp.one.data.OddsFormat
import edu.nd.pmcburne.hwapp.one.data.SportsbookOption
import edu.nd.pmcburne.hwapp.one.data.Sportsbooks
import edu.nd.pmcburne.hwapp.one.data.UserPreferences
import edu.nd.pmcburne.hwapp.one.data.UserPreferencesRepository
import edu.nd.pmcburne.hwapp.one.data.auth.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepo: UserPreferencesRepository,
    authRepo: AuthRepository,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val prefs by preferencesRepo.preferencesFlow.collectAsState(initial = UserPreferences())
    val accountEmail = authRepo.currentUser()?.email

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountSection(
                email = accountEmail,
                onSignOut = {
                    authRepo.signOut()
                    onSignOut()
                }
            )

            SportsbookSection(
                selectedKey = prefs.preferredSportsbookKey,
                onSelect = { option ->
                    scope.launch { preferencesRepo.setPreferredSportsbook(option.key) }
                }
            )

            OddsFormatSection(
                selected = prefs.oddsFormat,
                onSelect = { format ->
                    scope.launch { preferencesRepo.setOddsFormat(format) }
                }
            )

            ShowOnlyFavoritesSection(
                enabled = prefs.showOnlyFavoriteTeams,
                onToggle = { newValue ->
                    scope.launch { preferencesRepo.setShowOnlyFavoriteTeams(newValue) }
                }
            )
        }
    }
}

@Composable
private fun AccountSection(
    email: String?,
    onSignOut: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email ?: "Not signed in",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun SportsbookSection(
    selectedKey: String,
    onSelect: (SportsbookOption) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preferred sportsbook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Used as the first choice when picking which book's moneylines to show.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Sportsbooks.ALL.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option.key == selectedKey,
                            onClick = { onSelect(option) }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option) }
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(text = "  ${option.label}")
                }
            }
        }
    }
}

@Composable
private fun OddsFormatSection(
    selected: OddsFormat,
    onSelect: (OddsFormat) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Odds format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Affects how moneyline odds are displayed throughout the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OddsFormat.entries.forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = format == selected,
                            onClick = { onSelect(format) }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = format == selected,
                        onClick = { onSelect(format) }
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(text = "  ${format.label}")
                }
            }
        }
    }
}

@Composable
private fun ShowOnlyFavoritesSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    Text(
                        text = "Show only favorite teams",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Has no effect yet — will hide non-favorite games once Favorites is implemented.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
