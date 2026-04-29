package edu.nd.pmcburne.hwapp.one.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun currentUser(): FirebaseUser? = auth.currentUser

    val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val user = fa.currentUser
            trySend(if (user == null) AuthState.SignedOut else AuthState.SignedIn(user))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("Sign-in returned no user.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyMessage(e), e))
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("Sign-up returned no user.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyMessage(e), e))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found for that email."
        is FirebaseAuthInvalidCredentialsException -> "Wrong password or invalid email."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "Password is too weak (use at least 6 characters)."
        else -> e.message ?: "Authentication failed."
    }
}
