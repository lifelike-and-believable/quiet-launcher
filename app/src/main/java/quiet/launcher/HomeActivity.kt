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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** The quiet home screen: the grid of apps (docs/specs/app-list.md). */
class HomeActivity : Activity() {

    private lateinit var grid: RecyclerView
    private lateinit var emptyMessage: TextView
    private val adapter = AppAdapter(::open)
    private var firstShow = true

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

    // STUB (tests-first commit): resets the selection on every intent except a Home press.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.getBooleanExtra(EXTRA_HOME, false)) rebuild(selectFirst = true)
    }

    override fun onResume() {
        super.onResume()
        // app-list#AC-6: the first time, the first app; app-list#AC-7: afterwards, keep the selection.
        rebuild(selectFirst = firstShow)
        firstShow = false
    }

    override fun onDestroy() {
        unregisterReceiver(packageChanges)
        super.onDestroy()
    }

    /** app-list#AC-7: rebuild from the package data, keeping the selection where it can. */
    private fun rebuild(selectFirst: Boolean) {
        val selected = grid.findFocus()?.tag as? String
        val oldIndex = adapter.indexOf(selected)
        val apps = AppCatalog.visible(PackageSource(packageManager).read(), packageName)
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
