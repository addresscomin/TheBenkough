package com.thebenkough.app.feature.script

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.thebenkough.app.data.local.BenkoughDatabase

private val EditorInk = Color(0xFF17101E)
private val EditorPlum = Color(0xFF211329)
private val EditorIvory = Color(0xFFF2E9DE)
private val EditorLilac = Color(0xFFB7A7C9)
private val EditorLime = Color(0xFFDDF27A)

@Composable
fun ScriptRoomScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        Room.databaseBuilder(
            context.applicationContext,
            BenkoughDatabase::class.java,
            "benkough-local.db"
        ).build()
    }
    val draftViewModel: ScriptDraftViewModel = viewModel(
        factory = ScriptDraftViewModelFactory(database.scriptDraftDao())
    )
    val state by draftViewModel.state.collectAsState()
    val wordCount = state.body.trim().split(Regex("\\s+")).count { it.isNotBlank() }.let {
        if (state.body.isBlank()) 0 else it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorInk)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Text(
            "THE BENKOUGH®  /  CREATOR STUDIO",
            color = EditorLilac,
            fontSize = 9.sp,
            letterSpacing = 1.6.sp
        )
        Text(
            "SCRIPT ROOM  /  001",
            color = EditorLime,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            "WRITE THE\nFIRST LINE.",
            color = EditorIvory,
            fontSize = 39.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp
        )
        Text(
            "A quiet place to shape the idea before it becomes a finished piece.",
            color = EditorLilac,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Text("DOCUMENT TITLE", color = EditorLilac, fontSize = 9.sp, letterSpacing = 1.4.sp)
        TextField(
            value = state.title,
            onValueChange = draftViewModel::updateTitle,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(3.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = EditorIvory,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = EditorPlum,
                unfocusedContainerColor = EditorPlum,
                focusedTextColor = EditorIvory,
                unfocusedTextColor = EditorIvory,
                cursorColor = EditorLime,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("YOUR SCRIPT", color = EditorLilac, fontSize = 9.sp, letterSpacing = 1.4.sp)
            Text("$wordCount WORDS", color = EditorLime, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        TextField(
            value = state.body,
            onValueChange = draftViewModel::updateBody,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
            minLines = 12,
            shape = RoundedCornerShape(3.dp),
            placeholder = {
                Text("Start with the opening line…", color = EditorLilac.copy(alpha = .65f))
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = EditorIvory,
                fontSize = 15.sp,
                lineHeight = 25.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = EditorPlum,
                unfocusedContainerColor = EditorPlum,
                focusedTextColor = EditorIvory,
                unfocusedTextColor = EditorIvory,
                cursorColor = EditorLime,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Text(state.saveStatus, color = EditorLime, fontSize = 9.sp, letterSpacing = 1.2.sp)
        Text(
            "DRAFTS ARE SAVED LOCALLY ON THIS DEVICE. CLOUD SYNC WILL BE ADDED AFTER THE PRIVATE BACKEND IS CONNECTED.",
            color = EditorLilac.copy(alpha = .65f),
            fontSize = 8.sp,
            lineHeight = 13.sp,
            letterSpacing = .4.sp
        )
    }
}
