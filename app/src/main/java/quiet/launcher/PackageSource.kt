package quiet.launcher

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Reads the TV's apps from Android's package data (docs/specs/app-list.md, AC-3). */
class PackageSource(private val pm: PackageManager) {

    // STUB (tests-first commit): wrong on purpose.
    fun read(): List<AppEntry> =
        listOf(AppEntry("stub.package", "stub.package.Main", "Stub", 0L, hasHomeActivity = true))

    companion object {
        // STUB (tests-first commit): always the icon.
        fun chooseArtwork(banner: Drawable?, icon: () -> Drawable?): Drawable? = icon()
    }
}
