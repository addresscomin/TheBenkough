package com.thebenkough.app.feature.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.ProjectVaultRepository
import com.thebenkough.app.data.remote.VaultAsset
import kotlinx.coroutines.launch

private val VInk = Color(0xFF17101E)
private val VPanel = Color(0xFF211329)
private val VText = Color(0xFFF2E9DE)
private val VMuted = Color(0xFFB7A7C9)
private val VAccent = Color(0xFFDDF27A)

@Composable
fun ProjectVaultScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var assets by remember { mutableStateOf<List<VaultAsset>>(emptyList()) }
    var status by remember { mutableStateOf("PRIVATE CLOUD VAULT") }
    var busy by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<VaultAsset?>(null) }

    val uploadPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "UPLOADING FILE TO PRIVATE VAULT…"
                try {
                    ProjectVaultRepository.upload(context, uri)
                    assets = ProjectVaultRepository.listAssets()
                    status = "FILE UPLOADED · PRIVATE CLOUD STORAGE"
                } catch (error: Exception) {
                    status = "UPLOAD FAILED · ${error.message?.take(72) ?: "CHECK ACCESS"}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val selected = pendingDownload
        if (uri != null && selected != null) {
            scope.launch {
                busy = true
                status = "DOWNLOADING ${selected.originalName}…"
                try {
                    val bytes = ProjectVaultRepository.download(selected.storagePath)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("Could not open the selected save location.")
                    status = "DOWNLOAD COMPLETE · ${bytes.size} BYTES"
                } catch (error: Exception) {
                    status = "DOWNLOAD FAILED · ${error.message?.take(72) ?: "TRY AGAIN"}"
                } finally {
                    busy = false
                    pendingDownload = null
                }
            }
        } else pendingDownload = null
    }

    fun refresh() {
        scope.launch {
            busy = true
            status = "LOADING PRIVATE FILES…"
            try {
                assets = ProjectVaultRepository.listAssets()
                status = "${assets.size} FILES IN YOUR PRIVATE VAULT"
            } catch (error: Exception) {
                status = "VAULT LOAD FAILED · ${error.message?.take(72) ?: "CHECK CONNECTION"}"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().background(VInk).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("THE BENKOUGH®  /  PROJECT VAULT", color = VMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text("YOUR FILES.\nYOUR SPACE.", color = VText, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
        Text("Private Supabase Storage · 25 MB per file · workspace access rules", color = VMuted, fontSize = 12.sp)
        Text(status, color = VAccent, fontSize = 9.sp, letterSpacing = .8.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { uploadPicker.launch(arrayOf("*/*")) }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = VAccent, contentColor = VInk)) {
                Text("UPLOAD FILE  ↗", fontWeight = FontWeight.Black)
            }
            Button(onClick = { refresh() }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = VPanel, contentColor = VAccent)) {
                Text("REFRESH", fontWeight = FontWeight.Bold)
            }
        }
        if (assets.isEmpty()) {
            Text("No files yet. Upload your first project asset.", color = VMuted, modifier = Modifier.padding(top = 16.dp))
        } else {
            assets.forEach { asset ->
                Column(
                    modifier = Modifier.fillMaxWidth().background(VPanel, RoundedCornerShape(4.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(asset.originalName, color = VText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${asset.mimeType}  ·  ${asset.sizeBytes / 1024} KB", color = VMuted, fontSize = 10.sp)
                    Button(onClick = {
                        pendingDownload = asset
                        savePicker.launch(asset.originalName)
                    }, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3933B8), contentColor = Color.White)) {
                        Text("DOWNLOAD FILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            "FILES ARE STORED IN THE PRIVATE WORKSPACE-ASSETS BUCKET. DOWNLOADS REQUIRE A VALID SIGNED-IN WORKSPACE MEMBERSHIP.",
            color = VMuted.copy(alpha = .7f), fontSize = 8.sp, lineHeight = 13.sp
        )
    }
}
