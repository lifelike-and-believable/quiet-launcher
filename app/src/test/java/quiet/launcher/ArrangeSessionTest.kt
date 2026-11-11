package quiet.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** docs/specs/arrange-mode.md: arrange mode's rules, without the screen. */
class ArrangeSessionTest {

    private fun entry(pkg: String) = AppEntry(pkg, "$pkg.Main", pkg.uppercase(), 0, false)

    /** Twelve apps on the home screen (rows of five), two hidden. */
    private fun session() = ArrangeSession(
        shown = (1..12).map { entry("a%02d".format(it)) },
        hidden = listOf(entry("h1"), entry("h2")),
    )

    private fun ArrangeSession.order() = shown.map { it.packageName }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 left and right move one place, across the end of a row`() {
        val s = session()
        s.startMove("a05")

        s.step(ArrangeSession.Direction.RIGHT)
        assertEquals(5, s.order().indexOf("a05"))
        s.step(ArrangeSession.Direction.LEFT)
        s.step(ArrangeSession.Direction.LEFT)
        assertEquals(3, s.order().indexOf("a05"))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 up and down move a row, and stop at the first and last places`() {
        val s = session()
        s.startMove("a03")

        s.step(ArrangeSession.Direction.DOWN)
        assertEquals(7, s.order().indexOf("a03"))
        s.step(ArrangeSession.Direction.DOWN)
        assertEquals(11, s.order().indexOf("a03")) // the last place: never into the hidden apps
        s.step(ArrangeSession.Direction.RIGHT)
        assertEquals(11, s.order().indexOf("a03"))
        repeat(4) { s.step(ArrangeSession.Direction.UP) }
        assertEquals(0, s.order().indexOf("a03"))
        s.step(ArrangeSession.Direction.LEFT)
        assertEquals(0, s.order().indexOf("a03"))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 putting back restores the order from before the move`() {
        val s = session()
        val before = s.order()
        s.startMove("a02")
        s.step(ArrangeSession.Direction.DOWN)

        s.putBack()

        assertEquals(before, s.order())
        assertEquals(null, s.moving)
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 placing keeps the new order in the arrangement`() {
        val s = session()
        s.startMove("a06")
        s.step(ArrangeSession.Direction.UP)

        s.place()

        assertEquals("a06", s.arrangement().order.first())
        assertEquals(listOf("h1", "h2"), s.arrangement().hidden)
    }

    // arrange-mode#AC-7
    @Test
    fun `AC-7 hide moves the app to the end of the hidden apps and selects the app that took its place`() {
        val s = session()

        val next = s.hide("a04")

        assertFalse("a04" in s.order())
        assertEquals(listOf("h1", "h2", "a04"), s.hidden.map { it.packageName })
        assertEquals("a05", next)
        assertEquals(listOf("h1", "h2", "a04"), s.arrangement().hidden)
    }

    // arrange-mode#AC-7: hiding the last app in the list selects the one before it.
    @Test
    fun `AC-7 hiding the last app selects the one before it`() {
        assertEquals("a11", session().hide("a12"))
    }

    // arrange-mode#AC-5
    @Test
    fun `AC-5 the last app on the home screen cannot be hidden`() {
        val s = ArrangeSession(shown = listOf(entry("only")), hidden = listOf(entry("h1")))

        assertFalse(s.canHide("only"))
        assertTrue(session().canHide("a01"))
    }

    // arrange-mode#AC-7
    @Test
    fun `AC-7 show moves a hidden app to the end of the apps`() {
        val s = session()

        s.show("h1")

        assertEquals("h1", s.order().last())
        assertEquals(listOf("h2"), s.hidden.map { it.packageName })
        assertEquals("h1", s.arrangement().order.last())
    }
}
