package quiet.launcher

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Arrange mode (docs/specs/arrange-mode.md, AC-4 to AC-9; docs/design/arranging.md). Opened from the
 * home screen's question; every change is saved as it is made; Back, Home, or two minutes with no
 * button pressed leave.
 */
class ArrangeActivity : Activity() {

    /** The last thing announced to TalkBack; for tests. */
    var lastAnnouncement: String? = null
        private set

    private val store by lazy { ArrangementStore(this) }
    private lateinit var session: ArrangeSession
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { leave() }

    /** arrange-receiver#AC-6: the PC sent an arrangement, so this one is out of date. */
    private val changedFromPc = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) = finish()
    }

    private lateinit var grid: RecyclerView
    private lateinit var count: TextView
    private lateinit var statusLine: TextView
    private lateinit var menu: LinearLayout
    private lateinit var root: FrameLayout
    private val items = ArrangeItems()

    /** The app the open menu belongs to. */
    private var menuFor: AppEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arranged = AppCatalog.arranged(AppCatalog.visible(PackageSource(packageManager).read(), packageName), store.load())
        session = ArrangeSession(arranged.shown, arranged.hidden)
        setContentView(buildScreen())
        refresh(select = intent.getStringExtra(EXTRA_SELECT) ?: session.shown.firstOrNull()?.packageName)
        announce(getString(R.string.arrange_announcement, countText()))
        restartTimeout()
        onArrangementChanged(changedFromPc)
    }

    override fun onDestroy() {
        unregisterReceiver(changedFromPc)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ---- Keys (AC-5, AC-6, AC-8) ----

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        restartTimeout()
        val down = event.action == KeyEvent.ACTION_DOWN
        val up = event.action == KeyEvent.ACTION_UP
        val moving = session.moving
        if (moving != null) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> if (down) step(ArrangeSession.Direction.LEFT)
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (down) step(ArrangeSession.Direction.RIGHT)
                KeyEvent.KEYCODE_DPAD_UP -> if (down) step(ArrangeSession.Direction.UP)
                KeyEvent.KEYCODE_DPAD_DOWN -> if (down) step(ArrangeSession.Direction.DOWN)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (up) place()
                KeyEvent.KEYCODE_BACK -> if (up) putBack()
                KeyEvent.KEYCODE_HOME -> return super.dispatchKeyEvent(event)
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (up) { if (menu.visibility == View.VISIBLE) closeMenu() else leave() }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun restartTimeout() {
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    /** AC-8: an app being moved goes back where it was; everything else was saved already. */
    private fun leave() {
        if (session.moving != null) session.putBack()
        finish()
    }

    // ---- The menu (AC-5) ----

    private fun openMenu(app: AppEntry) {
        if (session.moving != null) return
        menuFor = app
        val isHidden = session.hidden.any { it.packageName == app.packageName }
        val canHide = !isHidden && session.canHide(app.packageName)
        val visible = when {
            isHidden -> listOf(R.id.menu_show, R.id.menu_cancel)
            canHide -> listOf(R.id.menu_move, R.id.menu_hide, R.id.menu_cancel)
            else -> listOf(R.id.menu_move, R.id.menu_cancel)
        }
        status(if (!isHidden && !canHide) getString(R.string.keep_one) else "")
        for (i in 0 until menu.childCount) {
            val item = menu.getChildAt(i)
            item.visibility = if (item.id in visible) View.VISIBLE else View.GONE
        }
        // Up and Down stay inside the menu; Left and Right do nothing.
        visible.forEachIndexed { i, id ->
            findViewById<View>(id).apply {
                nextFocusUpId = visible[(i - 1).coerceAtLeast(0)]
                nextFocusDownId = visible[(i + 1).coerceAtMost(visible.lastIndex)]
                nextFocusLeftId = id
                nextFocusRightId = id
            }
        }
        placeMenuBeside(app.packageName)
        menu.visibility = View.VISIBLE
        findViewById<View>(visible.first()).requestFocus()
    }

    private fun closeMenu(select: String? = menuFor?.packageName) {
        menu.visibility = View.GONE
        menuFor = null
        focus(select)
    }

    /** Beside the tile: to its right, or to its left in the last column (design-arranging, Layout). */
    private fun placeMenuBeside(pkg: String) {
        val tile = tileView(pkg) ?: return
        val tileAt = IntArray(2).also { tile.getLocationInWindow(it) }
        val rootAt = IntArray(2).also { root.getLocationInWindow(it) }
        val dp = resources.displayMetrics.density
        val width = (150 * dp).toInt()
        menu.layoutParams = (menu.layoutParams as FrameLayout.LayoutParams).apply { this.width = width }
        val right = tileAt[0] - rootAt[0] + tile.width + (8 * dp).toInt()
        val x = if (right + width > root.width) tileAt[0] - rootAt[0] - width - (8 * dp).toInt() else right
        menu.x = x.toFloat()
        menu.y = (tileAt[1] - rootAt[1]).toFloat()
    }

    private fun onMenu(id: Int) {
        val app = menuFor ?: return
        when (id) {
            R.id.menu_move -> {
                menu.visibility = View.GONE
                menuFor = null
                session.startMove(app.packageName)
                status(getString(R.string.moving, app.label))
                refresh(select = app.packageName)
            }
            R.id.menu_hide -> {
                menu.visibility = View.GONE
                menuFor = null
                val next = session.hide(app.packageName)
                save()
                status(getString(R.string.hid, app.label))
                refresh(select = next)
            }
            R.id.menu_show -> {
                menu.visibility = View.GONE
                menuFor = null
                val index = session.hidden.indexOfFirst { it.packageName == app.packageName }
                session.show(app.packageName)
                save()
                status(getString(R.string.showed, app.label))
                // The next hidden app, or the app itself if none is left.
                val next = session.hidden.getOrNull(index)?.packageName ?: session.hidden.lastOrNull()?.packageName ?: app.packageName
                refresh(select = next)
            }
            else -> closeMenu()
        }
    }

    // ---- Moving (AC-6) ----

    private fun step(direction: ArrangeSession.Direction) {
        val pkg = session.moving ?: return
        session.step(direction)
        refresh(select = pkg, slide = true)
        announce(place(R.string.moving_step, pkg))
    }

    private fun place() {
        val pkg = session.moving ?: return
        session.place()
        save()
        status("")
        refresh(select = pkg)
        announce(place(R.string.placed, pkg))
    }

    private fun putBack() {
        val pkg = session.moving ?: return
        session.putBack()
        status("")
        refresh(select = pkg, slide = true)
        announce(place(R.string.put_back, pkg))
    }

    /** "<name>, <place> of <count>" for TalkBack (AC-9). */
    private fun place(text: Int, pkg: String): String {
        val index = session.shown.indexOfFirst { it.packageName == pkg }
        return getString(text, session.shown[index].label, index + 1, session.shown.size)
    }

    private fun save() = store.save(session.arrangement())

    // ---- Drawing ----

    private fun countText(): String =
        if (session.hidden.isEmpty()) getString(R.string.no_hidden_count)
        else resources.getQuantityString(R.plurals.hidden_count, session.hidden.size, session.hidden.size)

    private fun status(text: String) {
        statusLine.text = text
    }

    private fun announce(text: String) {
        lastAnnouncement = text
        window.decorView.announceForAccessibility(text)
    }

    /**
     * AC-6 and AC-7: the apps slide to their new places only while an app is being moved ([slide]);
     * anything else (Hide, Show, putting an app down) draws them in place.
     */
    private fun refresh(select: String?, slide: Boolean = false) {
        grid.itemAnimator = if (slide) slideAnimator else null
        items.set(session.shown, session.hidden)
        count.text = countText()
        grid.adapter?.notifyDataSetChanged()
        focus(select)
    }

    private val slideAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

    private fun focus(pkg: String?) {
        val position = items.positionOf(pkg ?: return)
        if (position < 0) return
        val holder = grid.findViewHolderForAdapterPosition(position)
        if (holder != null && !grid.isComputingLayout) {
            holder.itemView.requestFocus()
        } else {
            grid.scrollToPosition(position)
        }
        grid.post { grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
    }

    private fun tileView(pkg: String): View? =
        grid.findViewHolderForAdapterPosition(items.positionOf(pkg))?.itemView

    private fun buildScreen(): View {
        val dp = resources.displayMetrics.density
        fun px(v: Float) = (v * dp).toInt()

        val title = TextView(this).apply {
            id = R.id.arrange_title
            setText(R.string.arrange_title)
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        count = TextView(this).apply {
            id = R.id.arrange_count
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.END
        }
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(count, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val instructions = TextView(this).apply {
            id = R.id.arrange_instructions
            setText(R.string.arrange_instructions)
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        statusLine = TextView(this).apply {
            id = R.id.status_line
            setTextColor(NOTE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(18f), 0, 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

        val tiles = AppAdapter { app -> openMenu(app) }.apply { decorate = ::decorate }
        grid = RecyclerView(this).apply {
            id = R.id.arrange_grid
            layoutManager = ArrangeGridLayoutManager(this@ArrangeActivity, items)
            adapter = ArrangeAdapter(items, tiles)
            itemAnimator = null
            clipToPadding = false
            clipChildren = false
            setPadding(0, px(14f), 0, 0)
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(px(48f), px(27f), px(48f), 0)
            addView(headRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(instructions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(3f) })
            addView(statusLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(28f)))
            addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        menu = LinearLayout(this).apply {
            id = R.id.arrange_menu
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(px(6f), px(6f), px(6f), px(6f))
            background = GradientDrawable().apply {
                cornerRadius = 9 * dp
                setColor(Color.parseColor("#262d34"))
                setStroke(dp.toInt().coerceAtLeast(1), Color.parseColor("#59646e"))
            }
            elevation = 8 * dp
            for ((id, text) in listOf(
                R.id.menu_move to R.string.menu_move,
                R.id.menu_hide to R.string.menu_hide,
                R.id.menu_show to R.string.menu_show,
                R.id.menu_cancel to R.string.menu_cancel,
            )) {
                addView(menuItem(id, text, dp), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(36f)))
            }
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(BACKGROUND)
            clipChildren = false
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(menu, FrameLayout.LayoutParams(px(150f), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return root
    }

    private fun menuItem(id: Int, text: Int, dp: Float) = TextView(this).apply {
        this.id = id
        setText(text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER_VERTICAL
        setPadding((11 * dp).toInt(), 0, (11 * dp).toInt(), 0)
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply { cornerRadius = 6 * dp; setColor(Color.WHITE) })
            addState(intArrayOf(), GradientDrawable().apply { cornerRadius = 6 * dp; setColor(Color.TRANSPARENT) })
        }
        setOnFocusChangeListener { v, has -> (v as TextView).setTextColor(if (has) Color.parseColor("#0b0f13") else TEXT) }
        setTextColor(TEXT)
        setOnClickListener { onMenu(id) }
    }

    /** Hidden apps darkened and marked; the app being moved lifted with a dashed blue outline. */
    private fun decorate(tile: View, frame: FrameLayout, app: AppEntry, focused: Boolean) {
        val dp = resources.displayMetrics.density
        val isHidden = session.hidden.any { it.packageName == app.packageName }
        frame.findViewById<View>(R.id.app_art).alpha = if (isHidden) 0.4f else 1f
        frame.findViewById<View>(R.id.hidden_badge).visibility = if (isHidden) View.VISIBLE else View.GONE
        tile.contentDescription = if (isHidden) getString(R.string.hidden_app, app.label) else app.label
        if (app.packageName == session.moving) {
            frame.foreground = DashedRing(dp)
            frame.animate().cancel()
            frame.scaleX = 1.1f
            frame.scaleY = 1.1f
        } else if (frame.foreground is DashedRing) {
            // Put down or back (AC-6): no longer lifted. A selected tile gets the selection's look
            // again, ring and size, from the adapter's own focus handling.
            frame.foreground = null
            frame.scaleX = 1f
            frame.scaleY = 1f
            if (focused) tile.onFocusChangeListener?.onFocusChange(tile, true)
        } else if (!focused) {
            frame.foreground = null
        }
    }

    companion object {
        /** The package of the app that was held, selected when arrange mode opens. */
        const val EXTRA_SELECT = "quiet.launcher.SELECT"

        /** AC-8: two minutes with no button pressed. */
        const val TIMEOUT_MS = 120_000L

        private val BACKGROUND = Color.parseColor("#101418")
        private val TEXT = Color.parseColor("#eef2f5")
        private val MUTED = Color.parseColor("#a9b4bd")
        private val NOTE = Color.parseColor("#ffd28a")
    }
}

/** What the arrange grid shows: the apps, then the hidden apps' label, then the hidden apps or a note. */
class ArrangeItems {
    sealed interface Item
    data class App(val entry: AppEntry) : Item
    object Label : Item
    object NoHidden : Item

    var list: List<Item> = emptyList()
        private set

    fun set(shown: List<AppEntry>, hidden: List<AppEntry>) {
        list = shown.map { App(it) } + Label + (if (hidden.isEmpty()) listOf(NoHidden) else hidden.map { App(it) })
    }

    fun positionOf(pkg: String): Int = list.indexOfFirst { it is App && it.entry.packageName == pkg }
}

/** The arrange grid's items; app tiles come from [tiles], the home screen's adapter. */
class ArrangeAdapter(private val items: ArrangeItems, private val tiles: AppAdapter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private class Plain(view: View) : RecyclerView.ViewHolder(view)

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = items.list.size

    override fun getItemId(position: Int): Long = when (val item = items.list[position]) {
        is ArrangeItems.App -> item.entry.packageName.hashCode().toLong()
        ArrangeItems.Label -> -1
        ArrangeItems.NoHidden -> -2
    }

    override fun getItemViewType(position: Int) = when (items.list[position]) {
        is ArrangeItems.App -> APP
        ArrangeItems.Label -> LABEL
        ArrangeItems.NoHidden -> NO_HIDDEN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val dp = parent.resources.displayMetrics.density
        return when (viewType) {
            APP -> tiles.onCreateViewHolder(parent, 0)
            LABEL -> Plain(LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                setPadding((4 * dp).toInt(), (14 * dp).toInt(), 0, (6 * dp).toInt())
                addView(TextView(parent.context).apply {
                    id = R.id.hidden_label
                    setText(R.string.hidden_label)
                    setTextColor(Color.parseColor("#eef2f5"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(parent.context).apply {
                    setText(R.string.hidden_note)
                    setTextColor(Color.parseColor("#a9b4bd"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setPadding((8 * dp).toInt(), 0, 0, 0)
                })
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            else -> Plain(TextView(parent.context).apply {
                id = R.id.no_hidden
                setText(R.string.no_hidden)
                setTextColor(Color.parseColor("#a9b4bd"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding((4 * dp).toInt(), (6 * dp).toInt(), 0, 0)
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.list[position]
        if (item is ArrangeItems.App) {
            // The note beside the label only when there are hidden apps (design-arranging, Layout).
            (holder as AppAdapter.Tile).bind(item.entry)
        } else if (item == ArrangeItems.Label) {
            val note = (holder.itemView as LinearLayout).getChildAt(1)
            note.visibility = if (items.list.lastOrNull() == ArrangeItems.NoHidden) View.GONE else View.VISIBLE
        }
    }

    companion object {
        const val APP = 0
        const val LABEL = 1
        const val NO_HIDDEN = 2
    }
}

/**
 * The arrange grid: five tiles a row, the label and the note full width. Like the home screen's
 * grid, the direction pad stops at the ends of rows instead of wrapping (top-row-and-search#AC-10).
 */
class ArrangeGridLayoutManager(context: Context, private val items: ArrangeItems) :
    GridLayoutManager(context, ArrangeSession.COLUMNS) {

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int) =
                if (items.list.getOrNull(position) is ArrangeItems.App) 1 else ArrangeSession.COLUMNS
        }
    }

    override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
        val tile = findContainingItemView(focused) ?: return null
        val position = getPosition(tile)
        val column = spanSizeLookup.getSpanIndex(position, spanCount)
        val nextIsTile = items.list.getOrNull(position + 1) is ArrangeItems.App
        val lastTile = items.list.indexOfLast { it is ArrangeItems.App }
        val stop = when (direction) {
            View.FOCUS_LEFT -> column == 0
            View.FOCUS_RIGHT -> column == spanCount - 1 || !nextIsTile
            View.FOCUS_DOWN -> spanSizeLookup.getSpanGroupIndex(position, spanCount) ==
                spanSizeLookup.getSpanGroupIndex(lastTile, spanCount)
            else -> false
        }
        return if (stop) focused else null
    }
}

/** The moving app's outline: a dashed blue ring with a black edge (design-arranging, Layout). */
private class DashedRing(private val dp: Float) : Drawable() {
    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 5 * dp }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#8ab4f8"); strokeWidth = 3 * dp
        pathEffect = DashPathEffect(floatArrayOf(6 * dp, 4 * dp), 0f)
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val r = android.graphics.RectF(bounds).apply { inset(2.5f * dp, 2.5f * dp) }
        canvas.drawRoundRect(r, 6 * dp, 6 * dp, black)
        canvas.drawRoundRect(r, 6 * dp, 6 * dp, blue)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
    @Deprecated("Deprecated in Android", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}
