package quiet.launcher

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** docs/specs/arranging-store.md, AC-6: the quiet home screen draws the arranged list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangedHomeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun names(robot: HomeScreenRobot): List<String> =
        (0 until robot.grid.adapter!!.itemCount).map { robot.tile(it).findViewById<TextView>(R.id.app_label).text.toString() }

    // arranging-store#AC-6
    @Test
    fun `AC-6 the home screen shows the arranged order without the hidden apps`() {
        ArrangementStore(context).save(Arrangement(order = listOf("app.n03", "app.n01"), hidden = listOf("app.n02")))

        val robot = HomeScreenRobot(appCount = 4)

        assertEquals(listOf("App 03", "App 01", "App 04"), names(robot))
    }

    // arranging-store#AC-6
    @Test
    fun `AC-6 a changed arrangement shows the next time the home screen is shown`() {
        val robot = HomeScreenRobot(appCount = 3)
        assertEquals(listOf("App 01", "App 02", "App 03"), names(robot))

        ArrangementStore(context).save(Arrangement(order = listOf("app.n02"), hidden = listOf("app.n03")))
        robot.homePress()

        assertEquals(listOf("App 02", "App 01"), names(robot))
    }
}
