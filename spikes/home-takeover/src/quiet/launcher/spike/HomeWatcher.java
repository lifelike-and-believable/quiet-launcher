package quiet.launcher.spike;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Spike. Logs every window change and key event (tag QLSPIKE). Mode "window" (default) brings
 * MainActivity forward only when Google TV's home screen itself comes up, and lets its other
 * pages, such as search results, through. Mode "key" instead consumes the Home key. Set the
 * mode by starting MainActivity with --es mode window|key|off.
 */
public class HomeWatcher extends AccessibilityService {
    static final String TAG = "QLSPIKE";
    static final String LAUNCHERX = "com.google.android.apps.tv.launcherx";
    static final String GTV_HOME = LAUNCHERX + ".home.HomeActivity";
    static final String GTV_VANILLA_HOME = LAUNCHERX + ".home.VanillaModeHomeActivity";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String pkg = String.valueOf(event.getPackageName());
        String cls = String.valueOf(event.getClassName());
        String mode = MainActivity.mode(this);
        boolean isGtvHome = LAUNCHERX.equals(pkg)
                && (GTV_HOME.equals(cls) || GTV_VANILLA_HOME.equals(cls));
        Log.i(TAG, "window pkg=" + pkg + " cls=" + cls + " mode=" + mode + " gtvHome=" + isGtvHome);
        if (isGtvHome && "window".equals(mode)) {
            Log.i(TAG, "taking over from Google TV home");
            showOurs();
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        Log.i(TAG, "key " + KeyEvent.keyCodeToString(code) + " action=" + event.getAction());
        if (code == KeyEvent.KEYCODE_HOME && "key".equals(MainActivity.mode(this))) {
            if (event.getAction() == KeyEvent.ACTION_UP) showOurs();
            return true;
        }
        return false;
    }

    private void showOurs() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(i);
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "service connected, mode=" + MainActivity.mode(this));
    }
}
