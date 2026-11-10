package quiet.launcher

/**
 * The set-up person's arrangement of the apps (docs/specs/arranging-store.md): package names in
 * order, the hidden ones, and a version that increases with every save.
 */
data class Arrangement(
    val order: List<String>,
    val hidden: List<String>,
    val version: Long = 0,
)

/** The home screen's list, and the hidden apps for arrange mode. */
data class Arranged(val shown: List<AppEntry>, val hidden: List<AppEntry>)
