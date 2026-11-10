package quiet.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Reads the TV's apps from Android's package data (docs/specs/app-list.md, AC-3). */
class PackageSource(private val pm: PackageManager) {

    fun read(): List<AppEntry> {
        val homePackages = pm.queryIntentActivities(intentFor(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        return pm.queryIntentActivities(intentFor(Intent.CATEGORY_LEANBACK_LAUNCHER), 0).map { resolved ->
            val activity = resolved.activityInfo
            AppEntry(
                packageName = activity.packageName,
                activityName = activity.name,
                label = resolved.loadLabel(pm).toString(),
                firstInstallTime = firstInstallTime(activity.packageName),
                hasHomeActivity = activity.packageName in homePackages,
                artwork = chooseArtwork(activity.loadBanner(pm)) { activity.loadIcon(pm) },
            )
        }
    }

    private fun firstInstallTime(packageName: String): Long =
        try {
            pm.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }

    companion object {
        private fun intentFor(category: String) = Intent(Intent.ACTION_MAIN).addCategory(category)

        /** The banner if the app has one (activity or application), otherwise its icon. */
        fun chooseArtwork(banner: Drawable?, icon: () -> Drawable?): Drawable? = banner ?: icon()
    }
}
