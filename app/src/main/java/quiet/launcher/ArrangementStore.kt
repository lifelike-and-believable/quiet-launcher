package quiet.launcher

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Keeps the arrangement in the app's own storage (docs/specs/arranging-store.md, AC-5). */
class ArrangementStore(private val context: Context) {

    private val file get() = File(context.filesDir, FILE_NAME)

    /** The stored arrangement, or null if there is none or it cannot be read. */
    fun load(): Arrangement? {
        val f = file
        if (!f.exists()) return null
        return try {
            val json = JSONObject(f.readText())
            Arrangement(
                order = json.getJSONArray("order").strings(),
                hidden = json.getJSONArray("hidden").strings(),
                version = json.getLong("version"),
            )
        } catch (e: Exception) {
            Log.w(TAG, "arrangement could not be read; using none", e)
            null
        }
    }

    /** Saves [arrangement] with the next version, and returns what was saved. */
    fun save(arrangement: Arrangement): Arrangement {
        val saved = arrangement.copy(version = (load()?.version ?: 0) + 1)
        val json = JSONObject()
            .put("version", saved.version)
            .put("order", JSONArray(saved.order))
            .put("hidden", JSONArray(saved.hidden))
        // Written beside the file and renamed over it, so a power cut leaves the old one or the new one.
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(json.toString())
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return saved
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    companion object {
        const val FILE_NAME = "arrangement.json"
        private const val TAG = "QuietLauncher"
    }
}
