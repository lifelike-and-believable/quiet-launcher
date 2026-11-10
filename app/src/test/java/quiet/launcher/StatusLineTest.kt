package quiet.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class StatusLineTest {

    // top-row-and-search#AC-9
    @Test
    fun `AC-9 the status line says when there is no internet connection, and clears when it returns`() {
        val r = HomeScreenRobot()
        val cm = r.context.getSystemService(ConnectivityManager::class.java)
        val callbacks = shadowOf(cm).networkCallbacks
        assertTrue("the screen listens for the connection", callbacks.isNotEmpty())
        val network = ShadowNetwork.newInstance(1)

        callbacks.forEach { it.onLost(network) }
        r.idle()
        assertEquals(
            "No internet connection. Search and voice search need it; your apps still open.",
            r.statusLine.text.toString(),
        )

        val online = ShadowNetworkCapabilities.newInstance().also {
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        callbacks.forEach { it.onCapabilitiesChanged(network, online) }
        r.idle()
        assertEquals("", r.statusLine.text.toString())
    }
}
