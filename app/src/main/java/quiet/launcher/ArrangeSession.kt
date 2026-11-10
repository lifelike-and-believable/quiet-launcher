package quiet.launcher

/** Arrange mode's rules (docs/specs/arrange-mode.md, AC-5 to AC-7), apart from the screen. */
class ArrangeSession(shown: List<AppEntry>, hidden: List<AppEntry>) {

    enum class Direction { LEFT, RIGHT, UP, DOWN }

    val shown: List<AppEntry> get() = TODO("arrange-mode#AC-6")
    val hidden: List<AppEntry> get() = TODO("arrange-mode#AC-7")
    val moving: String? get() = TODO("arrange-mode#AC-6")

    fun startMove(pkg: String): Unit = TODO("arrange-mode#AC-6")
    fun step(direction: Direction): Unit = TODO("arrange-mode#AC-6")
    fun place(): Unit = TODO("arrange-mode#AC-6")
    fun putBack(): Unit = TODO("arrange-mode#AC-6")
    fun canHide(pkg: String): Boolean = TODO("arrange-mode#AC-5")
    fun hide(pkg: String): String? = TODO("arrange-mode#AC-7")
    fun show(pkg: String): Unit = TODO("arrange-mode#AC-7")
    fun arrangement(): Arrangement = TODO("arrange-mode#AC-6")
}
