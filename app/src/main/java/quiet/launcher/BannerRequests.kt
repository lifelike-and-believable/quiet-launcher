package quiet.launcher

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable

/** An app's banner for the PC's quiet-banners (docs/specs/tv-banners.md, AC-1 to AC-3). */
class BannerRequests(
    private val context: Context,
    private val loadBanner: (ActivityInfo) -> Drawable? = { it.loadBanner(context.packageManager) },
) {

    /** AC-1, AC-2: the banner of [packageName], drawn as a PNG, or the reason there is none. */
    fun banner(packageName: String?): ArrangeRequests.Answer = TODO("tv-banners#AC-1")

    companion object {
        const val ACTION_GET = "quiet.launcher.action.GET_BANNER"
        const val EXTRA_PACKAGE = "package"
        const val MAX_WIDTH = 640
        const val MAX_HEIGHT = 360
    }
}
