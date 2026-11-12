package com.thebenkough.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.launch

private val AuthInk = Color(0xFF050607)
private val AuthPlum = Color(0xFF101318)
private val AuthIvory = Color(0xFFFFFFFF)
private val AuthLilac = Color(0xFFB9C2CC)
private val AuthLime = Color(0xFFB7E36A)
private val AuthFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AuthLime,
    focusedBorderColor = AuthLime,
    unfocusedBorderColor = Color(0xFF65717D),
    focusedLabelColor = AuthLime,
    unfocusedLabelColor = Color(0xFFB9C2CC),
    focusedContainerColor = AuthPlum,
    unfocusedContainerColor = AuthPlum
)
private val AuthInputStyle = TextStyle(
    color = Color.White,
    fontFamily = FontFamily.SansSerif,
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.sp
)

@Composable
fun PhoneAuthScreen(onAuthenticated: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = AuthInk) {
        Column(
            modifier = Modifier.fillMaxSize().background(AuthInk).padding(horizontal = 26.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("THE BENKOUGH®", color = AuthIvory, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(18.dp))
            Text(if (otpSent) "VERIFY YOUR\nNUMBER." else "YOUR WORKSPACE.\nYOUR NUMBER.", color = AuthIvory, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif)
            Spacer(Modifier.height(10.dp))
            Text(if (otpSent) "Enter the 6-digit code sent to +91 $phone." else "Sign in securely with a one-time SMS code. No password required.", color = AuthLilac, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(26.dp))

            if (!otpSent) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10); message = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("10-digit mobile number", fontFamily = FontFamily.SansSerif) },
                    prefix = { Text("+91 ", color = Color.White, fontWeight = FontWeight.Bold) },
                    textStyle = AuthInputStyle,
                    colors = AuthFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6); message = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("6-digit OTP", fontFamily = FontFamily.SansSerif) },
                    textStyle = AuthInputStyle,
                    colors = AuthFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            if (!otpSent) {
                                val fullPhone = "+91$phone"
                                SupabaseProvider.client.auth.signInWith(OTP) { this.phone = fullPhone }
                                otpSent = true
                                message = "OTP request sent. Check your SMS."
                            } else {
                                SupabaseProvider.client.auth.verifyPhoneOtp(
                                    type = OtpType.Phone.SMS,
                                    phone = "+91$phone",
                                    token = otp
                                )
                                onAuthenticated()
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Authentication failed. Please try again."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && if (otpSent) otp.length == 6 else phone.length == 10,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthLime, contentColor = AuthPlum),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    when { busy -> "PLEASE WAIT…"; otpSent -> "VERIFY & CONTINUE"; else -> "SEND OTP" },
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            if (otpSent) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("Wrong number? ", color = AuthLilac, fontSize = 12.sp)
                    androidx.compose.material3.TextButton(onClick = { otpSent = false; otp = ""; message = null }) {
                        Text("Change number", color = AuthLime, fontSize = 12.sp)
                    }
                }
            }

            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = if (it.startsWith("OTP request")) AuthLime else Color(0xFFFFB7A8), fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text("SECURE AUTHENTICATION  /  SUPABASE", color = AuthLilac.copy(alpha = .65f), fontSize = 9.sp, letterSpacing = 1.2.sp)
        }
    }
}
