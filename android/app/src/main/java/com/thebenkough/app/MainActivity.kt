package com.thebenkough.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.thebenkough.app.feature.auth.AccountAuthScreen
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import com.thebenkough.app.feature.script.ScriptRoomScreen
import com.thebenkough.app.feature.workspace.WorkspaceRecordsScreen
import com.thebenkough.app.feature.notifications.NotificationCenterScreen
import com.thebenkough.app.feature.vault.ProjectVaultScreen
import com.thebenkough.app.feature.invoice.InvoiceManagerScreen
import com.thebenkough.app.data.remote.PushRegistration

private val Void = Color(0xFF050608)
private val Night = Color(0xFF0A0D12)
private val Glass = Color(0xFF121821)
private val GlassLight = Color(0xFF1B2530)
private val White = Color(0xFFF4F7FB)
private val Muted = Color(0xFF929EAD)
private val Cyan = Color(0xFF77E6D2)
private val Violet = Color(0xFF9B8CFF)
private val Line = Color(0x35FFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BenkoughApp() } }
    }
}

private enum class Destination(val label: String) {
    HOME("Home"), NOTIFICATIONS("Inbox"), CREATOR_HUB("Create"), BUSINESS_HUB("Business"),
    VAULT_HUB("Vault"), SCRIPT("Script Room"), STORYBOARD("Storyboard"), CAPTIONS("Captions"),
    HASHTAGS("Hashtags"), CALENDAR("Calendar"), CONTENT_LIBRARY("Content Library"),
    TELEPROMPTER("Prompt Notes"), CLIENTS("Clients"), LEADS("Leads"), PROJECTS("Projects"),
    TASKS("Tasks"), INVOICES("Invoices"), ASSETS("Assets & References")
}

@Composable
private fun BenkoughApp() {
    val appContext = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var authChecked by rememberSaveable { mutableStateOf(false) }
    var authenticated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = try { SupabaseProvider.client.auth.currentSessionOrNull() != null }
        catch (_: Throwable) { false }
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (authenticated) runCatching { PushRegistration.registerCurrentDevice(appContext) }
    }

    if (!authChecked) {
        Surface(Modifier.fillMaxSize(), color = Void) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandMark(large = true)
                    Text("SECURE WORKSPACE", color = Muted, fontSize = 10.sp, letterSpacing = 2.5.sp)
                }
            }
        }
        return
    }
    if (!authenticated) {
        AccountAuthScreen(onAuthenticated = { authenticated = true })
        return
    }

    Surface(Modifier.fillMaxSize(), color = Void) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
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
                        eyebrow = "CREATIVE WORKSPACE", title = "Creator Studio",
                        description = "Turn rough ideas into finished content, with every part of your process in one place.",
                        accent = Violet,
                        items = listOf(
                            HubItem("Script Room", "Write and autosave your next idea", Destination.SCRIPT, Icons.Rounded.Description),
                            HubItem("Storyboard", "Shape scenes, shots and sequences", Destination.STORYBOARD, Icons.Rounded.LibraryBooks),
                            HubItem("Captions", "Create platform-ready copy", Destination.CAPTIONS, Icons.Rounded.Description),
                            HubItem("Hashtag Bank", "Keep campaign tags organized", Destination.HASHTAGS, Icons.Rounded.AutoAwesome),
                            HubItem("Content Calendar", "Plan your publishing schedule", Destination.CALENDAR, Icons.Rounded.CalendarMonth),
                            HubItem("Content Library", "Collect drafts and published work", Destination.CONTENT_LIBRARY, Icons.Rounded.LibraryBooks),
                            HubItem("Prompt Notes", "Prepare your speaking notes", Destination.TELEPROMPTER, Icons.Rounded.Mic)
                        ), onOpen = { destination = it }
                    )
                    Destination.BUSINESS_HUB -> HubScreen(
                        eyebrow = "OPERATIONS WORKSPACE", title = "Business Hub",
                        description = "Keep your clients, projects, tasks and money records clear and connected.",
                        accent = Cyan,
                        items = listOf(
                            HubItem("Clients", "People and relationship notes", Destination.CLIENTS, Icons.Rounded.Groups),
                            HubItem("Leads", "Prospects and next follow-ups", Destination.LEADS, Icons.Rounded.Business),
                            HubItem("Projects", "Scope, milestones and status", Destination.PROJECTS, Icons.Rounded.LibraryBooks),
                            HubItem("Tasks", "Priorities and work in progress", Destination.TASKS, Icons.Rounded.Checklist),
                            HubItem("Invoices", "Billing records and payment notes", Destination.INVOICES, Icons.Rounded.ReceiptLong)
                        ), onOpen = { destination = it }
                    )
                    Destination.NOTIFICATIONS -> NotificationCenterScreen()
                    Destination.VAULT_HUB, Destination.ASSETS -> ProjectVaultScreen()
                    Destination.SCRIPT -> ScriptRoomScreen()
                    Destination.STORYBOARD -> RecordsPage("STORYBOARD", "SCENES & SEQUENCES", "Build a scene list with shot ideas, order and production notes.", "Scene name", "Visual, dialogue, shot and sequence notes…")
                    Destination.CAPTIONS -> RecordsPage("CAPTIONS", "CAPTION WORKSHOP", "Draft and store captions, descriptions and calls to action.", "Caption title", "Caption text, CTA and platform notes…")
                    Destination.HASHTAGS -> RecordsPage("HASHTAGS", "HASHTAG BANK", "Save hashtag groups by topic, campaign or platform.", "Collection name", "Add hashtags separated by spaces…")
                    Destination.CALENDAR -> RecordsPage("CALENDAR", "CONTENT CALENDAR", "Record planned publishing dates, channels and content notes.", "Post or campaign name", "Date, platform, time and publishing notes…")
                    Destination.CONTENT_LIBRARY -> RecordsPage("CONTENT_LIBRARY", "CONTENT LIBRARY", "Keep content ideas and published-post references together.", "Content title", "Idea, status, link or production notes…")
                    Destination.TELEPROMPTER -> RecordsPage("TELEPROMPTER_NOTES", "PROMPT NOTES", "Prepare and save speaking scripts.", "Prompt title", "Paste or write the speaking script…")
                    Destination.CLIENTS -> RecordsPage("CLIENTS", "CLIENT DIRECTORY", "Store client names, contact notes and relationship history.", "Client name", "Contact details and meeting notes…")
                    Destination.LEADS -> RecordsPage("LEADS", "LEAD TRACKER", "Record prospects, follow-up dates and next actions.", "Lead or business name", "Source, contact, follow-up date and next step…")
                    Destination.PROJECTS -> RecordsPage("PROJECTS", "PROJECT REGISTER", "Track project scope, milestones and current status.", "Project name", "Scope, deadline, budget and status…")
                    Destination.TASKS -> RecordsPage("TASKS", "TASK BOARD", "Keep work items, due dates and completion notes.", "Task name", "Due date, priority and task details…")
                    Destination.INVOICES -> InvoiceManagerScreen()
                }
            }
            BottomDock(selected = destination, onSelect = { destination = it })
        }
    }
}

