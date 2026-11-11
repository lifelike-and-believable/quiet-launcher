package quiet.launcher

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** What the PC's arrangement script can ask (docs/specs/arrange-receiver.md, AC-2 to AC-6). */
class ArrangeRequests(private val context: Context) {

    /** A result code and base64 JSON, as `am broadcast` prints them. */
    data class Answer(val code: Int, val data: String)

    private val store = ArrangementStore(context)

    private fun installed(): List<AppEntry> =
        AppCatalog.visible(PackageSource(context.packageManager).read(), context.packageName)

    /** AC-2: the apps as the home screen and arrange mode show them, the version, and whether arranged. */
    fun get(): Answer {
        val stored = store.load()
        val arranged = AppCatalog.arranged(installed(), stored)
        val apps = JSONArray()
        for (app in arranged.shown) apps.put(app(app, hidden = false))
        for (app in arranged.hidden) apps.put(app(app, hidden = true))
        return answer(
            DONE,
            JSONObject().put("version", stored?.version ?: 0).put("arranged", stored != null).put("apps", apps),
        )
    }

    /** AC-3 to AC-5: save [arrangement] (base64 JSON) if [version] is still current, or [replace]. */
    fun set(arrangement: String?, version: Long, replace: Boolean): Answer {
        val parsed = try {
            val json = JSONObject(String(Base64.getDecoder().decode(arrangement ?: error("no arrangement was sent"))))
            Arrangement(order = json.getJSONArray("order").strings(), hidden = json.getJSONArray("hidden").strings())
        } catch (e: Exception) {
            return refused("the arrangement could not be read: ${e.message ?: e.javaClass.simpleName}")
        }
        val current = store.load()?.version ?: 0
        if (current != version && !replace) {
            return answer(CHANGED, JSONObject().put("version", current))
        }
        val apps = installed()
        if (apps.isNotEmpty() && apps.all { it.packageName in parsed.hidden }) {
            return refused("the arrangement hides every app; keep at least one on the home screen")
        }
        val saved = store.save(parsed)
        // AC-6: arrange mode closes and the home screen redraws, now.
        context.sendBroadcast(Intent(ACTION_CHANGED).setPackage(context.packageName))
        return answer(DONE, JSONObject().put("version", saved.version))
    }

    private fun app(app: AppEntry, hidden: Boolean) =
        JSONObject().put("name", app.label).put("package", app.packageName).put("hidden", hidden)

    private fun refused(reason: String) = answer(REFUSED, JSONObject().put("error", reason))

    private fun answer(code: Int, json: JSONObject) =
        Answer(code, Base64.getEncoder().encodeToString(json.toString().toByteArray()))

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    companion object {
        const val DONE = 1
        const val CHANGED = 2
        const val REFUSED = 3

        const val ACTION_GET = "quiet.launcher.action.GET_ARRANGEMENT"
        const val ACTION_SET = "quiet.launcher.action.SET_ARRANGEMENT"

        /** In the app only: the arrangement was changed from the PC. */
        const val ACTION_CHANGED = "quiet.launcher.action.ARRANGEMENT_CHANGED"
    }
}

/** Registers [receiver] for the arrangement changed from the PC (arrange-receiver#AC-6), in this app only. */
fun Context.onArrangementChanged(receiver: android.content.BroadcastReceiver) {
    val filter = android.content.IntentFilter(ArrangeRequests.ACTION_CHANGED)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(receiver, filter)
    }
}
