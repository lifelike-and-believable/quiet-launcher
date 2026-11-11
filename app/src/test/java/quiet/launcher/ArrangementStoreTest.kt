package quiet.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** docs/specs/arranging-store.md, AC-5. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArrangementStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file get() = File(context.filesDir, ArrangementStore.FILE_NAME)

    // arranging-store#AC-5
    @Test
    fun `AC-5 with no file there is no arrangement`() {
        assertNull(ArrangementStore(context).load())
    }

    // arranging-store#AC-5: a new store reads what another saved, as after a restart.
    @Test
    fun `AC-5 a saved arrangement is read back the same by a new store`() {
        ArrangementStore(context).save(Arrangement(order = listOf("b", "a"), hidden = listOf("c")))

        val loaded = ArrangementStore(context).load()

        assertEquals(listOf("b", "a"), loaded?.order)
        assertEquals(listOf("c"), loaded?.hidden)
    }

    // arranging-store#AC-5
    @Test
    fun `AC-5 each save increases the version by one, from 1`() {
        val store = ArrangementStore(context)

        assertEquals(1L, store.save(Arrangement(listOf("a"), emptyList())).version)
        assertEquals(2L, store.save(Arrangement(listOf("b"), emptyList())).version)
        assertEquals(2L, ArrangementStore(context).load()?.version)
    }

    // arranging-store#AC-5
    @Test
    fun `AC-5 a file that cannot be read is no arrangement, and nothing fails`() {
        file.writeText("{ not json")

        assertNull(ArrangementStore(context).load())
    }

    // arranging-store#AC-5: written beside and renamed over, so no temporary file is left behind.
    @Test
    fun `AC-5 a save leaves only the arrangement file`() {
        ArrangementStore(context).save(Arrangement(listOf("a"), emptyList()))

        val left = context.filesDir.listFiles()!!.map { it.name }.filter { it.startsWith("arrangement") }

        assertEquals(listOf(ArrangementStore.FILE_NAME), left)
    }
}
