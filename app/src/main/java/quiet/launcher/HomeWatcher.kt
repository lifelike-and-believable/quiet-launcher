package quiet.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Takes over the Home button (docs/specs/home-takeover.md, decision-home-takeover).
 * STUB (tests-first commit): wrong on purpose, in the opposite direction from the real behaviour,
 * so the tests of what it must leave alone fail too.
 */
class HomeWatcher : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean = handleKey(event)

    /** Public for tests: true when the key is consumed. STUB: consumes every key, shows nothing. */
    fun handleKey(event: KeyEvent): Boolean = true

    /** STUB: reacts to every window except Google TV's home screen. */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.className?.toString()?.endsWith(".home.HomeActivity") != true) {
            startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onInterrupt() {}
}
