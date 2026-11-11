package quiet.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

/** docs/specs/arrange-receiver.md, AC-2 to AC-5: what the receiver answers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArrangeRequestsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ArrangementStore(context)
    private val requests = ArrangeRequests(context)

    @Before
    fun installApps() {
        for (i in 1..4) TvPackages.install(context.packageManager, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
    }

    private fun decode(data: String) = JSONObject(String(Base64.getDecoder().decode(data)))

    private fun encode(order: List<String>, hidden: List<String>) = Base64.getEncoder().encodeToString(
        JSONObject().put("order", JSONArray(order)).put("hidden", JSONArray(hidden)).toString().toByteArray(),
    )

    // arrange-receiver#AC-2
    @Test
    fun `AC-2 with nothing arranged it answers the usual order, version 0, not arranged`() {
        val answer = requests.get()
        val json = decode(answer.data)

        assertEquals(ArrangeRequests.DONE, answer.code)
        assertEquals(0L, json.getLong("version"))
        assertFalse(json.getBoolean("arranged"))
        val apps = json.getJSONArray("apps")
        assertEquals(4, apps.length())
        assertEquals("App 01", apps.getJSONObject(0).getString("name"))
        assertEquals("app.n01", apps.getJSONObject(0).getString("package"))
        assertFalse(apps.getJSONObject(0).getBoolean("hidden"))
    }

    // arrange-receiver#AC-2
    @Test
    fun `AC-2 with an arrangement it answers the shown apps in order, then the hidden ones`() {
        store.save(Arrangement(order = listOf("app.n03"), hidden = listOf("app.n02")))

        val json = decode(requests.get().data)
        val apps = json.getJSONArray("apps")
        val packages = (0 until apps.length()).map { apps.getJSONObject(it).getString("package") }
        val hidden = (0 until apps.length()).map { apps.getJSONObject(it).getBoolean("hidden") }

        assertEquals(listOf("app.n03", "app.n01", "app.n04", "app.n02"), packages)
        assertEquals(listOf(false, false, false, true), hidden)
        assertEquals(1L, json.getLong("version"))
        assertTrue(json.getBoolean("arranged"))
    }

    // arrange-receiver#AC-3
    @Test
    fun `AC-3 a send with the version read saves it and answers the new version`() {
        val answer = requests.set(encode(listOf("app.n04", "app.n01"), listOf("app.n03")), version = 0, replace = false)

        assertEquals(ArrangeRequests.DONE, answer.code)
        assertEquals(1L, decode(answer.data).getLong("version"))
        assertEquals(listOf("app.n04", "app.n01"), store.load()!!.order)
        assertEquals(listOf("app.n03"), store.load()!!.hidden)
    }

    // arrange-receiver#AC-4
    @Test
    fun `AC-4 a send with an old version saves nothing and answers the current version`() {
        store.save(Arrangement(order = listOf("app.n02"), hidden = emptyList()))
        store.save(Arrangement(order = listOf("app.n03"), hidden = emptyList()))

        val answer = requests.set(encode(listOf("app.n04"), emptyList()), version = 1, replace = false)

        assertEquals(ArrangeRequests.CHANGED, answer.code)
        assertEquals(2L, decode(answer.data).getLong("version"))
        assertEquals(listOf("app.n03"), store.load()!!.order)
    }

    // arrange-receiver#AC-4
    @Test
    fun `AC-4 with replace an old version is saved anyway`() {
        store.save(Arrangement(order = listOf("app.n02"), hidden = emptyList()))

        val answer = requests.set(encode(listOf("app.n04"), emptyList()), version = 0, replace = true)

        assertEquals(ArrangeRequests.DONE, answer.code)
        assertEquals(listOf("app.n04"), store.load()!!.order)
    }

    // arrange-receiver#AC-5
    @Test
    fun `AC-5 something that cannot be read is refused with the reason`() {
        for (bad in listOf(null, "not base64!", Base64.getEncoder().encodeToString("{ not json".toByteArray()))) {
            val answer = requests.set(bad, version = 0, replace = false)

            assertEquals(ArrangeRequests.REFUSED, answer.code)
            assertTrue(decode(answer.data).getString("error").isNotEmpty())
        }
        assertNull(store.load())
    }

    // arrange-receiver#AC-5
    @Test
    fun `AC-5 an arrangement that hides every installed app is refused`() {
        val answer = requests.set(encode(emptyList(), listOf("app.n01", "app.n02", "app.n03", "app.n04")), version = 0, replace = false)

        assertEquals(ArrangeRequests.REFUSED, answer.code)
        assertNull(store.load())
    }
}
