package quiet.launcher.spike;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Spike stand-in for the quiet home screen: a label and one button that searches Google TV. */
public class MainActivity extends Activity {
    static final String PREFS = "spike";

    static String mode(Context c) {
        return c.getSharedPreferences(PREFS, MODE_PRIVATE).getString("mode", "window");
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        applyMode(getIntent());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(20, 24, 28));

        TextView label = new TextView(this);
        label.setText("QUIET SPIKE HOME");
        label.setTextColor(Color.WHITE);
        label.setTextSize(48);
        label.setGravity(Gravity.CENTER);
        root.addView(label);

        Button search = new Button(this);
        search.setText("Search: Severance");
        search.setOnClickListener(v -> search("Severance"));
        root.addView(search);

        setContentView(root);
        search.requestFocus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        applyMode(intent);
        String q = intent.getStringExtra("query");
        if (q != null) search(q);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(HomeWatcher.TAG, "MainActivity resumed, mode=" + mode(this));
    }

    private void applyMode(Intent intent) {
        String m = intent.getStringExtra("mode");
        if (m != null) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("mode", m).commit();
            Log.i(HomeWatcher.TAG, "mode set to " + m);
        }
    }

    private void search(String query) {
        Intent i = new Intent("android.search.action.GLOBAL_SEARCH");
        i.setComponent(new ComponentName("com.google.android.katniss",
                "com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline"));
        i.putExtra("query", query);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.i(HomeWatcher.TAG, "searching for " + query);
        startActivity(i);
    }
}
