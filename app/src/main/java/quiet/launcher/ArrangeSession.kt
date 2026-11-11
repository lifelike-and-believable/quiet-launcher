package quiet.launcher

/** Arrange mode's rules (docs/specs/arrange-mode.md, AC-5 to AC-7), apart from the screen. */
class ArrangeSession(shown: List<AppEntry>, hidden: List<AppEntry>) {

    enum class Direction { LEFT, RIGHT, UP, DOWN }

    private val shownApps = shown.toMutableList()
    private val hiddenApps = hidden.toMutableList()
    private var beforeMove: List<AppEntry>? = null

    val shown: List<AppEntry> get() = shownApps
    val hidden: List<AppEntry> get() = hiddenApps

    /** The package being moved, or null. */
    var moving: String? = null
        private set

    fun startMove(pkg: String) {
        beforeMove = shownApps.toList()
        moving = pkg
    }

    /** AC-6: one place left or right, a row up or down, never past the first or last place. */
    fun step(direction: Direction) {
        val pkg = moving ?: return
        val from = shownApps.indexOfFirst { it.packageName == pkg }
        if (from < 0) return
        val to = (from + when (direction) {
            Direction.LEFT -> -1
            Direction.RIGHT -> 1
            Direction.UP -> -COLUMNS
            Direction.DOWN -> COLUMNS
        }).coerceIn(0, shownApps.lastIndex)
        if (to == from) return
        shownApps.add(to, shownApps.removeAt(from))
    }

    fun place() {
        moving = null
        beforeMove = null
    }

    fun putBack() {
        beforeMove?.let { shownApps.clear(); shownApps.addAll(it) }
        place()
    }

    /** AC-5: the last app on the home screen stays. */
    fun canHide(pkg: String): Boolean = shownApps.size > 1 && shownApps.any { it.packageName == pkg }

    /** AC-7: returns the package to select next: the app that took its place, or the one before it. */
    fun hide(pkg: String): String? {
        if (!canHide(pkg)) return null
        val index = shownApps.indexOfFirst { it.packageName == pkg }
        hiddenApps.add(shownApps.removeAt(index))
        return shownApps[index.coerceAtMost(shownApps.lastIndex)].packageName
    }

    fun show(pkg: String) {
        val index = hiddenApps.indexOfFirst { it.packageName == pkg }
        if (index < 0) return
        shownApps.add(hiddenApps.removeAt(index))
    }

    fun arrangement(): Arrangement = Arrangement(
        order = shownApps.map { it.packageName },
        hidden = hiddenApps.map { it.packageName },
    )

    companion object {
        const val COLUMNS = 5
    }
}
