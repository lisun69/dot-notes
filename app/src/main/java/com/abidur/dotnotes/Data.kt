package com.abidur.dotnotes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: Long, val text: String, val done: Boolean, val time: Long)

const val PAGE_COUNT = 2

/** Notes for 2 pages, saved on the phone. */
object Notes {
    private fun p(c: Context) = c.applicationContext.getSharedPreferences("dot_notes", Context.MODE_PRIVATE)
    private fun key(page: Int) = "page_$page"

    fun list(c: Context, page: Int): List<Note> = try {
        val a = JSONArray(p(c).getString(key(page), "[]") ?: "[]")
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Note(o.getLong("id"), o.optString("text"), o.optBoolean("done"), o.optLong("time"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Open notes first (newest on top), then done notes. */
    fun sorted(c: Context, page: Int): List<Note> {
        val all = list(c, page)
        return all.filter { !it.done }.sortedByDescending { it.time } + all.filter { it.done }.sortedByDescending { it.time }
    }

    private fun save(c: Context, page: Int, list: List<Note>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done).put("time", it.time)) }
        p(c).edit().putString(key(page), a.toString()).apply()
        NotesWidget.refresh(c)
    }

    fun add(c: Context, page: Int, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        save(c, page, list(c, page) + Note(now * 1000 + (0..999).random(), t, false, now))
    }

    fun toggle(c: Context, page: Int, id: Long) =
        save(c, page, list(c, page).map { if (it.id == id) it.copy(done = !it.done) else it })

    fun edit(c: Context, page: Int, id: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        save(c, page, list(c, page).map { if (it.id == id) it.copy(text = t) else it })
    }

    fun delete(c: Context, page: Int, id: Long) = save(c, page, list(c, page).filter { it.id != id })

    fun clearDone(c: Context, page: Int) = save(c, page, list(c, page).filter { !it.done })

    fun title(c: Context, page: Int): String =
        p(c).getString("title_$page", null)?.takeIf { it.isNotBlank() } ?: String.format("PAGE %02d", page + 1)

    fun setTitle(c: Context, page: Int, title: String) {
        p(c).edit().putString("title_$page", title.trim().take(14)).apply()
        NotesWidget.refresh(c)
    }
}

/** Widget theme, opacity, and which page each widget shows. */
object WidgetLook {
    private fun p(c: Context) = c.applicationContext.getSharedPreferences("notes_widget", Context.MODE_PRIVATE)

    fun style(c: Context): WidgetStyle = WidgetStyle.values().getOrElse(p(c).getInt("style", 0)) { WidgetStyle.AMOLED }
    fun setStyle(c: Context, s: WidgetStyle) = p(c).edit().putInt("style", s.ordinal).apply()

    fun opacity(c: Context) = p(c).getInt("opacity", 100).coerceIn(0, 100)
    fun setOpacity(c: Context, v: Int) = p(c).edit().putInt("opacity", v.coerceIn(0, 100)).apply()

    fun page(c: Context, widgetId: Int) = p(c).getInt("page_$widgetId", 0).coerceIn(0, PAGE_COUNT - 1)
    fun setPage(c: Context, widgetId: Int, page: Int) = p(c).edit().putInt("page_$widgetId", page).apply()
}

/** Widget themes. Colours are ARGB. */
enum class WidgetStyle(
    val label: String,
    val bgRes: Int,
    val bg: Long,
    val stroke: Long,
    val primary: Long,
    val secondary: Long,
    val faded: Long,
    val light: Boolean
) {
    AMOLED("AMOLED", R.drawable.wbg_amoled, 0xFF000000, 0xFF262626, 0xFFFFFFFF, 0xFF8A8A8A, 0xFF5A5A5A, false),
    LIGHT("LIGHT", R.drawable.wbg_light, 0xFFFFFFFF, 0xFFE2E2E2, 0xFF000000, 0xFF6E6E6E, 0xFFA8A8A8, true),
    CLEAN("CLEAN", R.drawable.wbg_clean, 0xFF1C1C1E, 0x00000000, 0xFFF2F2F2, 0xFF9A9A9A, 0xFF606064, false);

    val primaryInt get() = primary.toInt()
    val secondaryInt get() = secondary.toInt()
    val fadedInt get() = faded.toInt()
}
