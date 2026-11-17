package quiet.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.min
import kotlin.math.roundToInt

/** An app's banner for the PC's quiet-banners (docs/specs/tv-banners.md, AC-1 to AC-3). */
class BannerRequests(
    private val context: Context,
    private val loadBanner: (ActivityInfo) -> Drawable? = { it.loadBanner(context.packageManager) },
) {

    /** AC-1, AC-2: the banner of [packageName], drawn as a PNG, or the reason there is none. */
    fun banner(packageName: String?): ArrangeRequests.Answer {
        // AC-2: only the apps the home screen shows; nothing is drawn for anything else.
        val pm = context.packageManager
        val apps = AppCatalog.visible(PackageSource(pm) { null }.read(), context.packageName)
        val app = apps.firstOrNull { it.packageName == packageName }
            ?: return refused("$packageName is not an app on the home screen")
        val activity = try {
            pm.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
        } catch (e: Exception) {
            return refused("${app.packageName} could not be read: ${e.message ?: e.javaClass.simpleName}")
        }
        val banner = loadBanner(activity) ?: return refused("${app.label} has no banner")
        val png = draw(banner)
        return answer(
            ArrangeRequests.DONE,
            JSONObject().put("name", app.label).put("package", app.packageName)
                .put("png", Base64.getEncoder().encodeToString(png)),
        )
    }

    /** AC-1: drawn at its own size, scaled down (never up) to fit 640 by 360, as a PNG. */
    private fun draw(banner: Drawable): ByteArray {
        val width = banner.intrinsicWidth.takeIf { it > 0 } ?: MAX_WIDTH
        val height = banner.intrinsicHeight.takeIf { it > 0 } ?: MAX_HEIGHT
        val scale = min(1f, min(MAX_WIDTH.toFloat() / width, MAX_HEIGHT.toFloat() / height))
        val w = (width * scale).roundToInt().coerceAtLeast(1)
        val h = (height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bounds = banner.copyBounds()
        banner.setBounds(0, 0, w, h)
        banner.draw(Canvas(bitmap))
        banner.bounds = bounds
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            .also { bitmap.recycle() }
    }

    private fun refused(reason: String) = answer(ArrangeRequests.REFUSED, JSONObject().put("error", reason))

    private fun answer(code: Int, json: JSONObject) =
        ArrangeRequests.Answer(code, Base64.getEncoder().encodeToString(json.toString().toByteArray()))

    companion object {
        const val ACTION_GET = "quiet.launcher.action.GET_BANNER"
        const val EXTRA_PACKAGE = "package"
        const val MAX_WIDTH = 640
        const val MAX_HEIGHT = 360

        /** At most this many characters in an answer: a broadcast's result goes through Binder as UTF-16, and
         *  Binder carries about 1 MB at once (tv-banners#AC-1). */
        const val MAX_ANSWER_CHARS = 200_000
    }
}
