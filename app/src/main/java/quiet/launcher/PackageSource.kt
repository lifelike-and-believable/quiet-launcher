package quiet.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Reads the TV's apps from Android's package data (docs/specs/app-list.md, AC-3). */
class PackageSource(
    private val pm: PackageManager,
    private val loadArtwork: (ActivityInfo) -> Drawable? = { activity ->
        chooseArtwork(activity.loadBanner(pm)) { activity.loadIcon(pm) }
    },
) {

    fun read(): List<AppEntry> {
        val homePackages = pm.queryIntentActivities(intentFor(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        seen.clear()
        return pm.queryIntentActivities(intentFor(Intent.CATEGORY_LEANBACK_LAUNCHER), 0).map { resolved ->
            val activity = resolved.activityInfo
            val times = installTimes(activity.packageName)
            AppEntry(
                packageName = activity.packageName,
                activityName = activity.name,
                label = resolved.loadLabel(pm).toString(),
                firstInstallTime = times.first,
                hasHomeActivity = activity.packageName in homePackages,
                artwork = artworkFor(activity, times.second),
            ).also { seen += artworkKey(activity, times.second) }
        }.also { artworkCache.keys.retainAll(seen) }
    }

    /** home-takeover#AC-12: loaded once per app, and again only when the app is updated. */
    private val artworkCache = HashMap<String, Drawable?>()
    private val seen = HashSet<String>()

    private fun artworkKey(activity: ActivityInfo, updated: Long) = "${activity.packageName}/${activity.name}@$updated"

    private fun artworkFor(activity: ActivityInfo, updated: Long): Drawable? =
        artworkCache.getOrPut(artworkKey(activity, updated)) { loadArtwork(activity) }

    /** First-install and last-update times. */
    private fun installTimes(packageName: String): Pair<Long, Long> =
        try {
            pm.getPackageInfo(packageName, 0).let { it.firstInstallTime to it.lastUpdateTime }
        } catch (e: PackageManager.NameNotFoundException) {
            0L to 0L
        }

    companion object {
        private fun intentFor(category: String) = Intent(Intent.ACTION_MAIN).addCategory(category)

        /** The banner if the app has one (activity or application), otherwise its icon. */
        fun chooseArtwork(banner: Drawable?, icon: () -> Drawable?): Drawable? = banner ?: icon()
    }
}
