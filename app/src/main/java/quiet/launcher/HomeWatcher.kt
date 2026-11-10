package quiet.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Takes over the Home button (docs/specs/home-takeover.md, decision-home-takeover): consumes the
 * Home key and brings the quiet home screen forward, and covers Google TV's home screen if it comes
 * up on its own. Nothing else is touched, so Google TV's pages and other apps work as before.
 */
class HomeWatcher : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean = handleKey(event)

    /** home-takeover#AC-1 and AC-2. Public for tests: true when the key is consumed. */
    fun handleKey(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.i(TAG, "home key at ${event.eventTime}")
            showQuietHome()
        }
        return true
    }

    /** home-takeover#AC-3. */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != LAUNCHERX) return
        val window = event.className?.toString() ?: return
        if (window in GOOGLE_TV_HOME_SCREENS) {
            Log.i(TAG, "covering $window at ${event.eventTime}")
            showQuietHome()
        }
    }

    /** True while the cover is up. Public for tests. STUB until implemented. */
    val isCovering: Boolean get() = true

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        Log.i(TAG, "service connected")
    }

    private fun showQuietHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(HomeActivity.EXTRA_HOME, true),
        )
    }

    companion object {
        const val TAG = "QuietLauncher"
        private const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
        private val GOOGLE_TV_HOME_SCREENS = setOf(
            "$LAUNCHERX.home.HomeActivity",
            "$LAUNCHERX.home.VanillaModeHomeActivity",
        )
    }
}
