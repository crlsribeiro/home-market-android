package app.carlosribeiro.homemarket.presentation.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import app.carlosribeiro.homemarket.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/** What the Sign in with Google sheet returned. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult

    /** The user closed the sheet: nothing to show. */
    data object Cancelled : GoogleSignInResult

    /** No Google account on the device, or the app is not configured for Google sign-in. */
    data object Unavailable : GoogleSignInResult
}

/**
 * Returns a function that opens the Credential Manager "Sign in with Google" sheet and reports the
 * result. The web client id comes from `google-services.json` (`default_web_client_id`).
 */
@Composable
fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, onResult) {
        { scope.launch { onResult(requestGoogleIdToken(context)) } }
    }
}

private suspend fun requestGoogleIdToken(context: Context): GoogleSignInResult {
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInResult.Unavailable
        }
    } catch (ignored: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (ignored: GetCredentialException) {
        GoogleSignInResult.Unavailable
    } catch (ignored: GoogleIdTokenParsingException) {
        GoogleSignInResult.Unavailable
    }
}
