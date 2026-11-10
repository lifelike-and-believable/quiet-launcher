package quiet.launcher

import android.content.Context

/** What the PC's arrangement script can ask (docs/specs/arrange-receiver.md, AC-2 to AC-6). */
class ArrangeRequests(private val context: Context) {

    /** A result code and base64 JSON, as `am broadcast` prints them. */
    data class Answer(val code: Int, val data: String)

    fun get(): Answer = TODO("arrange-receiver#AC-2")

    fun set(arrangement: String?, version: Long, replace: Boolean): Answer = TODO("arrange-receiver#AC-3")

    companion object {
        const val DONE = 1
        const val CHANGED = 2
        const val REFUSED = 3
    }
}
