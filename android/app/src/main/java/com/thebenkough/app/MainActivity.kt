package com.thebenkough.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.thebenkough.app.feature.auth.AccountAuthScreen
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import com.thebenkough.app.feature.script.ScriptRoomScreen
import com.thebenkough.app.feature.workspace.WorkspaceRecordsScreen
import com.thebenkough.app.feature.notifications.NotificationCenterScreen
import com.thebenkough.app.feature.vault.ProjectVaultScreen
import com.thebenkough.app.feature.invoice.InvoiceManagerScreen
import com.thebenkough.app.data.remote.PushRegistration

private val Plum = Color(0xFF211329)
private val Ink = Color(0xFF17101E)
private val Cobalt = Color(0xFF3933B8)
private val Lime = Color(0xFFDDF27A)
private val Ivory = Color(0xFFF2E9DE)
private val Lilac = Color(0xFFB7A7C9)
private val Rose = Color(0xFFC9A6B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BenkoughApp() } }
    }
}

private enum class Destination(val label: String) {
    HOME("HOME"),
    NOTIFICATIONS("INBOX"),
    CREATOR_HUB("CREATE"),
    BUSINESS_HUB("BUSINESS"),
    VAULT_HUB("VAULT"),
    SCRIPT("SCRIPT ROOM"),
    STORYBOARD("STORYBOARD"),
    CAPTIONS("CAPTIONS"),
    HASHTAGS("HASHTAGS"),
    CALENDAR("CALENDAR"),
    CONTENT_LIBRARY("CONTENT LIBRARY"),
    TELEPROMPTER("PROMPT NOTES"),
    CLIENTS("CLIENTS"),
    LEADS("LEADS"),
    PROJECTS("PROJECTS"),
    TASKS("TASKS"),
    INVOICES("INVOICES"),
    ASSETS("ASSETS & REFERENCES")
}

