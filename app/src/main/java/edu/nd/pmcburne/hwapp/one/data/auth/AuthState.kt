package edu.nd.pmcburne.hwapp.one.data.auth

import com.google.firebase.auth.FirebaseUser

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val user: FirebaseUser) : AuthState
}
