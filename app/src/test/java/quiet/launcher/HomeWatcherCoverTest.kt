package quiet.launcher

import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeWatcherCoverTest {

    private val service = Robolectric.setupService(HomeWatcher::class.java)

    private fun window(pkg: String, cls: String) =
        AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = pkg
            className = cls
        }

    private fun googleTvHome() = window(LAUNCHERX, "$LAUNCHERX.home.HomeActivity")

    private fun quietHome() = window("quiet.launcher", HomeActivity::class.java.name)

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    // home-takeover#AC-11
    @Test
    fun `AC-11 Google TV's home screen is covered at once`() {
        assertFalse(service.isCovering)

        service.onAccessibilityEvent(googleTvHome())

        assertTrue(service.isCovering)
    }

    // home-takeover#AC-11
    @Test
    fun `AC-11 the cover comes down once the quiet home screen is up`() {
        service.onAccessibilityEvent(googleTvHome())

        service.onAccessibilityEvent(quietHome())
        advance(500)

        assertFalse(service.isCovering)
    }

    // home-takeover#AC-11
    @Test
    fun `AC-11 the cover never stays more than 3 seconds`() {
        service.onAccessibilityEvent(googleTvHome())

        advance(2_900)
        assertTrue(service.isCovering)
        advance(200)
        assertFalse(service.isCovering)
    }

    // home-takeover#AC-11: other windows neither raise nor lower the cover.
    @Test
    fun `AC-11 other windows leave the cover as it is`() {
        service.onAccessibilityEvent(window("com.netflix.ninja", "com.netflix.ninja.MainActivity"))
        assertFalse(service.isCovering)

        service.onAccessibilityEvent(googleTvHome())
        service.onAccessibilityEvent(window(LAUNCHERX, "$LAUNCHERX.entity.EntityActivity"))
        advance(500)
        assertTrue(service.isCovering)
    }

    private companion object {
        const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
    }
}
