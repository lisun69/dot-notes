package com.abidur.dotnotes

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Widget theme + opacity. Opens from the widget's dot-gear, from Setup, or when adding the widget. */
class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        }
        setContent {
            NothingTheme {
                WidgetSettingsScreen {
                    NotesWidget.refresh(this)
                    finish()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        NotesWidget.refresh(this)
    }
}

@Composable
fun WidgetSettingsScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var style by remember { mutableStateOf(WidgetLook.style(ctx)) }
    var opacity by remember { mutableFloatStateOf(WidgetLook.opacity(ctx).toFloat()) }

    Column(
        Modifier.fillMaxSize().background(NBlack).verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
            DotText("WIDGET", 3.dp, NWhite)
            Spacer(Modifier.width(8.dp))
            RedDot(7.dp)
        }

        WidgetPreview(style, opacity.roundToInt())

        NCardBox {
            NLabel("Theme")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetStyle.values().forEach { s ->
                    NButton(s.label, filled = s == style, modifier = Modifier.weight(1f)) {
                        style = s
                        WidgetLook.setStyle(ctx, s)
                        NotesWidget.refresh(ctx)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            NText(
                when (style) {
                    WidgetStyle.AMOLED -> "Pure black with a thin border. Best for dark wallpapers and battery."
                    WidgetStyle.LIGHT -> "White background with black text."
                    WidgetStyle.CLEAN -> "Soft dark grey, no border. Calm and minimal."
                },
                11.sp, NGrey
            )
        }

        NCardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NLabel("Background opacity", Modifier.weight(1f))
                NText("${opacity.roundToInt()}%", 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Slider(
                value = opacity,
                onValueChange = { opacity = it; WidgetLook.setOpacity(ctx, it.roundToInt()) },
                onValueChangeFinished = { NotesWidget.refresh(ctx) },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = NRed, activeTrackColor = NRed, inactiveTrackColor = NLine,
                    activeTickColor = NWhite.copy(alpha = 0.4f), inactiveTickColor = NGrey.copy(alpha = 0.4f)
                )
            )
            NText("0% = see-through (only text shows). 100% = solid.", 11.sp, NGrey)
        }

        NButton("Done", filled = true, modifier = Modifier.fillMaxWidth()) { onDone() }
    }
}

/** Live preview of the widget on a sample wallpaper. */
@Composable
fun WidgetPreview(style: WidgetStyle, opacity: Int) {
    val ctx = LocalContext.current
    val wall = Brush.linearGradient(listOf(Color(0xFF4A5A6A), Color(0xFF8A6A50), Color(0xFF2A3A4A)))
    val pri = Color(style.primary)
    val sec = Color(style.secondary)
    val faded = Color(style.faded)
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(wall).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(230.dp).clip(shape)
                .background(Color(style.bg).copy(alpha = opacity / 100f))
                .then(
                    if (style.stroke != 0x00000000L) Modifier.border(1.dp, Color(style.stroke).copy(alpha = opacity / 100f), shape)
                    else Modifier
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RedDot(6.dp)
                Spacer(Modifier.width(6.dp))
                NText(Notes.title(ctx, 0).uppercase(), 10.sp, pri, maxLines = 1)
                Spacer(Modifier.width(10.dp))
                NText(Notes.title(ctx, 1).uppercase(), 10.sp, faded, maxLines = 1)
                Spacer(Modifier.weight(1f))
                NText("1/3", 9.sp, sec)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(22.dp).clip(CircleShape).background(NRed), contentAlignment = Alignment.Center) {
                    NText("+", 13.sp, NWhite)
                }
            }
            Spacer(Modifier.height(10.dp))
            listOf("Buy groceries" to false, "Call Shariful bhai" to false, "Send CS tracker" to true).forEach { (t, d) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Check(d, 13.dp, ring = sec)
                    Spacer(Modifier.width(8.dp))
                    NText(t, 11.sp, if (d) faded else pri, strike = d, maxLines = 1)
                }
            }
        }
    }
}
