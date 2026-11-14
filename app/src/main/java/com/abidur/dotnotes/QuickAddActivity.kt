package com.abidur.dotnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pop-up opened from the widget's red + button. */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startPage = intent.getIntExtra(EXTRA_PAGE, 0).coerceIn(0, PAGE_COUNT - 1)
        setContent {
            NothingTheme {
                var page by remember { mutableIntStateOf(startPage) }
                var text by remember { mutableStateOf("") }
                var added by remember { mutableIntStateOf(0) }
                val focus = remember { FocusRequester() }

                fun save(close: Boolean) {
                    if (text.isBlank()) {
                        if (close) finish()
                        return
                    }
                    Notes.add(this@QuickAddActivity, page, text)
                    text = ""
                    added++
                    if (close) {
                        Toast.makeText(this@QuickAddActivity, "Saved to ${Notes.title(this@QuickAddActivity, page)}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                Box(
                    Modifier.fillMaxSize().background(Color(0xB3000000))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() }
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.padding(20.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    ) {
                        NCardBox(Modifier.widthIn(max = 440.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RedDot()
                                Spacer(Modifier.width(10.dp))
                                NText("NEW NOTE", 14.sp, spacing = 2.sp, modifier = Modifier.weight(1f))
                                if (added > 0) NText("+$added ADDED", 11.sp, NRed)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (i in 0 until PAGE_COUNT) {
                                    NButton(Notes.title(this@QuickAddActivity, i), filled = i == page, modifier = Modifier.weight(1f)) { page = i }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                label = { NText("Write a note", 12.sp) },
                                textStyle = fieldText,
                                colors = fieldColors(),
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NButton("+ Next", modifier = Modifier.weight(1f)) { save(false) }
                                NButton("Save", filled = true, modifier = Modifier.weight(1f)) { save(true) }
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            }
        }
    }
}
