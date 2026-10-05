package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.Registration
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the signed-in user, or null when signed out. */
    fun observeCurrentUser(): Flow<AppUser?>

    suspend fun signIn(email: String, password: String): AuthResult

    /** Signs in to Firebase with the ID token from Sign in with Google. */
    suspend fun signInWithGoogle(idToken: String): AuthResult

    suspend fun register(registration: Registration): AuthResult

    suspend fun signOut()
}
