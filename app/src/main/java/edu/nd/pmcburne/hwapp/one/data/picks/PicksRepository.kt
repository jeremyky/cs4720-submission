package edu.nd.pmcburne.hwapp.one.data.picks

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PicksRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val col = db.collection("picks")

    suspend fun upsertPick(
        gameId: String,
        userId: String,
        userEmail: String,
        selectedTeam: String,
        opponentTeam: String,
        note: String,
        existingCreatedAt: Timestamp?
    ): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val docId = Pick.docId(gameId, userId)
            val pick = Pick(
                id = docId,
                gameId = gameId,
                userId = userId,
                userEmail = userEmail,
                selectedTeam = selectedTeam,
                opponentTeam = opponentTeam,
                note = note,
                createdAt = existingCreatedAt ?: now,
                updatedAt = now
            )
            col.document(docId).set(pick).await()
            Log.d(TAG, "upsertPick OK path=picks/$docId gameId=$gameId team=$selectedTeam")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertPick FAILED for gameId=$gameId userId=$userId", e)
            Result.failure(e)
        }
    }

    suspend fun deletePick(gameId: String, userId: String): Result<Unit> {
        return try {
            col.document(Pick.docId(gameId, userId)).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deletePick FAILED for gameId=$gameId userId=$userId", e)
            Result.failure(e)
        }
    }

    fun observeUserPick(gameId: String, userId: String): Flow<Pick?> = callbackFlow {
        val docRef = col.document(Pick.docId(gameId, userId))
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "observeUserPick error gameId=$gameId userId=$userId", error)
                trySend(null)
                return@addSnapshotListener
            }
            val pick = snapshot?.toObject(Pick::class.java)
            trySend(pick)
        }
        awaitClose { registration.remove() }
    }

    fun observeCommunityPicks(gameId: String): Flow<List<Pick>> = callbackFlow {
        // No orderBy here so we don't need a composite index. Sort client-side.
        val query = col.whereEqualTo("gameId", gameId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "observeCommunityPicks error gameId=$gameId", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val picks = snapshot?.documents
                ?.mapNotNull { it.toObject(Pick::class.java) }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                .orEmpty()
            Log.d(TAG, "observeCommunityPicks gameId=$gameId returned ${picks.size} picks")
            trySend(picks)
        }
        awaitClose { registration.remove() }
    }

    fun observeMyPicks(userId: String): Flow<List<Pick>> = callbackFlow {
        // No orderBy here either; sort client-side.
        val query = col.whereEqualTo("userId", userId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "observeMyPicks error userId=$userId", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val picks = snapshot?.documents
                ?.mapNotNull { it.toObject(Pick::class.java) }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                .orEmpty()
            Log.d(TAG, "observeMyPicks userId=$userId returned ${picks.size} picks")
            trySend(picks)
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "PicksRepository"
    }
}
