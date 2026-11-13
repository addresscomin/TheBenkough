package com.thebenkough.app.feature.workspace

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.thebenkough.app.data.local.BenkoughDatabase
import com.thebenkough.app.data.local.WorkspaceRecordEntity
import com.thebenkough.app.data.remote.WorkspaceCloudBackup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val PageInk = Color(0xFF17101E)
private val PagePlum = Color(0xFF211329)
private val PageIvory = Color(0xFFF2E9DE)
private val PageLilac = Color(0xFFB7A7C9)
private val PageLime = Color(0xFFDDF27A)

@Composable
fun WorkspaceRecordsScreen(
    module: String,
    eyebrow: String,
    title: String,
    description: String,
    titleHint: String = "Name or title",
    detailsHint: String = "Notes, details, next steps…"
) {
    val context = LocalContext.current
    val dao = remember(context) { BenkoughDatabase.getInstance(context).workspaceRecordDao() }
    val records by dao.observeModule(module).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var entryTitle by rememberSaveable(module) { mutableStateOf("") }
    var entryDetails by rememberSaveable(module) { mutableStateOf("") }
    var message by rememberSaveable(module) { mutableStateOf("SAVED ON THIS DEVICE") }
    var syncBusy by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable(module) { mutableStateOf<Long?>(null) }
    var editTitle by rememberSaveable(module) { mutableStateOf("") }
    var editDetails by rememberSaveable(module) { mutableStateOf("") }
    var pdfRecord by remember { mutableStateOf<WorkspaceRecordEntity?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val selected = pdfRecord
        if (uri != null && selected != null) {
            message = try {
                writeRecordPdf(context, uri, selected)
                "PDF EXPORTED"
            } catch (error: Exception) {
                "PDF EXPORT FAILED · ${error.message?.take(60) ?: "TRY AGAIN"}"
            }
        }
        pdfRecord = null
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PageInk)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("THE BENKOUGH®  /  WORKSPACE", color = PageLilac, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text(eyebrow.uppercase(), color = PageLime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text(title, color = PageIvory, fontSize = 36.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text(description, color = PageLilac, fontSize = 13.sp, lineHeight = 20.sp)

        Text("CREATE NEW ENTRY", color = PageLime, fontSize = 9.sp, letterSpacing = 1.5.sp)
        OutlinedTextField(value = entryTitle, onValueChange = { entryTitle = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(titleHint) }, colors = fieldColors())
        OutlinedTextField(value = entryDetails, onValueChange = { entryDetails = it },
            modifier = Modifier.fillMaxWidth(), minLines = 3,
            label = { Text(detailsHint) }, colors = fieldColors())
        Button(
            onClick = {
                val cleanTitle = entryTitle.trim()
                if (cleanTitle.isNotEmpty()) {
                    scope.launch {
                        dao.insert(WorkspaceRecordEntity(
                            module = module,
                            title = cleanTitle,
                            details = entryDetails.trim(),
                            cloudKey = UUID.randomUUID().toString()
                        ))
                        entryTitle = ""
                        entryDetails = ""
                        message = "ENTRY SAVED ON THIS DEVICE"
                    }
                } else message = "ADD A TITLE FIRST"
            },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PageLime, contentColor = PageInk)
        ) { Text("SAVE ENTRY  ↗", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }

        Button(
            onClick = {
                scope.launch {
                    syncBusy = true
                    message = "SYNCING LOCAL + CLOUD RECORDS…"
                    try {
                        val merged = WorkspaceCloudBackup.sync(dao)
                        message = "SYNC COMPLETE · $merged CLOUD RECORDS MERGED"
                    } catch (error: Exception) {
                        message = "SYNC FAILED · LOCAL DATA IS SAFE · ${error.message?.take(58) ?: "CHECK CONNECTION"}"
                    } finally {
                        syncBusy = false
                    }
                }
            },
            enabled = !syncBusy,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PagePlum, contentColor = PageLime)
        ) { Text(if (syncBusy) "SYNCING…" else "SYNC NOW · UPLOAD + RESTORE  ↗", fontWeight = FontWeight.Bold, letterSpacing = .7.sp) }
        Text(message, color = PageLilac, fontSize = 9.sp, letterSpacing = 1.2.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("YOUR ENTRIES", color = PageIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${records.size} SAVED", color = PageLime, fontSize = 9.sp, letterSpacing = 1.sp)
        }

        if (records.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().background(PagePlum, RoundedCornerShape(3.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NOTHING HERE YET.", color = PageIvory, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Create your first entry above. It will be stored locally on this device.", color = PageLilac, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            records.forEach { record ->
                Column(modifier = Modifier.fillMaxWidth().background(PagePlum, RoundedCornerShape(3.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingId == record.id) {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, label = { Text("Edit title") }, colors = fieldColors())
                        OutlinedTextField(value = editDetails, onValueChange = { editDetails = it }, modifier = Modifier.fillMaxWidth(),
                            minLines = 3, label = { Text("Edit details") }, colors = fieldColors())
                        Button(onClick = {
                            scope.launch {
                                dao.upsert(record.copy(title = editTitle.trim().ifBlank { record.title },
                                    details = editDetails.trim(), updatedAt = System.currentTimeMillis()))
                                editingId = null
                                message = "ENTRY UPDATED"
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = PageLime, contentColor = PageInk)) {
                            Text("SAVE CHANGES", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text(record.title, color = PageIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (record.details.isNotBlank()) Text(record.details, color = PageLilac, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(record.updatedAt)),
                            color = PageLilac.copy(alpha = .7f), fontSize = 9.sp)
                        Text("LOCAL + CLOUD READY", color = PageLime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    if (module == "INVOICES") {
                        Button(onClick = {
                            pdfRecord = record
                            pdfLauncher.launch("${record.title.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3933B8), contentColor = Color.White)) {
                            Text("EXPORT PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            editingId = record.id
                            editTitle = record.title
                            editDetails = record.details
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3933B8), contentColor = Color.White),
                            shape = RoundedCornerShape(2.dp)) {
                            Text("EDIT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = {
                            scope.launch {
                                dao.softDelete(record.id)
                                message = "ENTRY REMOVED · SYNC TO PROPAGATE DELETE"
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF482633), contentColor = Color(0xFFFFD6D6)),
                            shape = RoundedCornerShape(2.dp)) {
                            Text("DELETE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Text(
            "SYNC NOW MERGES THE NEWER COPY FROM CLOUD AND UPLOADS LOCAL CHANGES. DELETIONS ARE SYNCED AS TOMBSTONES. SYNC REQUIRES INTERNET AND A SIGNED-IN ACCOUNT.",
            color = PageLilac.copy(alpha = .65f), fontSize = 8.sp, lineHeight = 13.sp
        )
    }
}

private fun writeRecordPdf(context: Context, uri: Uri, record: WorkspaceRecordEntity) {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = android.graphics.Color.BLACK }
    canvas.drawText("THE BENKOUGH", 44f, 62f, heading)
    canvas.drawText(if (record.module == "INVOICES") "INVOICE RECORD" else record.module, 44f, 88f, sub)
    canvas.drawLine(44f, 108f, 550f, 108f, Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f })
    canvas.drawText(record.title.take(70), 44f, 145f, heading)
    canvas.drawText("Created: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.updatedAt)), 44f, 172f, sub)
    var y = 215f
    record.details.split("\n").flatMap { line -> line.chunked(78) }.forEach { line ->
        if (y < 790f) {
            canvas.drawText(line, 44f, y, body)
            y += 22f
        }
    }
    document.finishPage(page)
    context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
        ?: error("Could not open selected PDF destination")
    document.close()
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PageIvory, unfocusedTextColor = PageIvory,
    focusedBorderColor = PageLime, unfocusedBorderColor = PageLilac.copy(alpha = .45f),
    focusedLabelColor = PageLime, unfocusedLabelColor = PageLilac, cursorColor = PageLime
)
