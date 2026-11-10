package quiet.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The PC's arrangement script, over ADB (docs/specs/arrange-receiver.md). */
class ArrangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = TODO("arrange-receiver#AC-2")
}
