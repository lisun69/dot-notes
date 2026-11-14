package com.abidur.dotnotes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

const val ACTION_PAGE = "com.abidur.dotnotes.PAGE"
const val ACTION_ITEM = "com.abidur.dotnotes.ITEM"
const val EXTRA_PAGE = "page"
const val EXTRA_NOTE = "note"

/** Resizable 2-page notes widget. Tap a note to check it off (strike-through). */
class NotesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { draw(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        draw(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            ACTION_PAGE -> {
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    WidgetLook.setPage(context, id, intent.getIntExtra(EXTRA_PAGE, 0))
                    refresh(context)
                }
            }
            ACTION_ITEM -> {
                val page = intent.getIntExtra(EXTRA_PAGE, 0)
                val note = intent.getLongExtra(EXTRA_NOTE, 0L)
                if (note != 0L) Notes.toggle(context, page, note)
            }
        }
    }

    companion object {
        private val TABS = intArrayOf(R.id.tab0, R.id.tab1)

        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NotesWidget::class.java))
            ids.forEach { draw(context, mgr, it) }
            if (ids.isNotEmpty()) mgr.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        private fun draw(c: Context, mgr: AppWidgetManager, id: Int) {
            val st = WidgetLook.style(c)
            val page = WidgetLook.page(c, id)
            val notes = Notes.list(c, page)
            val done = notes.count { it.done }
            val h = mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: 200
            val w = mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 250

            val v = RemoteViews(c.packageName, R.layout.widget_notes)

            // theme + opacity
            v.setImageViewResource(R.id.bg, st.bgRes)
            v.setInt(R.id.bg, "setImageAlpha", WidgetLook.opacity(c) * 255 / 100)
            v.setInt(R.id.settings_btn, "setColorFilter", st.secondaryInt)
            v.setTextColor(R.id.count, st.secondaryInt)
            v.setTextColor(R.id.empty, st.secondaryInt)

            // 2 page tabs
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            for (i in 0 until PAGE_COUNT) {
                val selected = i == page
                v.setTextViewText(TABS[i], Notes.title(c, i).uppercase())
                v.setTextColor(TABS[i], if (selected) st.primaryInt else st.fadedInt)
                v.setOnClickPendingIntent(
                    TABS[i],
                    PendingIntent.getBroadcast(
                        c, id * 10 + i,
                        Intent(c, NotesWidget::class.java).setAction(ACTION_PAGE)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra(EXTRA_PAGE, i),
                        flags
                    )
                )
            }
            v.setViewVisibility(R.id.page_dot0, if (page == 0) View.VISIBLE else View.INVISIBLE)
            v.setViewVisibility(R.id.page_dot1, if (page == 1) View.VISIBLE else View.INVISIBLE)
            v.setTextViewText(R.id.count, if (notes.isEmpty()) "" else "$done/${notes.size}")
            v.setViewVisibility(R.id.count, if (w < 200) View.GONE else View.VISIBLE)
            v.setViewVisibility(R.id.settings_btn, if (h < 90) View.GONE else View.VISIBLE)

            // scrollable list
            val svc = Intent(c, NotesService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            @Suppress("DEPRECATION")
            v.setRemoteAdapter(R.id.list, svc)
            v.setEmptyView(R.id.list, R.id.empty)
            v.setPendingIntentTemplate(
                R.id.list,
                PendingIntent.getBroadcast(
                    c, 5000 + id,
                    Intent(c, NotesWidget::class.java).setAction(ACTION_ITEM),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            // buttons
            v.setOnClickPendingIntent(
                R.id.add_btn,
                PendingIntent.getActivity(
                    c, 7000 + id,
                    Intent(c, QuickAddActivity::class.java).putExtra(EXTRA_PAGE, page)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    flags
                )
            )
            v.setOnClickPendingIntent(
                R.id.settings_btn,
                PendingIntent.getActivity(
                    c, 2,
                    Intent(c, WidgetSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    flags
                )
            )
            v.setOnClickPendingIntent(
                R.id.empty,
                PendingIntent.getActivity(
                    c, 8000 + id,
                    Intent(c, QuickAddActivity::class.java).putExtra(EXTRA_PAGE, page)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    flags
                )
            )
            mgr.updateAppWidget(id, v)
        }
    }
}

/** Supplies the note rows to the widget list. */
class NotesService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        NotesFactory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))
}

class NotesFactory(private val c: Context, private val widgetId: Int) : RemoteViewsService.RemoteViewsFactory {
    private var page = 0
    private var items: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        page = WidgetLook.page(c, widgetId)
        items = Notes.sorted(c, page)
    }

    override fun onDestroy() {}

    override fun getCount() = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val st = WidgetLook.style(c)
        val v = RemoteViews(c.packageName, R.layout.widget_note_item)
        val n = items.getOrNull(position) ?: return v
        if (n.done) {
            val s = SpannableString(n.text)
            s.setSpan(StrikethroughSpan(), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            v.setTextViewText(R.id.text, s)
            v.setTextColor(R.id.text, st.fadedInt)
            v.setImageViewResource(R.id.check, R.drawable.check_on)
        } else {
            v.setTextViewText(R.id.text, n.text)
            v.setTextColor(R.id.text, st.primaryInt)
            v.setImageViewResource(R.id.check, if (st.light) R.drawable.check_off_light else R.drawable.check_off)
        }
        v.setOnClickFillInIntent(R.id.item_root, Intent().putExtra(EXTRA_PAGE, page).putExtra(EXTRA_NOTE, n.id))
        return v
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = items.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds() = true
}
