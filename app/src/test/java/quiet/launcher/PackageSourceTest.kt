package quiet.launcher

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackageSourceTest {

    private val pm = ApplicationProvider.getApplicationContext<Context>().packageManager

    // app-list#AC-3
    @Test
    fun `AC-3 reads label, install time, launch activity, and whether the app is a home screen`() {
        TvPackages.install(pm, "com.netflix.ninja", "Netflix", installed = 1000L)
        TvPackages.install(pm, "com.spocky.projengmenu", "Projectivy Launcher", installed = 2000L, home = true)

        val entries = PackageSource(pm).read().associateBy { it.packageName }

        val netflix = entries.getValue("com.netflix.ninja")
        assertEquals("Netflix", netflix.label)
        assertEquals(1000L, netflix.firstInstallTime)
        assertEquals("com.netflix.ninja.Main", netflix.activityName)
        assertFalse(netflix.hasHomeActivity)
        assertTrue(entries.getValue("com.spocky.projengmenu").hasHomeActivity)
    }

    // app-list#AC-3
    @Test
    fun `AC-3 artwork is the banner when there is one, otherwise the icon`() {
        val banner = ColorDrawable(0x111111)
        val icon = ColorDrawable(0x222222)

        assertSame(banner, PackageSource.chooseArtwork(banner) { icon })
        assertSame(icon, PackageSource.chooseArtwork(null) { icon })
    }
}
