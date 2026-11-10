package quiet.launcher

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The app tiles (docs/design/home-screen.md, Layout): the app's banner or icon (300 x 169 px at
 * 1080p, so 150 x 84.5 dp on the TV's 2x density) and its full name underneath, on up to two lines.
 */
class AppAdapter(private val onOpen: (AppEntry) -> Unit) : RecyclerView.Adapter<AppAdapter.Tile>() {

    private var apps: List<AppEntry> = emptyList()

    init {
        setHasStableIds(true)
    }

    fun submit(newApps: List<AppEntry>) {
        apps = newApps
        notifyDataSetChanged()
    }

    fun indexOf(packageName: String?): Int = apps.indexOfFirst { it.packageName == packageName }

    override fun getItemCount() = apps.size

    override fun getItemId(position: Int) = apps[position].packageName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Tile = Tile(parent)

    override fun onBindViewHolder(holder: Tile, position: Int) = holder.bind(apps[position])

    inner class Tile(parent: ViewGroup) : RecyclerView.ViewHolder(LinearLayout(parent.context)) {
        private val root = itemView as LinearLayout
        private val art = ImageView(parent.context)
        private val label = TextView(parent.context)
        private var app: AppEntry? = null

        init {
            val dp = parent.resources.displayMetrics.density
            root.id = R.id.app_tile
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER_HORIZONTAL
            root.isFocusable = true
            root.isFocusableInTouchMode = true
            root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            root.setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt())

            art.id = R.id.app_art
            art.scaleType = ImageView.ScaleType.FIT_CENTER
            art.background = tileBackground(dp, focused = false)
            root.addView(art, LinearLayout.LayoutParams((150 * dp).toInt(), (84.5f * dp).toInt()))

            label.id = R.id.app_label
            label.maxLines = 2
            label.ellipsize = TextUtils.TruncateAt.END
            label.gravity = Gravity.CENTER_HORIZONTAL
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            label.setTextColor(MUTED)
            root.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (5 * dp).toInt()
            })

            root.setOnClickListener { app?.let(onOpen) }
            root.setOnFocusChangeListener { _, hasFocus ->
                // Drawn over the artwork, so a light banner cannot hide it.
                art.foreground = if (hasFocus) focusRing(dp) else null
                label.setTextColor(if (hasFocus) TEXT else MUTED)
                label.typeface = if (hasFocus) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                art.animate().scaleX(if (hasFocus) 1.06f else 1f).scaleY(if (hasFocus) 1.06f else 1f).setDuration(120).start()
            }
        }

        fun bind(entry: AppEntry) {
            app = entry
            root.tag = entry.packageName
            root.contentDescription = entry.label
            art.setImageDrawable(entry.artwork)
            label.text = entry.label
        }
    }

    private companion object {
        val TEXT = Color.parseColor("#eef2f5")
        val MUTED = Color.parseColor("#a9b4bd")

        /** Behind the artwork: the tile colour, seen around an icon or a banner that does not fill it. */
        fun tileBackground(dp: Float, focused: Boolean): GradientDrawable = GradientDrawable().apply {
            cornerRadius = 8 * dp
            setColor(Color.parseColor("#1c2228"))
        }

        /**
         * Selected (docs/design/home-screen.md, Layout): a white outline with a black edge outside
         * it, so it shows on light and dark artwork alike.
         */
        fun focusRing(dp: Float): LayerDrawable {
            val black = GradientDrawable().apply {
                cornerRadius = 8 * dp
                setStroke((5 * dp).toInt(), Color.BLACK)
            }
            val white = GradientDrawable().apply {
                cornerRadius = 6 * dp
                setStroke((2.5f * dp).toInt(), Color.WHITE)
            }
            return LayerDrawable(arrayOf(black, white)).apply {
                val inset = (2.5f * dp).toInt()
                setLayerInset(1, inset, inset, inset, inset)
            }
        }
    }
}
