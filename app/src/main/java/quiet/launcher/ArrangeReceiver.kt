package quiet.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The PC's arrangement script, over ADB (docs/specs/arrange-receiver.md). `adb shell am broadcast`
 * waits for the result and prints its code and data.
 */
class ArrangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requests = ArrangeRequests(context)
        val answer = when (intent.action) {
            ArrangeRequests.ACTION_GET -> requests.get()
            ArrangeRequests.ACTION_SET -> requests.set(
                intent.getStringExtra(EXTRA_ARRANGEMENT),
                intent.getLongExtra(EXTRA_VERSION, -1),
                intent.getBooleanExtra(EXTRA_REPLACE, false),
            )
            else -> return
        }
        setResult(answer.code, answer.data, null)
    }

    companion object {
        const val EXTRA_ARRANGEMENT = "arrangement"
        const val EXTRA_VERSION = "version"
        const val EXTRA_REPLACE = "replace"
    }
}
