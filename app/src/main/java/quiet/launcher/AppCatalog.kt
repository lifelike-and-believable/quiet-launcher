package quiet.launcher

/** Which apps the quiet home screen shows, and in what order (docs/specs/app-list.md). */
object AppCatalog {
    const val PLAY_STORE = "com.android.vending"

    // STUB (tests-first commit): wrong on purpose, so every test fails on its assertion.
    fun visible(entries: List<AppEntry>, ownPackage: String): List<AppEntry> = entries.reversed()
}
