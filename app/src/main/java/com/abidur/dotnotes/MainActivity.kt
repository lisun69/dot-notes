package com.abidur.dotnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NothingTheme { NotesApp(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue = tick.intValue + 1 // reload after changes from the widget
    }
}

@Composable
fun NotesApp(tick: Int) {
    var version by remember { mutableIntStateOf(0) }
    val key = tick + version * 100000
    val changed: () -> Unit = { version += 1 }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().background(NBlack).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotText("NOTES", 3.dp, NWhite)
            Spacer(Modifier.width(8.dp))
            RedDot(7.dp)
        }
        val names = remember(key) { (0 until PAGE_COUNT).map { Notes.title(ctx, it).uppercase() } + "SETUP" }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            names.forEachIndexed { i, n ->
                Column(
                    Modifier.weight(1f).clickable { tab = i }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NText(n, 12.sp, if (i == tab) NWhite else NGrey, spacing = 1.sp, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (i == tab) NRed else Color.Transparent))
                }
            }
        }
        Box(Modifier.weight(1f)) {
            if (tab < PAGE_COUNT) PageTab(tab, key, changed) else SetupTab(key, changed)
        }
    }
}

@Composable
fun PageTab(page: Int, key: Int, changed: () -> Unit) {
    val ctx = LocalContext.current
    val notes = remember(key, page) { Notes.sorted(ctx, page) }
    var input by remember(page) { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    val done = notes.count { it.done }

    fun add() {
        if (input.isNotBlank()) {
            Notes.add(ctx, page, input)
            input = ""
            changed()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { NText("Write a note", 12.sp) },
                textStyle = fieldText,
                colors = fieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(NRed).clickable { add() },
                contentAlignment = Alignment.Center
            ) { DotText("+", 3.dp, NWhite) }
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NLabel(if (notes.isEmpty()) "No notes yet" else "$done of ${notes.size} done", Modifier.weight(1f))
            if (done > 0) {
                NText("CLEAR DONE", 11.sp, NRed, Modifier.clickable { Notes.clearDone(ctx, page); changed() }.padding(6.dp), spacing = 1.sp)
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 30.dp)) {
            items(notes, key = { it.id }) { n ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(CircleShape).clickable { Notes.toggle(ctx, page, n.id); changed() }.padding(8.dp)) {
                        Check(n.done)
                    }
                    Spacer(Modifier.width(6.dp))
                    NText(
                        n.text, 15.sp, if (n.done) NFaded else NWhite,
                        Modifier.weight(1f).clickable { editing = n }.padding(vertical = 8.dp),
                        strike = n.done
                    )
                }
            }
        }
        NText("Tap the circle to check off. Tap the text to edit or delete.", 10.sp, NGrey, Modifier.padding(bottom = 12.dp))
    }

    editing?.let { n ->
        var text by remember(n.id) { mutableStateOf(n.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = NCard,
            title = { NText("EDIT NOTE", 15.sp, spacing = 1.sp) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    textStyle = fieldText, colors = fieldColors(), maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                NButton("Save", filled = true) { Notes.edit(ctx, page, n.id, text); editing = null; changed() }
            },
            dismissButton = {
                NButton("Delete") { Notes.delete(ctx, page, n.id); editing = null; changed() }
            }
        )
    }
}

@Composable
fun SetupTab(key: Int, changed: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NCardBox {
            NLabel("Home screen widget")
            Spacer(Modifier.height(8.dp))
            NText("Theme: ${WidgetLook.style(ctx).label}  ·  Opacity: ${WidgetLook.opacity(ctx)}%", 12.sp)
            Spacer(Modifier.height(12.dp))
            NButton("Widget theme & opacity", filled = true, modifier = Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(ctx, WidgetSettingsActivity::class.java))
            }
            Spacer(Modifier.height(10.dp))
            NText(
                "Add: long-press home screen > Widgets > Dot Notes. Resize: long-press and drag the edges. Tap a page name to switch page. Tap a note to check it off. Red + adds a note. Dot-gear changes the look.",
                11.sp, NGrey
            )
        }

        NCardBox {
            NLabel("Page names")
            Spacer(Modifier.height(8.dp))
            NText("Rename your 2 pages, e.g. TO DO and IDEAS (max 14 letters).", 11.sp, NGrey)
            for (i in 0 until PAGE_COUNT) {
                var name by remember(key, i) { mutableStateOf(Notes.title(ctx, i)) }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(14) },
                        label = { NText("Page ${i + 1}", 12.sp) },
                        singleLine = true,
                        textStyle = fieldText,
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    NButton("Save") { Notes.setTitle(ctx, i, name); changed() }
                }
            }
        }

        val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RedDot(5.dp)
                Spacer(Modifier.width(6.dp))
                NText("DOT NOTES  v$ver", 10.sp, NGrey, spacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
            NText("Developed by Lisun", 10.sp, NGrey)
        }
    }
}
