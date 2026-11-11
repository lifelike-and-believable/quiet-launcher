package quiet.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Base64

/** docs/specs/arrange-receiver.md, AC-1 and AC-6. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangeReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun send(order: List<String>, hidden: List<String>) = ArrangeRequests(context).set(
        Base64.getEncoder().encodeToString(JSONObject().put("order", JSONArray(order)).put("hidden", JSONArray(hidden)).toString().toByteArray()),
        version = ArrangementStore(context).load()?.version ?: 0,
        replace = false,
    )

    // arrange-receiver#AC-1
    @Test
    fun `AC-1 the receiver is exported and only a holder of DUMP can call it`() {
        val info = context.packageManager.getReceiverInfo(ComponentName(context, ArrangeReceiver::class.java), 0)

        assertTrue(info.exported)
        assertEquals(android.Manifest.permission.DUMP, info.permission)
    }

    // arrange-receiver#AC-6
    @Test
    fun `AC-6 a send from the PC closes arrange mode if it is open`() {
        for (i in 1..3) TvPackages.install(context.packageManager, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
        val arrange = Robolectric.buildActivity(
            ArrangeActivity::class.java,
            Intent(context, ArrangeActivity::class.java).putExtra(ArrangeActivity.EXTRA_SELECT, "app.n01"),
        ).setup()

        assertEquals(ArrangeRequests.DONE, send(listOf("app.n02"), emptyList()).code)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(arrange.get().isFinishing)
    }

    // arrange-receiver#AC-6
    @Test
    fun `AC-6 the home screen shows the arrangement sent from the PC at once`() {
        val robot = HomeScreenRobot(appCount = 3)

        send(listOf("app.n03"), listOf("app.n01"))
        robot.idle()

        val names = (0 until robot.grid.adapter!!.itemCount).map { robot.tile(it).findViewById<TextView>(R.id.app_label).text.toString() }
        assertEquals(listOf("App 03", "App 02"), names)
    }
}
