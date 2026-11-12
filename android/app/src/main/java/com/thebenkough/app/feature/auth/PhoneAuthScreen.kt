package com.thebenkough.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

private val AuthInk = Color(0xFF050607)
private val AuthPanel = Color(0xFF111820)
private val AuthWhite = Color(0xFFFFFFFF)
private val AuthMuted = Color(0xFFB9C2CC)
private val AuthLime = Color(0xFFB7E36A)

@Composable
fun PhoneAuthScreen(onAuthenticated: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = AuthInk) {
        Column(
            modifier = Modifier.fillMaxSize().background(AuthInk).padding(horizontal = 26.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "THE BENKOUGH®",
                color = AuthWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "YOUR WORKSPACE.\nNO SMS. NO FEE.",
                color = AuthWhite,
                fontSize = 34.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Start with a free secure guest account. No phone number, SMS provider or paid OTP service is needed.",
                color = AuthMuted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            SupabaseProvider.client.auth.signInAnonymously()
                            onAuthenticated()
                        } catch (error: Exception) {
                            message = error.message ?: "Could not create a guest account."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthLime,
                    contentColor = Color(0xFF101318),
                    disabledContainerColor = AuthLime.copy(alpha = .55f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (busy) "SETTING UP YOUR WORKSPACE…" else "CONTINUE FOR FREE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "IMPORTANT: Guest accounts are device-specific. If you sign out, clear app data or change phones, this account cannot be recovered unless you later link a permanent sign-in method.",
                color = AuthMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Start,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.fillMaxWidth().background(AuthPanel, RoundedCornerShape(8.dp)).padding(14.dp)
            )
            message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = Color(0xFFFFB7A8), fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "SUPABASE AUTH  /  FREE GUEST ACCESS",
                color = AuthMuted.copy(alpha = .7f),
                fontSize = 9.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}