@Composable
private fun BenkoughApp() {
    val appContext = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var authChecked by rememberSaveable { mutableStateOf(false) }
    var authenticated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = try {
            SupabaseProvider.client.auth.currentSessionOrNull() != null
        } catch (_: Throwable) {
            // Keep the app shell alive even if a native SDK fails during startup.
            false
        }
        authChecked = true
    }

    LaunchedEffect(authenticated) {
        if (authenticated) runCatching { PushRegistration.registerCurrentDevice(appContext) }
    }

    if (!authChecked) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("THE BENKOUGH  /  SECURE START", color = Ivory, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    if (!authenticated) {
        AccountAuthScreen(onAuthenticated = { authenticated = true })
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main-destination"
            ) { current ->
                when (current) {
                    Destination.HOME -> HomeScreen(
                        onCreate = { destination = Destination.CREATOR_HUB },
                        onBusiness = { destination = Destination.BUSINESS_HUB },
                        onVault = { destination = Destination.VAULT_HUB },
                        onNotifications = { destination = Destination.NOTIFICATIONS }
                    )
                    Destination.CREATOR_HUB -> HubScreen(
                        eyebrow = "01 / CREATOR STUDIO",
                        title = "MAKE THE IDEA REAL.",
                        description = "Plan, write and organize your content from one workspace.",
                        accent = Cobalt,
                        items = listOf(
                            HubItem("SCRIPT ROOM", "Write and autosave a script", Destination.SCRIPT),
                            HubItem("STORYBOARD", "Break a concept into scenes", Destination.STORYBOARD),
                            HubItem("CAPTIONS", "Draft captions and descriptions", Destination.CAPTIONS),
                            HubItem("HASHTAG BANK", "Organize reusable hashtag sets", Destination.HASHTAGS),
                            HubItem("CONTENT CALENDAR", "Plan publishing dates and notes", Destination.CALENDAR),
                            HubItem("CONTENT LIBRARY", "Keep ideas and published content", Destination.CONTENT_LIBRARY),
                            HubItem("PROMPT NOTES", "Prepare text for a teleprompter", Destination.TELEPROMPTER)
                        ),
                        onOpen = { destination = it }
                    )
                    Destination.BUSINESS_HUB -> HubScreen(
                        eyebrow = "02 / BUSINESS HUB",
                        title = "RUN THE WORK.",
                        description = "Keep client relationships, work and business records organized.",
                        accent = Lime,
                        items = listOf(
                            HubItem("CLIENTS", "Save client details and notes", Destination.CLIENTS),
                            HubItem("LEADS", "Track potential clients and follow-ups", Destination.LEADS),
                            HubItem("PROJECTS", "Record project scope and status", Destination.PROJECTS),
                            HubItem("TASKS", "Keep a list of work to complete", Destination.TASKS),
                            HubItem("INVOICE RECORDS", "Store invoice details and payment notes", Destination.INVOICES)
                        ),
                        onOpen = { destination = it }
                    )
                    Destination.NOTIFICATIONS -> NotificationCenterScreen()
                    Destination.VAULT_HUB -> ProjectVaultScreen()
                    Destination.ASSETS -> ProjectVaultScreen()
                    Destination.SCRIPT -> ScriptRoomScreen()
                    Destination.STORYBOARD -> RecordsPage("STORYBOARD", "SCENES & SEQUENCES", "Build a scene list with shot ideas, order and production notes.", "Scene name", "Visual, dialogue, shot and sequence notes…")
                    Destination.CAPTIONS -> RecordsPage("CAPTIONS", "CAPTION WORKSHOP", "Draft and store captions, descriptions and calls to action.", "Caption title", "Caption text, CTA and platform notes…")
                    Destination.HASHTAGS -> RecordsPage("HASHTAGS", "HASHTAG BANK", "Save hashtag groups by topic, campaign or platform.", "Collection name", "Add hashtags separated by spaces…")
                    Destination.CALENDAR -> RecordsPage("CALENDAR", "CONTENT CALENDAR", "Record planned publishing dates, channels and content notes.", "Post or campaign name", "Date, platform, time and publishing notes…")
                    Destination.CONTENT_LIBRARY -> RecordsPage("CONTENT_LIBRARY", "CONTENT LIBRARY", "Keep content ideas, drafts and published-post references together.", "Content title", "Idea, status, link or production notes…")
                    Destination.TELEPROMPTER -> RecordsPage("TELEPROMPTER_NOTES", "PROMPT NOTES", "Prepare and save speaking scripts. Live scrolling controls are not enabled yet.", "Prompt title", "Paste or write the speaking script…")
                    Destination.CLIENTS -> RecordsPage("CLIENTS", "CLIENT DIRECTORY", "Store client names, contact notes and relationship history.", "Client name", "Contact details and meeting notes…")
                    Destination.LEADS -> RecordsPage("LEADS", "LEAD TRACKER", "Record prospects, follow-up dates and next actions.", "Lead or business name", "Source, contact, follow-up date and next step…")
                    Destination.PROJECTS -> RecordsPage("PROJECTS", "PROJECT REGISTER", "Track project scope, milestones and current status.", "Project name", "Scope, deadline, budget and status…")
                    Destination.TASKS -> RecordsPage("TASKS", "TASK BOARD", "Keep work items, due dates and completion notes.", "Task name", "Due date, priority and task details…")
                    Destination.INVOICES -> InvoiceManagerScreen()
                }
            }
            BottomRail(selected = destination, onSelect = { destination = it })
        }
    }
}

@Composable
private fun RecordsPage(module: String, eyebrow: String, title: String, description: String, detailsHint: String) {
    WorkspaceRecordsScreen(
        module = module,
        eyebrow = eyebrow,
        title = title,
        description = description,
        titleHint = "Name or title",
        detailsHint = detailsHint
    )
}

private data class HubItem(val title: String, val subtitle: String, val destination: Destination)

