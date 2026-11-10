package quiet.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.robolectric.Shadows.shadowOf

/** Test helper: fake TV apps in Robolectric's package manager. */
object TvPackages {

    fun install(pm: PackageManager, pkg: String, label: String, installed: Long, home: Boolean = false) {
        val app = ApplicationInfo().apply {
            packageName = pkg
            nonLocalizedLabel = label
        }
        val info = PackageInfo().apply {
            packageName = pkg
            firstInstallTime = installed
            applicationInfo = app
        }
        val shadow = shadowOf(pm)
        shadow.installPackage(info)
        val component = ComponentName(pkg, "$pkg.Main")
        shadow.addActivityIfNotPresent(component).apply {
            nonLocalizedLabel = label
            applicationInfo = app
            exported = true
        }
        shadow.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            addCategory(Intent.CATEGORY_DEFAULT)
        })
        if (home) {
            shadow.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            })
        }
    }

    fun remove(pm: PackageManager, pkg: String) {
        shadowOf(pm).deletePackage(pkg)
    }
}
