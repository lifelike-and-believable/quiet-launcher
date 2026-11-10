package quiet.launcher

import android.content.Context

/** Keeps the arrangement in the app's own storage (docs/specs/arranging-store.md, AC-5). */
class ArrangementStore(private val context: Context) {

    fun load(): Arrangement? = TODO("arranging-store#AC-5")

    fun save(arrangement: Arrangement): Arrangement = TODO("arranging-store#AC-5")

    companion object {
        const val FILE_NAME = "arrangement.json"
    }
}
