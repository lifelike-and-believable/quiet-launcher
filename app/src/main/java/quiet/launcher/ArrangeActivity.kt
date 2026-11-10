package quiet.launcher

import android.app.Activity

/** Arrange mode (docs/specs/arrange-mode.md, AC-4 to AC-9). */
class ArrangeActivity : Activity() {

    /** The last thing announced to TalkBack; for tests. */
    var lastAnnouncement: String? = null
        private set

    companion object {
        /** The package of the app that was held, selected when arrange mode opens. */
        const val EXTRA_SELECT = "quiet.launcher.SELECT"
    }
}
