package com.thebenkough.app.feature.invoice

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

private val IInk = Color(0xFF17101E)
private val IPanel = Color(0xFF211329)
private val IText = Color(0xFFF2E9DE)
private val IMuted = Color(0xFFB7A7C9)
private val IAccent = Color(0xFFDDF27A)

@Serializable
private data class InvoiceRow(
    val id: String,
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("client_name") val clientName: String = "",
    @SerialName("subtotal_minor") val subtotalMinor: Long,
    @SerialName("tax_minor") val taxMinor: Long,
    @SerialName("total_minor") val totalMinor: Long,
    val status: String,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class InvoiceLine(
    val id: String,
    @SerialName("invoice_id") val invoiceId: String,
    val description: String,
    val quantity: Double,
    @SerialName("unit_price_minor") val unitPriceMinor: Long,
    @SerialName("line_total_minor") val lineTotalMinor: Long
)

@Serializable
private data class InvoiceWorkspace(val id: String)

@Composable
fun InvoiceManagerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var invoices by remember { mutableStateOf<List<InvoiceRow>>(emptyList()) }
    var client by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("0") }
    var status by remember { mutableStateOf("LOADING INVOICES…") }
    var busy by remember { mutableStateOf(false) }
    var pdfPayload by remember { mutableStateOf<Pair<InvoiceRow, List<InvoiceLine>>?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val selected = pdfPayload
        if (uri != null && selected != null) {
            status = try {
                writeInvoicePdf(context, uri, selected.first, selected.second)
                "INVOICE PDF EXPORTED"
            } catch (error: Exception) {
                "PDF FAILED · ${error.message?.take(55) ?: "TRY AGAIN"}"
            }
        }
        pdfPayload = null
    }

    suspend fun loadInvoices() {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("Sign in to access invoices.")
        val workspace = SupabaseProvider.client.postgrest["workspaces"]
            .select { filter { eq("owner_id", userId) } }
            .decodeList<InvoiceWorkspace>()
            .firstOrNull() ?: error("Personal workspace not found.")
        invoices = SupabaseProvider.client.postgrest["invoices"]
            .select { filter { eq("workspace_id", workspace.id) } }
            .decodeList<InvoiceRow>()
            .sortedByDescending { it.createdAt }
        status = "${invoices.size} INVOICES · CLOUD ACCOUNT"
    }

    LaunchedEffect(Unit) {
        try { loadInvoices() } catch (error: Exception) {
            status = "LOAD FAILED · ${error.message?.take(65) ?: "CHECK CONNECTION"}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(IInk).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("THE BENKOUGH®  /  BUSINESS HUB", color = IMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text("INVOICE\nSTUDIO.", color = IText, fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
        Text("Create itemized invoices, track payment status and export a PDF.", color = IMuted, fontSize = 12.sp)
        Text("NEW INVOICE", color = IAccent, fontSize = 9.sp, letterSpacing = 1.4.sp)
        OutlinedTextField(value = client, onValueChange = { client = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Client / customer name") }, singleLine = true, colors = invoiceFieldColors())
        OutlinedTextField(value = itemDescription, onValueChange = { itemDescription = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Item / service description") }, singleLine = true, colors = invoiceFieldColors())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { ch -> ch.isDigit() || ch == '.' }.take(8) },
                modifier = Modifier.weight(1f), label = { Text("Qty") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = invoiceFieldColors())
            OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                modifier = Modifier.weight(2f), label = { Text("Unit price ₹") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = invoiceFieldColors())
        }
        OutlinedTextField(value = taxPercent, onValueChange = { taxPercent = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Tax % (optional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = invoiceFieldColors())

        Button(
            onClick = {
                val qty = quantity.toDoubleOrNull()
                val price = unitPrice.toDoubleOrNull()
                val tax = taxPercent.toDoubleOrNull() ?: 0.0
                if (itemDescription.isBlank() || client.isBlank() || qty == null || qty <= 0 || price == null || price < 0 || tax !in 0.0..100.0) {
                    status = "ENTER VALID CLIENT, ITEM, QTY, PRICE AND TAX"
                } else {
                    scope.launch {
                        busy = true
                        try {
                            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("Sign in first.")
                            val workspace = SupabaseProvider.client.postgrest["workspaces"]
                                .select { filter { eq("owner_id", userId) } }
                                .decodeList<InvoiceWorkspace>()
                                .firstOrNull() ?: error("Personal workspace not found.")
                            val subtotal = (qty * price * 100.0).roundToLong()
                            val taxMinor = (subtotal * tax / 100.0).roundToLong()
                            val total = subtotal + taxMinor
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            val number = "BK-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(4).uppercase()}"
                            val invoiceId = UUID.randomUUID().toString()
                            val invoice = JsonObject(mapOf(
                                "id" to JsonPrimitive(invoiceId),
                                "workspace_id" to JsonPrimitive(workspace.id),
                                "invoice_number" to JsonPrimitive(number),
                                "client_name" to JsonPrimitive(client.trim()),
                                "currency" to JsonPrimitive("INR"),
                                "subtotal_minor" to JsonPrimitive(subtotal),
                                "tax_minor" to JsonPrimitive(taxMinor),
                                "total_minor" to JsonPrimitive(total),
                                "status" to JsonPrimitive("issued"),
                                "issue_date" to JsonPrimitive(today),
                                "created_by" to JsonPrimitive(userId)
                            ))
                            SupabaseProvider.client.postgrest["invoices"].insert(invoice)
                            val line = JsonObject(mapOf(
                                "invoice_id" to JsonPrimitive(invoiceId),
                                "description" to JsonPrimitive(itemDescription.trim()),
                                "quantity" to JsonPrimitive(qty),
                                "unit_price_minor" to JsonPrimitive((price * 100.0).roundToLong()),
                                "line_total_minor" to JsonPrimitive(subtotal)
                            ))
                            SupabaseProvider.client.postgrest["invoice_items"].insert(line)
                            itemDescription = ""
                            unitPrice = ""
                            quantity = "1"
                            taxPercent = "0"
                            loadInvoices()
                            status = "CREATED · $number · ₹${"%.2f".format(Locale.US, total / 100.0)}"
                        } catch (error: Exception) {
                            status = "CREATE FAILED · ${error.message?.take(65) ?: "TRY AGAIN"}"
                        } finally { busy = false }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IAccent, contentColor = IInk)
        ) { Text(if (busy) "SAVING…" else "CREATE INVOICE  ↗", fontWeight = FontWeight.Black) }

        Text(status, color = IAccent, fontSize = 9.sp, letterSpacing = .8.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("INVOICE REGISTER", color = IText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${invoices.size} TOTAL", color = IAccent, fontSize = 9.sp)
        }
        invoices.forEach { invoice ->
            Column(modifier = Modifier.fillMaxWidth().background(IPanel, RoundedCornerShape(4.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(invoice.invoiceNumber, color = IText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(invoice.status.uppercase(), color = IAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text("₹${"%.2f".format(Locale.US, invoice.totalMinor / 100.0)}  ·  ${invoice.issueDate ?: invoice.createdAt.take(10)}",
                    color = IMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val lines = SupabaseProvider.client.postgrest["invoice_items"]
                                    .select { filter { eq("invoice_id", invoice.id) } }
                                    .decodeList<InvoiceLine>()
                                pdfPayload = invoice to lines
                                pdfLauncher.launch("${invoice.invoiceNumber}.pdf")
                            } catch (error: Exception) {
                                status = "PDF LOAD FAILED · ${error.message?.take(55) ?: "TRY AGAIN"}"
                            } finally { busy = false }
                        }
                    }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3933B8), contentColor = Color.White)) {
                        Text("EXPORT PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    if (invoice.status != "paid" && invoice.status != "cancelled") {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    SupabaseProvider.client.postgrest["invoices"].update({
                                        set("status", "paid")
                                    }) { filter { eq("id", invoice.id) } }
                                    loadInvoices()
                                } catch (error: Exception) {
                                    status = "STATUS UPDATE FAILED · ${error.message?.take(50) ?: "TRY AGAIN"}"
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = IAccent, contentColor = IInk)) {
                            Text("MARK PAID", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = {
                        scope.launch {
                            try {
                                SupabaseProvider.client.postgrest["invoices"].delete { filter { eq("id", invoice.id) } }
                                loadInvoices()
                                status = "INVOICE DELETED"
                            } catch (error: Exception) {
                                status = "DELETE FAILED · ${error.message?.take(50) ?: "TRY AGAIN"}"
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF482633), contentColor = Color.White)) {
                        Text("DELETE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun writeInvoicePdf(context: Context, uri: Uri, invoice: InvoiceRow, lines: List<InvoiceLine>) {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 25f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
    canvas.drawText("THE BENKOUGH", 42f, 58f, title)
    canvas.drawText("INVOICE  ${invoice.invoiceNumber}", 42f, 98f, bold)
    canvas.drawText("BILL TO: ${invoice.clientName.take(55)}", 42f, 126f, body)
    canvas.drawText("DATE: ${invoice.issueDate ?: invoice.createdAt.take(10)}", 42f, 148f, body)
    canvas.drawLine(42f, 175f, 552f, 175f, Paint().apply { color = android.graphics.Color.LTGRAY })
    canvas.drawText("DESCRIPTION", 42f, 200f, bold)
    canvas.drawText("QTY", 350f, 200f, bold)
    canvas.drawText("RATE", 405f, 200f, bold)
    canvas.drawText("TOTAL", 490f, 200f, bold)
    var y = 230f
    lines.forEach { line ->
        if (y < 690f) {
            canvas.drawText(line.description.take(42), 42f, y, body)
            canvas.drawText("%.2f".format(Locale.US, line.quantity), 350f, y, body)
            canvas.drawText("%.2f".format(Locale.US, line.unitPriceMinor / 100.0), 405f, y, body)
            canvas.drawText("%.2f".format(Locale.US, line.lineTotalMinor / 100.0), 490f, y, body)
            y += 25f
        }
    }
    y = maxOf(y + 25f, 300f)
    canvas.drawText("SUBTOTAL  ₹${"%.2f".format(Locale.US, invoice.subtotalMinor / 100.0)}", 365f, y, bold)
    canvas.drawText("TAX  ₹${"%.2f".format(Locale.US, invoice.taxMinor / 100.0)}", 365f, y + 25f, body)
    canvas.drawText("TOTAL  ₹${"%.2f".format(Locale.US, invoice.totalMinor / 100.0)}", 365f, y + 55f, title)
    doc.finishPage(page)
    context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } ?: error("Cannot open PDF destination")
    doc.close()
}

@Composable
private fun invoiceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = IText, unfocusedTextColor = IText,
    focusedBorderColor = IAccent, unfocusedBorderColor = IMuted.copy(alpha = .45f),
    focusedLabelColor = IAccent, unfocusedLabelColor = IMuted, cursorColor = IAccent
)