@Composable
private fun HubScreen(
    eyebrow: String,
    title: String,
    description: String,
    accent: Color,
    items: List<HubItem>,
    onOpen: (Destination) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp).padding(top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("THE BENKOUGH®  /  WORKSPACE", color = Lilac, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text(eyebrow, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text(title, color = Ivory, fontSize = 39.sp, lineHeight = 41.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
        Text(description, color = Lilac, fontSize = 13.sp, lineHeight = 20.sp)
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(Plum).clickable { onOpen(item.destination) }.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("0${index + 1}  /  ${item.title}", color = Ivory, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
                    Text(item.subtitle, color = Lilac, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Text("↗", color = accent, fontSize = 21.sp)
            }
        }
        Text("LOCAL-FIRST WORKSPACE  /  CLOUD CONNECTION PENDING", color = Lilac.copy(alpha = .65f), fontSize = 8.sp, letterSpacing = .8.sp)
    }
}

@Composable
private fun HomeScreen(onCreate: () -> Unit, onBusiness: () -> Unit, onVault: () -> Unit, onNotifications: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("THE BENKOUGH®", color = Ivory, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("CREATIVE BUSINESS SYSTEM", color = Lilac, fontSize = 9.sp, letterSpacing = 2.2.sp)
            }
            Text("INBOX  ↗", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onNotifications))
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("CREATE.", color = Ivory, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
            Text("BUILD.", color = Lime, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
            Text("GROW.", color = Color(0xFF9B91F5), fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
        }
        Text("Your ideas, creative work and business operations — in one deliberate workspace.", color = Ivory.copy(alpha = .76f), fontSize = 15.sp, lineHeight = 23.sp)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Cobalt).clickable(onClick = onCreate).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("01 / CREATOR STUDIO", color = Color.White.copy(alpha = .75f), fontSize = 10.sp, letterSpacing = 2.sp)
                Text("TURN AN IDEA INTO A PLAN.", color = Color.White, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
                Text("OPEN STUDIO  ↗", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile("BUSINESS HUB", "Clients / projects", Lime, Plum, Modifier.weight(1f), onBusiness)
            ActionTile("PROJECT VAULT", "Files / references", Rose, Plum, Modifier.weight(1f), onVault)
        }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("YOUR WORKSPACE", color = Lilac, fontSize = 10.sp, letterSpacing = 2.sp)
            Text("A clean start. No noise.", color = Ivory, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Open a workspace and add your first record.", color = Lilac, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Text("DESIGNED FOR THE WORK THAT MOVES YOU FORWARD.", color = Lilac.copy(alpha = .65f), fontSize = 9.sp, letterSpacing = 1.3.sp)
        AdMobTestBanner()
    }
}


@Composable
private fun AdMobTestBanner() {
    val context = LocalContext.current
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { MobileAds.initialize(context) { initialized = true } }
    if (initialized) {
        val adView = remember(context) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, background: Color, foreground: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(background).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(title, color = foreground, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = foreground.copy(alpha = .75f), fontSize = 10.sp)
        Text("OPEN ↗", color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomRail(selected: Destination, onSelect: (Destination) -> Unit) {
    val roots = listOf(Destination.HOME, Destination.CREATOR_HUB, Destination.BUSINESS_HUB, Destination.VAULT_HUB)
    val selectedRoot = when (selected) {
        Destination.HOME, Destination.NOTIFICATIONS -> Destination.HOME
        Destination.CREATOR_HUB, Destination.SCRIPT, Destination.STORYBOARD, Destination.CAPTIONS,
        Destination.HASHTAGS, Destination.CALENDAR, Destination.CONTENT_LIBRARY, Destination.TELEPROMPTER -> Destination.CREATOR_HUB
        Destination.BUSINESS_HUB, Destination.CLIENTS, Destination.LEADS, Destination.PROJECTS,
        Destination.TASKS, Destination.INVOICES -> Destination.BUSINESS_HUB
        else -> Destination.VAULT_HUB
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Plum).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        roots.forEach { item ->
            Column(
                modifier = Modifier.clip(RoundedCornerShape(3.dp)).clickable { onSelect(item) }.padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (selectedRoot == item) "◆" else "◇", color = if (selectedRoot == item) Lime else Lilac, fontSize = 12.sp)
                Text(item.label, color = if (selectedRoot == item) Ivory else Lilac, fontSize = 8.sp, fontWeight = if (selectedRoot == item) FontWeight.Bold else FontWeight.Medium, letterSpacing = .4.sp)
            }
        }
    }
}
