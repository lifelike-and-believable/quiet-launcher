package quiet.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.time.Duration

/** Test helper: opens the quiet home screen with a few apps and drives it like the remote would. */
class HomeScreenRobot(appCount: Int = 7) {
    val context: Context = ApplicationProvider.getApplicationContext()

    init {
        val pm = context.packageManager
        for (i in 1..appCount) TvPackages.install(pm, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
    }

    val controller: ActivityController<HomeActivity> = Robolectric.buildActivity(HomeActivity::class.java).setup()
    val activity: HomeActivity get() = controller.get()

    init {
        idle()
    }

    fun idle() = shadowOf(Looper.getMainLooper()).idle()

    fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    fun <T : View> view(id: Int): T = activity.findViewById(id)

    val searchBox: EditText get() = view(R.id.search_box)
    val voice: TextView get() = view(R.id.voice_button)
    val settings: TextView get() = view(R.id.settings_button)
    val statusLine: TextView get() = view(R.id.status_line)
    val overlay: View get() = view(R.id.search_overlay)
    val grid: RecyclerView get() = view(R.id.app_grid)

    fun tile(position: Int): View = grid.findViewHolderForAdapterPosition(position)!!.itemView

    val focused: View? get() = activity.window.decorView.findFocus()

    fun nextStarted(): Intent? = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity

    fun type(text: String) {
        searchBox.requestFocus()
        searchBox.setText(text)
    }

    /** Presses the keyboard's Search key. */
    fun pressSearch() {
        searchBox.onEditorAction(EditorInfo.IME_ACTION_SEARCH)
        idle()
    }

    fun key(code: Int): Boolean {
        val down = activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        val up = activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        idle()
        return down || up
    }

    /** Google's search screen opens on top (the quiet home screen stops being in front), then Back. */
    fun leaveAndComeBack() {
        controller.pause().stop()
        idle()
        controller.restart().start().resume()
        idle()
    }

    fun homePress() {
        controller.newIntent(Intent(context, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_HOME, true))
        idle()
    }
}
