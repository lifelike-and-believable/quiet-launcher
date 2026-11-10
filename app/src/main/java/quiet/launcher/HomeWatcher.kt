package quiet.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Takes over the Home button (docs/specs/home-takeover.md, decision-home-takeover): consumes the
 * Home key and brings the quiet home screen forward, and covers Google TV's home screen at once if
 * it comes up on its own. Nothing else is touched, so Google TV's pages and other apps work as before.
 * Runs in its own process (":watcher"), so the home screen's work never delays its answer to a key.
 */
class HomeWatcher : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var cover: View? = null
    private val lowerCover = Runnable { hideCover() }

    /** True while the cover is up. Public for tests. */
    val isCovering: Boolean get() = cover != null

    /** True while Google TV's home screen may show (google-tv-button). STUB (tests-first commit). */
    val isAllowingGoogleTv: Boolean get() = true

    override fun onKeyEvent(event: KeyEvent): Boolean = handleKey(event)

    /** home-takeover#AC-1 and AC-2. Public for tests: true when the key is consumed. */
    fun handleKey(event: KeyEvent): Boolean {
        Log.d(TAG, "key ${event.keyCode} action ${event.action} repeat ${event.repeatCount} at ${event.eventTime}")
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.i(TAG, "home key at ${event.eventTime}")
            showQuietHome()
        }
        return true
    }

    /** home-takeover#AC-3 and AC-11. */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        val window = event.className?.toString() ?: return
        when {
            pkg == LAUNCHERX && window in GOOGLE_TV_HOME_SCREENS -> {
                Log.i(TAG, "covering $window at ${event.eventTime}")
                showCover()
                showQuietHome()
            }
            pkg == packageName && window == HomeActivity::class.java.name && cover != null -> {
                // The quiet home screen is up: lower the cover once it has had time to draw.
                handler.removeCallbacks(lowerCover)
                handler.postDelayed(lowerCover, SETTLE_MS)
            }
        }
    }

    override fun onInterrupt() {}

    // STUB (tests-first commit): brings the quiet home screen forward whenever the screen turns off.
    override fun onCreate() {
        super.onCreate()
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: Intent) = showQuietHome()
        }
        registerReceiver(receiver, android.content.IntentFilter(Intent.ACTION_SCREEN_OFF), android.content.Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onServiceConnected() {
        Log.i(TAG, "service connected")
    }

    override fun onDestroy() {
        hideCover()
        super.onDestroy()
    }

    private fun showQuietHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(HomeActivity.EXTRA_HOME, true),
        )
    }

    /**
     * A plain overlay in the quiet home screen's background colour over everything, taking no input,
     * so Google TV's home screen is hidden within a frame while the quiet one starts.
     */
    private fun showCover() {
        handler.removeCallbacks(lowerCover)
        handler.postDelayed(lowerCover, MAX_COVER_MS)
        if (cover != null) return
        val view = View(this).apply { setBackgroundColor(Color.parseColor("#101418")) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        )
        try {
            getSystemService(WindowManager::class.java).addView(view, params)
            cover = view
        } catch (e: RuntimeException) {
            Log.w(TAG, "could not raise the cover", e)
        }
    }

    private fun hideCover() {
        handler.removeCallbacks(lowerCover)
        val view = cover ?: return
        cover = null
        try {
            getSystemService(WindowManager::class.java).removeView(view)
        } catch (e: RuntimeException) {
            Log.w(TAG, "could not lower the cover", e)
        }
    }

    companion object {
        const val TAG = "QuietLauncher"
        /** The in-app request from the home screen's Google TV button (google-tv-button#AC-2). */
        const val ACTION_OPEN_GOOGLE_TV = "quiet.launcher.OPEN_GOOGLE_TV"
        private const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
        private val GOOGLE_TV_HOME_SCREENS = setOf(
            "$LAUNCHERX.home.HomeActivity",
            "$LAUNCHERX.home.VanillaModeHomeActivity",
        )

        /** After the quiet home screen's window comes up, how long before the cover is lowered. */
        private const val SETTLE_MS = 250L

        /** The cover never stays longer than this (home-takeover#AC-11). */
        private const val MAX_COVER_MS = 3_000L
    }
}
