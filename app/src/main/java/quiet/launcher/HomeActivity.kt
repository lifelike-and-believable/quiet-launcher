package quiet.launcher

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextClock
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The quiet home screen (docs/design/home-screen.md): the top row, the status line, the grid of
 * apps, and the searching overlay. Specs: docs/specs/app-list.md, home-takeover.md, and
 * top-row-and-search.md.
 */
class HomeActivity : Activity() {

    private lateinit var grid: RecyclerView
    private lateinit var emptyMessage: TextView
    private lateinit var searchBox: EditText
    private lateinit var voiceButton: TextView
    private lateinit var settingsButton: TextView
    private lateinit var statusLine: TextView
    private lateinit var overlay: View
    private lateinit var overlayTitle: TextView
    private lateinit var spinner: View

    private val adapter = AppAdapter(::open)
    private var firstShow = true
    private var homePressed = false

    /** One reader for the screen's lifetime, so artwork is loaded once (home-takeover#AC-12). */
    private val source by lazy { PackageSource(packageManager) }

    private val handler = Handler(Looper.getMainLooper())

    /** True from the Search key until Google's search screen opens, is cancelled, or times out. */
    private var searching = false

    /** The control to select when the screen is shown again after a search, voice search, or Settings. */
    private var returnTo = 0

    private var offline = false
    private var searchFailed = false

    private val searchTimeout = Runnable { searchDidNotOpen() }

