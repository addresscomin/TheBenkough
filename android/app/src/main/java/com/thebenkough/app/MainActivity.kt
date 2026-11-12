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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        setContent {
            MaterialTheme {
                BenkoughApp()
            }
        }
    }
}

private enum class Destination(val label: String) {
    HOME("HOME"), CREATE("CREATE"), BUSINESS("BUSINESS"), VAULT("VAULT")
}

@Composable
private fun BenkoughApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main-destination"
            ) { current ->
                when (current) {
                    Destination.HOME -> HomeScreen(
                        onCreate = { destination = Destination.CREATE },
                        onBusiness = { destination = Destination.BUSINESS },
                        onVault = { destination = Destination.VAULT }
                    )
                    Destination.CREATE -> ModuleScreen(
                        eyebrow = "01 / CREATOR STUDIO",
                        title = "MAKE
SOMETHING
MATTER.",
                        description = "A focused space for scripts, scenes, captions and content planning.",
                        accent = Cobalt,
                        items = listOf("SCRIPT ROOM", "STORYBOARD", "CAPTION ENGINE", "CONTENT PLANNER")
                    )
                    Destination.BUSINESS -> ModuleScreen(
                        eyebrow = "02 / BUSINESS HUB",
                        title = "BUILD
WITH
INTENTION.",
                        description = "Keep clients, projects, tasks and business records connected.",
                        accent = Lime,
                        items = listOf("CLIENTS", "LEADS", "PROJECTS & TASKS", "INVOICE RECORDS")
                    )
                    Destination.VAULT -> ModuleScreen(
                        eyebrow = "03 / PROJECT VAULT",
                        title = "KEEP THE
IMPORTANT
THINGS.",
                        description = "A home for project references, assets and working documents.",
                        accent = Rose,
                        items = listOf("PROJECT FILES", "REFERENCES", "RECENT ACTIVITY", "SYNC STATUS")
                    )
                }
            }
            BottomRail(selected = destination, onSelect = { destination = it })
        }
    }
}

@Composable
private fun HomeScreen(onCreate: () -> Unit, onBusiness: () -> Unit, onVault: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("THE BENKOUGH®", color = Ivory, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("CREATIVE BUSINESS SYSTEM", color = Lilac, fontSize = 9.sp, letterSpacing = 2.2.sp)
            }
            Text("BK / 001", color = Lilac, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("CREATE.", color = Ivory, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
            Text("BUILD.", color = Lime, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
            Text("GROW.", color = Color(0xFF9B91F5), fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
        }

        Text(
            "Your ideas, creative work and business operations — in one deliberate workspace.",
            color = Ivory.copy(alpha = .76f), fontSize = 15.sp, lineHeight = 23.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Cobalt)
                .clickable(onClick = onCreate)
                .padding(20.dp)
        ) {
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
            Text("Create your first project to begin organizing your work.", color = Lilac, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Text("DESIGNED FOR THE WORK THAT MOVES YOU FORWARD.", color = Lilac.copy(alpha = .65f), fontSize = 9.sp, letterSpacing = 1.3.sp)
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    background: Color,
    foreground: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(title, color = foreground, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = foreground.copy(alpha = .75f), fontSize = 10.sp)
        Text("OPEN ↗", color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModuleScreen(eyebrow: String, title: String, description: String, accent: Color, items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 34.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("THE BENKOUGH®  /  WORKSPACE", color = Lilac, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(eyebrow, color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Ivory, fontSize = 43.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.8).sp)
        Text(description, color = Lilac, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.size(2.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Plum)
                    .padding(17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("0${index + 1}", color = accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(item, color = Ivory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("↗", color = accent, fontSize = 20.sp)
            }
        }
        Text("FEATURE WORKFLOWS ARE BEING BUILT IN STAGES.", color = Lilac.copy(alpha = .65f), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun BottomRail(selected: Destination, onSelect: (Destination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Plum)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Destination.entries.forEach { item ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (selected == item) "◆" else "◇", color = if (selected == item) Lime else Lilac, fontSize = 12.sp)
                Text(item.label, color = if (selected == item) Ivory else Lilac, fontSize = 9.sp, fontWeight = if (selected == item) FontWeight.Bold else FontWeight.Medium, letterSpacing = .6.sp)
            }
        }
    }
}
