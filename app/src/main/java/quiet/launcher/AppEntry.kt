package quiet.launcher

import android.graphics.drawable.Drawable

/** One app the TV can open from a home screen, as read from Android's package data. */
data class AppEntry(
    val packageName: String,
    /** The `MAIN` + `LEANBACK_LAUNCHER` activity the entry came from; the tile opens it. */
    val activityName: String,
    val label: String,
    val firstInstallTime: Long,
    val hasHomeActivity: Boolean,
    val artwork: Drawable? = null,
)
