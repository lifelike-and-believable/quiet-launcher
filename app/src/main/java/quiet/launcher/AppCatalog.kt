package quiet.launcher

/** Which apps the quiet home screen shows, and in what order (docs/specs/app-list.md). */
object AppCatalog {
    const val PLAY_STORE = "com.android.vending"

    /**
     * Google system apps that Google TV's own app list leaves out. The Play Store is the fourth, but
     * the quiet home screen shows it, last (design-home-screen, How it works).
     */
    private val HIDDEN_SYSTEM_APPS = setOf(
        "com.android.tv.settings",
        "com.google.android.play.games",
        "com.google.android.videos",
    )

    /** app-list#AC-1 (which apps) and AC-2 (order). */
    fun visible(entries: List<AppEntry>, ownPackage: String): List<AppEntry> =
        entries
            .filter { it.packageName != ownPackage }
            .filter { !it.hasHomeActivity }
            .filter { it.packageName !in HIDDEN_SYSTEM_APPS }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<AppEntry> { it.packageName == PLAY_STORE }
                    .thenBy { it.firstInstallTime }
                    .thenBy { it.label.lowercase() },
            )

    /** arranging-store#AC-1 to AC-4: the arrangement applied to the list [visible] gives. */
    fun arranged(apps: List<AppEntry>, arrangement: Arrangement?): Arranged = TODO("arranging-store#AC-1")
}
