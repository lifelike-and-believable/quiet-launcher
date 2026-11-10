package quiet.launcher

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** The quiet home screen. STUB (tests-first commit): a one-column grid of stub labels. */
class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val grid = RecyclerView(this).apply {
            id = R.id.app_grid
            layoutManager = GridLayoutManager(this@HomeActivity, 1)
        }
        val empty = TextView(this).apply { id = R.id.empty_message; text = "Stub" }
        setContentView(FrameLayout(this).apply { addView(grid); addView(empty) })
    }
}
