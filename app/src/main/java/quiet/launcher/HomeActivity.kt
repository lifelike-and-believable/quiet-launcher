package quiet.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** The quiet home screen (docs/specs/app-list.md, docs/specs/home-takeover.md). */
class HomeActivity : Activity() {

    private lateinit var grid: RecyclerView
    private lateinit var emptyMessage: TextView
    private val adapter = AppAdapter(::open)
    private var firstShow = true
    private var homePressed = false

    /** One reader for the screen's lifetime, so artwork is loaded once (home-takeover#AC-12). */
    private val source by lazy { PackageSource(packageManager) }

    private val packageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = rebuild(selectFirst = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density

        grid = RecyclerView(this).apply {
            id = R.id.app_grid
            layoutManager = GridLayoutManager(this@HomeActivity, COLUMNS)
            adapter = this@HomeActivity.adapter
            clipToPadding = false
            // The TV safe area: 96 x 54 px at 1080p (docs/design/home-screen.md, Layout).
            setPadding((48 * dp).toInt(), (27 * dp).toInt(), (48 * dp).toInt(), (27 * dp).toInt())
        }
        emptyMessage = TextView(this).apply {
            id = R.id.empty_message
            setText(R.string.no_apps)
            setTextColor(Color.parseColor("#a9b4bd"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#101418"))
            addView(grid, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(emptyMessage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        })

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
    }

    /**
     * home-takeover#AC-4: a Home press goes to the top with the first app selected; AC-12: at once,
     * with the list the screen already has.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_HOME, false)) {
            homePressed = true
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
        firstShow = false
        homePressed = false
        // For measuring the Home budget on the TV (home-takeover#AC-8).
        Choreographer.getInstance().postFrameCallback {
            Log.i(HomeWatcher.TAG, "home screen drawn at ${SystemClock.uptimeMillis()}")
        }
    }

    /** home-takeover#AC-5: Back does nothing here, so it can never reveal Google TV's home screen. */
    @Deprecated("Back is handled here for every Android version this app supports.")
    override fun onBackPressed() {
        // Deliberately empty.
    }

    override fun onDestroy() {
        unregisterReceiver(packageChanges)
        super.onDestroy()
    }

    /** app-list#AC-7: rebuild from the package data, keeping the selection where it can. */
    private fun rebuild(selectFirst: Boolean) {
        val selected = grid.findFocus()?.tag as? String
        val oldIndex = adapter.indexOf(selected)
        val apps = AppCatalog.visible(source.read(), packageName)
        adapter.submit(apps)
        emptyMessage.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        if (apps.isEmpty()) return

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
        if (adapter.itemCount == 0) return
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

    companion object {
        private const val COLUMNS = 5

        /** Set by HomeWatcher on an intent that stands for a Home press (home-takeover#AC-4). */
        const val EXTRA_HOME = "quiet.launcher.HOME"
    }
}
