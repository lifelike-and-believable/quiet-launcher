package quiet.launcher

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManifestTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // home-takeover#AC-6
    @Test
    fun `AC-6 the service is not exported and only Android can bind it`() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, HomeWatcher::class.java),
            PackageManager.GET_META_DATA,
        )

        assertFalse(info.exported)
        assertEquals(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, info.permission)
    }

    // home-takeover#AC-6
    @Test
    fun `AC-6 the service asks for window-state events and key filtering only`() {
        val parser = context.resources.getXml(R.xml.home_watcher)
        while (parser.eventType != XmlPullParser.START_TAG) parser.next()
        val ns = "http://schemas.android.com/apk/res/android"

        assertEquals(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            parser.getAttributeIntValue(ns, "accessibilityEventTypes", -1),
        )
        assertEquals(
            AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS,
            parser.getAttributeIntValue(ns, "accessibilityFlags", -1),
        )
        assertTrue(parser.getAttributeBooleanValue(ns, "canRequestFilterKeyEvents", false))
        assertFalse(parser.getAttributeBooleanValue(ns, "canRetrieveWindowContent", false))
    }
}
