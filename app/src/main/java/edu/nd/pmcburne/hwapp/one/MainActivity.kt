package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.nd.pmcburne.hwapp.one.data.UserPreferencesRepository
import edu.nd.pmcburne.hwapp.one.data.auth.AuthRepository
import edu.nd.pmcburne.hwapp.one.data.picks.PicksRepository
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val preferencesRepo = remember { UserPreferencesRepository(context.applicationContext) }
    val authRepo = remember { AuthRepository() }
    val picksRepo = remember { PicksRepository() }
    val navController = rememberNavController()

    val startDestination = if (authRepo.currentUser() != null) "nba_games" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                authRepo = authRepo,
                onLoginSuccess = {
                    navController.navigate("nba_games") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToCreateAccount = {
                    navController.navigate("create_account")
                }
            )
        }
        composable("create_account") {
            CreateAccountScreen(
                authRepo = authRepo,
                onAccountCreated = {
                    navController.navigate("nba_games") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("nba_games") { backStackEntry ->
            val viewModel: NBAViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = NBAViewModelFactory(preferencesRepo)
            )
            NBAGamesScreen(
                viewModel = viewModel,
                onGameClick = { game ->
                    navController.navigate("game_details/${game.id}")
                },
                onTeamClick = { teamName ->
                    navController.navigate("team/${teamName}")
                },
                onOpenStandings = { navController.navigate("standings") },
                onOpenFavorites = { navController.navigate("favorites") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            route = "game_details/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("nba_games")
            }
            val parentViewModel: NBAViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = NBAViewModelFactory(preferencesRepo)
            )
            val game = parentViewModel.games.find { it.id == gameId }
            GameDetailsScreen(
                gameId = gameId,
                game = game,
                authRepo = authRepo,
                picksRepo = picksRepo,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "team/{teamName}",
            arguments = listOf(navArgument("teamName") { type = NavType.StringType })
        ) { backStackEntry ->
            val teamName = backStackEntry.arguments?.getString("teamName").orEmpty()
            TeamPageScreen(
                teamName = teamName,
                preferencesRepo = preferencesRepo,
                onBack = { navController.popBackStack() }
            )
        }
        composable("standings") {
            StandingsScreen(onBack = { navController.popBackStack() })
        }
        composable("favorites") {
            FavoritesScreen(
                preferencesRepo = preferencesRepo,
                authRepo = authRepo,
                picksRepo = picksRepo,
                onBack = { navController.popBackStack() },
                onPickClick = { pick ->
                    navController.navigate("game_details/${pick.gameId}")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                preferencesRepo = preferencesRepo,
                authRepo = authRepo,
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
