package com.ascend.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.BuildConfig
import com.ascend.core.data.community.GoogleIdTokenRequester
import com.ascend.core.data.community.GoogleIdTokenResult
import kotlinx.coroutines.launch

/*
 * The additive, opt-in Community sign-in surface. Signing in links a remote identity for future
 * Community features; it never affects the single-player loop. Google is the primary path (shown when
 * configured); email/password is always available. Self-contained styling, no prototype dependency.
 */
private val BG = Color(0xFF04050B)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)
private val ACCENT = Color(0xFF3ECF8E)
private val BAD = Color(0xFFE5736B)

@Composable
fun AuthScreen(
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleRequester = remember { GoogleIdTokenRequester() }

    AuthContent(
        state = state,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp,
        onGoogle = {
            // Acquire the Google ID token at the UI layer (needs the Activity), then exchange it in the
            // ViewModel. The client secret is never involved.
            scope.launch {
                when (val result = googleRequester.request(context, BuildConfig.GOOGLE_WEB_CLIENT_ID)) {
                    is GoogleIdTokenResult.Success -> viewModel.submitGoogleIdToken(result.idToken)
                    is GoogleIdTokenResult.Failure -> viewModel.showError(result.message)
                    GoogleIdTokenResult.Unavailable -> viewModel.showError("Google sign-in isn't configured.")
                    GoogleIdTokenResult.Cancelled -> Unit
                }
            }
        },
        onSignOut = viewModel::signOut,
        onBack = onBack,
    )
}

@Composable
fun AuthContent(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit = { _, _ -> },
    onSignUp: (String, String) -> Unit = { _, _ -> },
    onGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(BG)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp)) {
            TextButton(onClick = onBack) { Text("‹ Back", color = MUTED, fontSize = 14.sp) }
            Spacer(Modifier.height(8.dp))
            Text("COMMUNITY", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.signedIn) "You're signed in" else "Sign in to Ascend Community",
                color = INK,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Optional — your solo game works fully without an account.",
                color = MUTED,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(22.dp))

            if (state.signedIn) {
                SignedIn(state, onSignOut)
            } else {
                SignedOut(state, onSignIn, onSignUp, onGoogle)
            }

            state.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = BAD, fontSize = 13.sp)
            }
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ACCENT)
            }
        }
    }
}

@Composable
private fun SignedIn(
    state: AuthUiState,
    onSignOut: () -> Unit,
) {
    Text(
        if (state.emailVerified) "Your email is verified." else "Check your inbox to verify your email.",
        color = MUTED,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(18.dp))
    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
        Text("Sign out", color = INK)
    }
}

@Composable
private fun SignedOut(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogle: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (state.googleAvailable) {
        Button(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Google", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        Text("or use email", color = MUTED, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(18.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { onSignIn(email, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in")
        }
        OutlinedButton(onClick = { onSignUp(email, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Create account", color = INK)
        }
    }
}
