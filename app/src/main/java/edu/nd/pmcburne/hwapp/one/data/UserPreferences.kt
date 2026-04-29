package edu.nd.pmcburne.hwapp.one.data

enum class OddsFormat(val label: String) {
    AMERICAN("American"),
    DECIMAL("Decimal")
}

data class SportsbookOption(
    val key: String,
    val label: String
)

object Sportsbooks {
    val DRAFTKINGS = SportsbookOption("draftkings", "DraftKings")
    val FANDUEL = SportsbookOption("fanduel", "FanDuel")
    val BETMGM = SportsbookOption("betmgm", "BetMGM")
    val CAESARS = SportsbookOption("caesars", "Caesars")

    val ALL = listOf(DRAFTKINGS, FANDUEL, BETMGM, CAESARS)

    fun byKey(key: String): SportsbookOption =
        ALL.firstOrNull { it.key == key } ?: DRAFTKINGS
}

data class UserPreferences(
    val preferredSportsbookKey: String = Sportsbooks.DRAFTKINGS.key,
    val oddsFormat: OddsFormat = OddsFormat.AMERICAN,
    val showOnlyFavoriteTeams: Boolean = false,
    val favoriteTeams: Set<String> = emptySet()
)