    private val packageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = rebuild(selectFirst = false)
    }

    private val connectivity = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) = runOnUiThread { setOffline(true) }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = runOnUiThread {
            setOffline(!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChanges, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageChanges, filter)
        }

        // top-row-and-search#AC-9: the status line follows the connection.
        val cm = getSystemService(ConnectivityManager::class.java)
        offline = cm.activeNetwork == null
        cm.registerDefaultNetworkCallback(connectivity)
        updateStatus()
    }

    /**
     * home-takeover#AC-4: a Home press goes to the top with the first app selected; AC-12: at once,
     * with the list the screen already has. top-row-and-search#AC-11: it also clears the search box.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_HOME, false)) {
            homePressed = true
            endSearch()
            searchFailed = false
            returnTo = 0
            searchBox.setText("")
            hideKeyboard()
            updateStatus()
            selectFirstNow()
            scheduleReload(selectFirst = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (firstShow) {
            // app-list#AC-6: nothing to show yet, so read the apps now and select the first.
            rebuild(selectFirst = true)
        } else {
            // home-takeover#AC-12: draw what is there first, then read the apps again (app-list#AC-7),
            // keeping the selection, or the first app after a Home press (home-takeover#AC-4).
            if (homePressed) selectFirstNow()
            scheduleReload(selectFirst = homePressed)
        }
        // top-row-and-search#AC-4 and AC-6: back from Google's screens or Settings, select the control used.
        if (!homePressed && returnTo != 0) findViewById<View>(returnTo)?.requestFocus()
        returnTo = 0
        firstShow = false
        homePressed = false
        // For measuring the Home budget on the TV (home-takeover#AC-8).
        Choreographer.getInstance().postFrameCallback {
            Log.i(HomeWatcher.TAG, "home screen drawn at ${SystemClock.uptimeMillis()}")
        }
    }

    override fun onPause() {
        super.onPause()
        // top-row-and-search#AC-4: Google's search screen has opened on top.
        if (searching) {
            endSearch()
            searchBox.setText("")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(packageChanges)
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(connectivity)
        super.onDestroy()
    }

    /** top-row-and-search#AC-3: while searching, only Back (to cancel) and Home act. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (searching) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (event.action == KeyEvent.ACTION_UP) cancelSearch()
                    true
                }
                KeyEvent.KEYCODE_HOME -> super.dispatchKeyEvent(event)
                else -> true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * home-takeover#AC-5: Back never closes this screen, so it can never reveal Google TV's home
     * screen. top-row-and-search#AC-11: from the search box, it goes to the first app.
     */
    @Deprecated("Back is handled here for every Android version this app supports.")
    override fun onBackPressed() {
        if (searchBox.hasFocus()) selectFirstNow()
    }

    // ---- Search (top-row-and-search#AC-2 to AC-6) ----

    private fun submitSearch(): Boolean {
        val text = searchBox.text.toString()
        if (text.isEmpty()) {
            searchBox.announceForAccessibility(getString(R.string.type_something))
            return true // keeps the keyboard open
        }
        searchFailed = false
        updateStatus()
        overlayTitle.text = getString(R.string.searching_for, text)
        spinner.visibility = if (animationsOn()) View.VISIBLE else View.GONE
        overlay.visibility = View.VISIBLE
        searching = true
        returnTo = R.id.search_box
        hideKeyboard()
        handler.postDelayed(searchTimeout, SEARCH_TIMEOUT_MS)
        try {
            startActivity(googleSearch().putExtra("query", text))
        } catch (e: ActivityNotFoundException) {
            Log.w(HomeWatcher.TAG, "Google's search is not installed", e)
            searchDidNotOpen()
        }
        return true
    }

    private fun cancelSearch() {
        endSearch()
        searchBox.requestFocus()
    }

    private fun searchDidNotOpen() {
        endSearch()
        searchFailed = true
        updateStatus()
        searchBox.requestFocus()
    }

    private fun endSearch() {
        handler.removeCallbacks(searchTimeout)
        searching = false
        overlay.visibility = View.GONE
    }

    private fun voiceSearch() {
        returnTo = R.id.voice_button
        try {
            startActivity(googleSearch())
        } catch (e: ActivityNotFoundException) {
            Log.w(HomeWatcher.TAG, "Google's voice search is not installed", e)
        }
    }

    /**
     * google-tv-button#AC-2: the service opens it, so it knows not to cover it. The request stays in
     * the app: it names this package, and the service's receiver is not exported.
     */
    private fun openGoogleTv() {
        sendBroadcast(Intent(HomeWatcher.ACTION_OPEN_GOOGLE_TV).setPackage(packageName))
    }

    private fun openSettings() {
        returnTo = R.id.settings_button
        // A fresh Settings task, at its top level, not wherever it was last left.
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun googleSearch() = Intent("android.search.action.GLOBAL_SEARCH")
        .setComponent(ComponentName(KATNISS, "com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun animationsOn(): Boolean =
        ValueAnimator.areAnimatorsEnabled() &&
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(searchBox.windowToken, 0)
    }

    // ---- Status line (top-row-and-search#AC-1, AC-5, AC-9, AC-12) ----

    private fun setOffline(value: Boolean) {
        if (offline == value) return
        offline = value
        updateStatus()
    }

    private fun updateStatus() {
        statusLine.accessibilityLiveRegion =
            if (searchFailed) View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE else View.ACCESSIBILITY_LIVE_REGION_POLITE
        statusLine.text = when {
            searchFailed -> getString(R.string.search_failed)
            offline -> getString(R.string.offline)
            else -> ""
        }
    }

    // ---- The grid (app-list, home-takeover) ----

    /** app-list#AC-7: rebuild from the package data, keeping the selection where it can. */
    private fun rebuild(selectFirst: Boolean) {
        val selected = grid.findFocus()?.tag as? String
        val focusElsewhere = !selectFirst && selected == null && window.decorView.findFocus() != null
        val oldIndex = adapter.indexOf(selected)
        val apps = AppCatalog.visible(source.read(), packageName)
        adapter.submit(apps)
        emptyMessage.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        if (apps.isEmpty()) {
            searchBox.requestFocus()
            return
        }
        // The selection is in the top row: leave it there.
        if (focusElsewhere) return

        val target = when {
            selectFirst -> 0
            adapter.indexOf(selected) >= 0 -> adapter.indexOf(selected)
            oldIndex >= 0 -> oldIndex.coerceAtMost(apps.lastIndex)
            else -> 0
        }
        if (selectFirst) grid.scrollToPosition(0)
        grid.post { focusTile(target) }
    }

    private var reloadPending = false
    private var reloadSelectsFirst = false

    /** Read the apps again after the next frame; at most one pending, selecting first if any asked. */
    private fun scheduleReload(selectFirst: Boolean) {
        reloadSelectsFirst = reloadSelectsFirst || selectFirst
        if (reloadPending) return
        reloadPending = true
        grid.post {
            val first = reloadSelectsFirst
            reloadPending = false
            reloadSelectsFirst = false
            rebuild(selectFirst = first)
        }
    }

    /** The first app, at the top, from the list already shown. */
    private fun selectFirstNow() {
        if (adapter.itemCount == 0) {
            searchBox.requestFocus()
            return
        }
        grid.scrollToPosition(0)
        focusTile(0)
    }

    private fun focusTile(position: Int) {
        val holder = grid.findViewHolderForAdapterPosition(position)
        if (holder != null) {
            holder.itemView.requestFocus()
        } else {
            grid.scrollToPosition(position)
            grid.post { grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
        }
    }

    /** app-list#AC-5: open the app's TV launch activity in a new task. */
    private fun open(app: AppEntry) {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(ComponentName(app.packageName, app.activityName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    // ---- Layout (docs/design/home-screen.md, Layout; sizes are px at 1080p / 2) ----

    private fun buildScreen(): View {
        val dp = resources.displayMetrics.density
        fun px(v: Float) = (v * dp).toInt()

        searchBox = EditText(this).apply {
            id = R.id.search_box
            setHint(R.string.search_hint)
            setHintTextColor(MUTED)
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            background = pill(px(22f).toFloat(), dp)
            setPadding(px(16f), 0, px(16f), 0)
            compoundDrawablePadding = px(8f)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0)
            nextFocusLeftId = R.id.search_box
            setOnEditorActionListener { _, actionId, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) submitSearch() else false
            }
        }
        voiceButton = button(R.id.voice_button, R.string.voice_search, R.drawable.ic_mic, dp) { voiceSearch() }
        // google-tv-button#AC-1 and AC-2: asks the service to open Google TV's own home screen.
        val googleTvButton = button(R.id.google_tv_button, R.string.google_tv, R.drawable.ic_tv, dp) { openGoogleTv() }
        settingsButton = button(R.id.settings_button, R.string.settings, R.drawable.ic_settings, dp) { openSettings() }
            .apply { nextFocusRightId = R.id.settings_button }
        val clock = TextClock(this).apply {
            id = R.id.clock
            format12Hour = "h:mm"
            format24Hour = "H:mm"
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            isFocusable = false
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
        }
        val topRow = LinearLayout(this).apply {
            id = R.id.top_row
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            addView(searchBox, LinearLayout.LayoutParams(px(360f), px(44f)))
            addView(voiceButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44f)).apply { marginStart = px(12f) })
            addView(View(this@HomeActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(googleTvButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44f)).apply { marginEnd = px(12f) })
            addView(settingsButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44f)))
            addView(clock, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = px(16f) })
        }
        statusLine = TextView(this).apply {
            id = R.id.status_line
            setTextColor(NOTE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(18f), 0, 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        grid = RecyclerView(this).apply {
            id = R.id.app_grid
            layoutManager = EdgeStopGridLayoutManager(this@HomeActivity, COLUMNS)
            adapter = this@HomeActivity.adapter
            clipToPadding = false
            clipChildren = false
        }
        emptyMessage = TextView(this).apply {
            id = R.id.empty_message
            setText(R.string.no_apps)
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val body = FrameLayout(this).apply {
            clipChildren = false
            addView(grid, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(emptyMessage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            // The TV safe area: 96 x 54 px at 1080p.
            setPadding(px(48f), px(27f), px(48f), 0)
            addView(topRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(48f)))
            addView(statusLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(28f)))
            addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        overlayTitle = TextView(this).apply {
            id = R.id.search_overlay_title
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        spinner = ProgressBar(this).apply { id = R.id.search_spinner; isIndeterminate = true }
        overlay = FrameLayout(this).apply {
            id = R.id.search_overlay
            setBackgroundColor(Color.parseColor("#DB101418"))
            isClickable = true
            visibility = View.GONE
            addView(LinearLayout(this@HomeActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(spinner, LinearLayout.LayoutParams(px(48f), px(48f)).apply { bottomMargin = px(18f) })
                addView(overlayTitle)
                addView(TextView(this@HomeActivity).apply {
                    id = R.id.search_overlay_note
                    setText(R.string.search_opening)
                    setTextColor(MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    gravity = Gravity.CENTER
                    setPadding(0, px(18f), 0, 0)
                })
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }

        return FrameLayout(this).apply {
            setBackgroundColor(BACKGROUND)
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(this@HomeActivity.overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun button(id: Int, text: Int, icon: Int, dp: Float, onClick: () -> Unit) = TextView(this).apply {
        this.id = id
        setText(text)
        setTextColor(TEXT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        background = pill(22 * dp, dp)
        setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
        compoundDrawablePadding = (8 * dp).toInt()
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        setOnClickListener { onClick() }
    }

    /** A rounded control: a visible edge (at least 3:1), and a white outline when selected. */
    private fun pill(radius: Float, dp: Float) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
            cornerRadius = radius
            setColor(Color.parseColor("#2b343c"))
            setStroke((2.5f * dp).toInt(), Color.WHITE)
        })
        addState(intArrayOf(), GradientDrawable().apply {
            cornerRadius = radius
            setColor(Color.parseColor("#1c2228"))
            setStroke(dp.toInt().coerceAtLeast(1), Color.parseColor("#59646e"))
        })
    }

    companion object {
        private const val COLUMNS = 5
        private const val KATNISS = "com.google.android.katniss"
        private const val SEARCH_TIMEOUT_MS = 10_000L
        private val BACKGROUND = Color.parseColor("#101418")
        private val TEXT = Color.parseColor("#eef2f5")
        private val MUTED = Color.parseColor("#a9b4bd")
        private val NOTE = Color.parseColor("#ffd28a")

        /** Set by HomeWatcher on an intent that stands for a Home press (home-takeover#AC-4). */
        const val EXTRA_HOME = "quiet.launcher.HOME"
    }
}
