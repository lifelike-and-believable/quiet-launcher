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

    fun install(pm: PackageManager, pkg: String, label: String, installed: Long, home: Boolean = false, updated: Long = installed) {
        val app = ApplicationInfo().apply {
            packageName = pkg
            nonLocalizedLabel = label
        }
        val info = PackageInfo().apply {
            packageName = pkg
            firstInstallTime = installed
            lastUpdateTime = updated
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

    /**
     * Google's search app (katniss) with its current entry point (`ASSIST`, katniss 8.2 on) and/or its
     * older `GLOBAL_SEARCH` trampoline (top-row-and-search#AC-2).
     */
    fun installGoogleSearch(pm: PackageManager, assist: Boolean = true, trampoline: Boolean = false) {
        val pkg = "com.google.android.katniss"
        val shadow = shadowOf(pm)
        shadow.installPackage(PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply { packageName = pkg }
        })
        if (assist) {
            val component = ComponentName(pkg, "com.google.android.apps.tv.katniss.voiceassisttrampoline.VoiceAssistTrampoline")
            shadow.addActivityIfNotPresent(component).exported = true
            shadow.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_ASSIST).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            })
        }
        if (trampoline) {
            val component = ComponentName(pkg, "com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline")
            shadow.addActivityIfNotPresent(component).exported = true
            shadow.addIntentFilterForActivity(component, IntentFilter("android.search.action.GLOBAL_SEARCH").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            })
        }
    }

    fun remove(pm: PackageManager, pkg: String) {
        shadowOf(pm).deletePackage(pkg)
    }
}
