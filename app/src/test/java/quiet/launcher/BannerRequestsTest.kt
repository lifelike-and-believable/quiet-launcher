package quiet.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Base64

/** docs/specs/tv-banners.md, AC-1 to AC-3: an app's banner for the PC. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BannerRequestsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val drawn = mutableListOf<String>()
    private val banners = HashMap<String, Drawable?>()
    private val requests = BannerRequests(context) { activity -> drawn += activity.packageName; banners[activity.packageName] }

    @Before
    fun installApps() {
        TvPackages.install(context.packageManager, "com.netflix.ninja", "Netflix", installed = 1)
        TvPackages.install(context.packageManager, "tv.plex", "Plex", installed = 2)
        TvPackages.install(context.packageManager, "com.example.nobanner", "No Banner", installed = 3)
        TvPackages.install(context.packageManager, "com.example.home", "Other Home", installed = 4, home = true)
    }

    private fun solid(width: Int, height: Int, color: Int): Drawable {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return BitmapDrawable(context.resources, bitmap).apply { setTargetDensity(bitmap.density) }
    }

    private fun decode(data: String) = JSONObject(String(Base64.getDecoder().decode(data)))

    private fun png(json: JSONObject): Bitmap {
        val bytes = Base64.getDecoder().decode(json.getString("png"))
        assertEquals("a PNG", listOf(0x89, 'P'.code, 'N'.code, 'G'.code), bytes.take(4).map { it.toInt() and 0xff })
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // tv-banners#AC-1
    @Test
    fun `AC-1 it answers with the app's name, package, and banner as a PNG`() {
        banners["com.netflix.ninja"] = solid(320, 180, Color.RED)

        val answer = requests.banner("com.netflix.ninja")
        val json = decode(answer.data)

        assertEquals(ArrangeRequests.DONE, answer.code)
        assertEquals("Netflix", json.getString("name"))
        assertEquals("com.netflix.ninja", json.getString("package"))
        val bitmap = png(json)
        assertEquals(listOf(320, 180), listOf(bitmap.width, bitmap.height))
        assertEquals(Color.RED, bitmap.getPixel(160, 90))
    }

    // tv-banners#AC-1
    @Test
    fun `AC-1 a big banner is scaled down to fit 640 by 360, keeping its shape`() {
        banners["com.netflix.ninja"] = solid(1280, 720, Color.BLUE)
        banners["tv.plex"] = solid(800, 800, Color.GREEN)

        val wide = png(decode(requests.banner("com.netflix.ninja").data))
        val square = png(decode(requests.banner("tv.plex").data))

        assertEquals(listOf(640, 360), listOf(wide.width, wide.height))
        assertEquals(Color.BLUE, wide.getPixel(320, 180))
        assertEquals(listOf(360, 360), listOf(square.width, square.height))
    }

    /** A banner that compresses badly, as a photograph does: every pixel different. */
    private fun noise(width: Int, height: Int): Drawable {
        val random = java.util.Random(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(IntArray(width * height) { random.nextInt() or (0xff shl 24) }, 0, width, 0, 0, width, height)
        return BitmapDrawable(context.resources, bitmap).apply { setTargetDensity(bitmap.density) }
    }

    // tv-banners#AC-1: on the TV, Disney+'s banner (a 512 by 288 photograph) drawn at full size made
    // an answer too big for the broadcast's result to carry; the app hung until Android killed it.
    @Test
    fun `AC-1 a banner whose answer would be too big is drawn smaller until it fits, keeping its shape`() {
        banners["com.netflix.ninja"] = noise(512, 288)

        val answer = requests.banner("com.netflix.ninja")

        assertEquals(ArrangeRequests.DONE, answer.code)
        assertTrue("${answer.data.length} chars", answer.data.length <= BannerRequests.MAX_ANSWER_CHARS)
        val bitmap = png(decode(answer.data))
        assertTrue("${bitmap.width} wide", bitmap.width < 512)
        assertEquals(512f / 288, bitmap.width.toFloat() / bitmap.height, 0.02f)
    }

    // tv-banners#AC-2
    @Test
    fun `AC-2 an app with no banner is refused with the reason`() {
        banners["com.example.nobanner"] = null

        val answer = requests.banner("com.example.nobanner")

        assertEquals(ArrangeRequests.REFUSED, answer.code)
        assertTrue(decode(answer.data).getString("error").contains("no banner"))
    }

    // tv-banners#AC-2
    @Test
    fun `AC-2 a package that is not an app on the home screen is refused and nothing is drawn`() {
        for (pkg in listOf("com.example.missing", "com.example.home", context.packageName, null)) {
            val answer = requests.banner(pkg)
            assertEquals("$pkg", ArrangeRequests.REFUSED, answer.code)
            assertTrue("$pkg", decode(answer.data).getString("error").isNotBlank())
        }
        assertEquals(emptyList<String>(), drawn)
    }

    // tv-banners#AC-3
    @Test
    fun `AC-3 the arrangement's receiver answers the question, behind the same permission`() {
        val info = context.packageManager.getReceiverInfo(ComponentName(context, ArrangeReceiver::class.java), 0)
        assertEquals(android.Manifest.permission.DUMP, info.permission)

        var code = 0
        var data: String? = null
        val result = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { code = resultCode; data = resultData }
        }
        context.sendOrderedBroadcast(
            Intent(BannerRequests.ACTION_GET).setComponent(ComponentName(context, ArrangeReceiver::class.java))
                .putExtra(BannerRequests.EXTRA_PACKAGE, "com.example.missing"),
            null, result, null, Activity.RESULT_CANCELED, null, null,
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(ArrangeRequests.REFUSED, code)
        assertFalse(decode(data!!).getString("error").isBlank())
    }
}
