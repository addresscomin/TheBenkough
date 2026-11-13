package com.thebenkough.app.feature.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thebenkough.app.data.remote.SupabaseProvider
import com.thebenkough.app.data.remote.PushRegistration
import io.github.jan.supabase.functions.functions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val NInk = Color(0xFF050608)
private val NPanel = Color(0xCC111922)
private val NText = Color(0xFFF4F7FB)
private val NMuted = Color(0xFF929EAD)
private val NAccent = Color(0xFF77E6D2)
private const val CHANNEL_ID = "benkough_updates"

@Serializable
private data class InboxItem(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String = "",
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Composable
fun NotificationCenterScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<InboxItem>>(emptyList()) }
    var message by remember { mutableStateOf("YOUR NOTIFICATIONS") }
    var loading by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (it) message = "DEVICE NOTIFICATIONS ENABLED"
    }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                    ?: error("Sign in to view notifications.")
                val loaded = SupabaseProvider.client.postgrest["notifications"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<InboxItem>()
                    .sortedByDescending { it.createdAt }
                items = loaded
                message = "${loaded.count { it.readAt == null }} UNREAD · ${loaded.size} TOTAL"
                if (permissionGranted) postNewLocalAlerts(context, loaded)
            } catch (error: Exception) {
                message = "INBOX LOAD FAILED · ${error.message?.take(70) ?: "CHECK CONNECTION"}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().background(NInk).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("THE BENKOUGH®  /  NOTIFICATION CENTRE", color = NMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text("STAY IN THE LOOP.", color = NText, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
        Text("Your private account inbox and device alert settings.", color = NMuted, fontSize = 13.sp)
        Text(message, color = NAccent, fontSize = 9.sp, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { refresh() }, enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = NAccent, contentColor = NInk)) {
                Text(if (loading) "LOADING…" else "REFRESH INBOX", fontWeight = FontWeight.Bold)
            }
            if (!permissionGranted && Build.VERSION.SDK_INT >= 33) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    colors = ButtonDefaults.buttonColors(containerColor = NPanel, contentColor = NAccent)) {
                    Text("ENABLE ALERTS", fontWeight = FontWeight.Bold)
                }
            }
        }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            SupabaseProvider.client.functions.invoke(
                                function = "send-user-notification",
                                body = JsonObject(mapOf(
                                    "title" to JsonPrimitive("THE BENKOUGH"),
                                    "body" to JsonPrimitive("Your test push notification is connected.")
                                ))
                            )
                            message = "PUSH REQUEST SENT · CHECK YOUR DEVICE"
                            refresh()
                        } catch (error: Exception) {
                            message = "PUSH REQUEST FAILED · ${error.message?.take(65) ?: "CHECK SERVER CONFIG"}"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && PushRegistration.isConfigured(),
                colors = ButtonDefaults.buttonColors(containerColor = NPanel, contentColor = NAccent)
            ) {
                Text("SEND TEST PUSH", fontWeight = FontWeight.Bold)
            }
            if (!PushRegistration.isConfigured()) {
                Text("REMOTE PUSH NEEDS FIREBASE APP CONFIG. SERVER-SIDE DELIVERY ALSO NEEDS THE FCM SERVICE ACCOUNT SECRET IN SUPABASE.", color = NMuted, fontSize = 8.sp, lineHeight = 13.sp)
            }
        if (items.isEmpty()) {
            Text("No notifications yet.", color = NMuted, modifier = Modifier.padding(top = 18.dp))
        } else {
            items.forEach { item ->
                Column(
                    modifier = Modifier.fillMaxWidth().background(NPanel, RoundedCornerShape(18.dp))
                        .clickable {
                            if (item.readAt == null) {
                                scope.launch {
                                    try {
                                        SupabaseProvider.client.postgrest["notifications"].update({
                                            set("read_at", java.time.Instant.now().toString())
                                        }) { filter { eq("id", item.id); eq("user_id", item.userId) } }
                                        items = items.map { if (it.id == item.id) it.copy(readAt = java.time.Instant.now().toString()) else it }
                                        message = "NOTIFICATION MARKED AS READ"
                                    } catch (error: Exception) {
                                        message = "UPDATE FAILED · ${error.message?.take(60) ?: "TRY AGAIN"}"
                                    }
                                }
                            }
                        }
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.title, color = NText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (item.readAt == null) "NEW" else "READ", color = NAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    if (item.body.isNotBlank()) Text(item.body, color = NMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    Text(item.createdAt, color = NMuted.copy(alpha = .65f), fontSize = 8.sp)
                }
            }
        }
        Text(
            "IN-APP INBOX + LOCAL ALERTS ARE ENABLED. REMOTE PUSH USES FIREBASE CLOUD MESSAGING AND THE SECURE SUPABASE EDGE FUNCTION.",
            color = NMuted.copy(alpha = .7f), fontSize = 8.sp, lineHeight = 13.sp
        )
    }
}

private fun postNewLocalAlerts(context: Context, items: List<InboxItem>) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "THE BENKOUGH updates", NotificationManager.IMPORTANCE_DEFAULT))
    }
    val prefs = context.getSharedPreferences("benkough_notifications", Context.MODE_PRIVATE)
    items.filter { it.readAt == null }.take(20).forEach { item ->
        if (!prefs.getBoolean(item.id, false)) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
                prefs.edit().putBoolean(item.id, true).apply()
            } catch (_: SecurityException) {
            }
        }
    }
}