@Composable
private fun BrandMark(large: Boolean = false) {
    val dimension = if (large) 66.dp else 48.dp
    Box(
        Modifier.size(dimension).clip(RoundedCornerShape(if (large) 22.dp else 16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF234B50), Color(0xFF1B2942), Color(0xFF34265A))))
            .border(1.dp, Color(0x6677E6D2), RoundedCornerShape(if (large) 22.dp else 16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = "The Benkough", tint = Cyan, modifier = Modifier.size(if (large) 34.dp else 27.dp))
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xCC1B2631), Color(0xAA10151E), Color(0xCC1A1729))))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x6677E6D2), Color(0x22FFFFFF), Color(0x669B8CFF)))), RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = .2.sp)
                Text(subtitle.uppercase(), color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(onCreate: () -> Unit, onBusiness: () -> Unit, onVault: () -> Unit, onNotifications: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1017), Void, Color(0xFF09080F))))
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenHeader("THE BENKOUGH", "Creative operating system") {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(GlassLight)
                    .border(1.dp, Line, CircleShape).clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = Cyan, modifier = Modifier.size(29.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YOUR SPACE.", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
            Text("Make work\nfeel effortless.", color = White, fontSize = 39.sp, lineHeight = 43.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.4).sp)
            Text("One focused place for ideas, creation and business.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        }

        GlassPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Cyan, modifier = Modifier.size(25.dp))
                        Text("BENKOUGH INTELLIGENCE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    }
                    Box(Modifier.clip(CircleShape).background(Color(0x3325D7B0)).padding(horizontal = 11.dp, vertical = 6.dp)) {
                        Text("AI SPACE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                Text("Your next idea\nstarts here.", color = White, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
                Text("AI tools will connect here once a model provider and secure server key are configured.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
                    Text("AI CONNECTION SETUP REQUIRED", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR WORKSPACES", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("03 MODULES", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }

        GlassWorkspaceCard("CREATOR STUDIO", "Scripts · captions · content planning", "01", Violet, Icons.Rounded.AutoAwesome, onCreate)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactWorkspaceCard("BUSINESS", "Clients & tasks", Cyan, Icons.Rounded.Business, Modifier.weight(1f), onBusiness)
            CompactWorkspaceCard("PROJECT VAULT", "Files & assets", Color(0xFFFFC78A), Icons.Rounded.FolderOpen, Modifier.weight(1f), onVault)
        }

        GlassPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("A CLEARER WORKFLOW", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Capture it. Shape it. Move it forward.", color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ArrowOutward, contentDescription = null, tint = Muted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun GlassWorkspaceCard(title: String, subtitle: String, number: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(accent.copy(alpha = .14f))
                    .border(1.dp, accent.copy(alpha = .4f), RoundedCornerShape(19.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.size(15.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(number, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .5.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Rounded.ArrowOutward, contentDescription = "Open $title", tint = accent, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun CompactWorkspaceCard(title: String, subtitle: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    GlassPanel(modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(34.dp))
            Text(title, color = White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .4.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            Icon(Icons.Rounded.ArrowOutward, contentDescription = "Open $title", tint = accent, modifier = Modifier.size(22.dp))
        }
    }
}

private data class HubItem(val title: String, val subtitle: String, val destination: Destination, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun HubScreen(eyebrow: String, title: String, description: String, accent: Color, items: List<HubItem>, onOpen: (Destination) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1017), Void)))
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        ScreenHeader("THE BENKOUGH", eyebrow)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(eyebrow.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(title, color = White, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        items.forEachIndexed { index, item ->
            GlassPanel(Modifier.fillMaxWidth().clickable { onOpen(item.destination) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(accent.copy(alpha = .13f))
                            .border(1.dp, accent.copy(alpha = .3f), RoundedCornerShape(17.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(item.icon, contentDescription = item.title, tint = accent, modifier = Modifier.size(31.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("0${index + 1}  /  ${item.title.uppercase()}", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp)
                        Text(item.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Icon(Icons.Rounded.ArrowOutward, contentDescription = "Open ${item.title}", tint = accent, modifier = Modifier.size(23.dp))
                }
            }
        }
    }
}

@Composable
private fun RecordsPage(module: String, eyebrow: String, title: String, description: String, detailsHint: String) {
    WorkspaceRecordsScreen(module = module, eyebrow = eyebrow, title = title, description = description,
        titleHint = "Name or title", detailsHint = detailsHint)
}

@Composable
private fun BottomDock(selected: Destination, onSelect: (Destination) -> Unit) {
    val roots = listOf(
        Triple(Destination.HOME, Icons.Rounded.Home, "HOME"),
        Triple(Destination.CREATOR_HUB, Icons.Rounded.AutoAwesome, "CREATE"),
        Triple(Destination.BUSINESS_HUB, Icons.Rounded.Business, "BUSINESS"),
        Triple(Destination.VAULT_HUB, Icons.Rounded.FolderOpen, "VAULT")
    )
    val active = when (selected) {
        Destination.HOME, Destination.NOTIFICATIONS -> Destination.HOME
        Destination.CREATOR_HUB, Destination.SCRIPT, Destination.STORYBOARD, Destination.CAPTIONS,
        Destination.HASHTAGS, Destination.CALENDAR, Destination.CONTENT_LIBRARY, Destination.TELEPROMPTER -> Destination.CREATOR_HUB
        Destination.BUSINESS_HUB, Destination.CLIENTS, Destination.LEADS, Destination.PROJECTS,
        Destination.TASKS, Destination.INVOICES -> Destination.BUSINESS_HUB
        else -> Destination.VAULT_HUB
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color(0xEE1A2530), Color(0xF20D1119), Color(0xEE201A31))))
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0x6677E6D2), Color(0x33FFFFFF), Color(0x669B8CFF))), RoundedCornerShape(25.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        roots.forEach { (destination, icon, label) ->
            val isActive = active == destination
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(19.dp))
                    .background(if (isActive) Brush.linearGradient(listOf(Color(0x5547B9A7), Color(0x443C356C))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(destination) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = label, tint = if (isActive) Cyan else Color(0xFF8B97A6), modifier = Modifier.size(29.dp))
                Text(label, color = if (isActive) White else Muted, fontSize = 9.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold, letterSpacing = .7.sp)
            }
        }
    }
}
