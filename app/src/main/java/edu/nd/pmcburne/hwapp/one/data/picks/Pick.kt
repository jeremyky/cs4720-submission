package edu.nd.pmcburne.hwapp.one.data.picks

import com.google.firebase.Timestamp

data class Pick(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val selectedTeam: String = "",
    val opponentTeam: String = "",
    val note: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun docId(gameId: String, userId: String): String = "${gameId}_${userId}"
    }
}
