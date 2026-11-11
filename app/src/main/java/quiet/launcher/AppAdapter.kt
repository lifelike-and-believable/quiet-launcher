package quiet.launcher

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
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

    /**
     * Arrange mode's looks (docs/specs/arrange-mode.md): called after a tile is bound and after its
     * selection changes, with the tile, its artwork's frame, the app, and whether it is selected.
     */
    var decorate: ((tile: View, frame: FrameLayout, app: AppEntry, focused: Boolean) -> Unit)? = null

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
        private val frame = FrameLayout(parent.context)
        private val label = TextView(parent.context)
        private var app: AppEntry? = null
        private var pulse: Pulse? = null

        init {
            val dp = parent.resources.displayMetrics.density
            root.id = R.id.app_tile
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER_HORIZONTAL
            root.isFocusable = true
            root.isFocusableInTouchMode = true
            root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            root.setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt())
            // The selected artwork grows by 6% into this padding: do not clip it there.
            root.clipToPadding = false
            root.clipChildren = false

            art.id = R.id.app_art
            art.scaleType = ImageView.ScaleType.FIT_CENTER
            // Rounded corners like the outline, and 1 dp inside its outer edge, so the artwork's own
            // smoothed edge sits under the outline's black and never shows outside it as a grey line.
            art.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val inset = dp.toInt().coerceAtLeast(1)
                    outline.setRoundRect(inset, inset, view.width - inset, view.height - inset, CORNER_DP * dp - inset)
                }
            }
            art.clipToOutline = true
            art.background = tileBackground(dp, focused = false)
            // The outline is drawn on this frame, over the artwork and not clipped with it.
            frame.addView(art, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            root.addView(frame, LinearLayout.LayoutParams((150 * dp).toInt(), (84.5f * dp).toInt()))
            // Arrange mode marks hidden apps (design-arranging, Layout); never shown on the home screen.
            frame.addView(TextView(parent.context).apply {
                id = R.id.hidden_badge
                setText(R.string.hidden_badge)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply { cornerRadius = 4 * dp; setColor(Color.parseColor("#CC000000")) }
                setPadding((6 * dp).toInt(), (2 * dp).toInt(), (6 * dp).toInt(), (2 * dp).toInt())
                visibility = View.GONE
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (5 * dp).toInt(); topMargin = (5 * dp).toInt()
            })

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
                pulse?.cancel()
                pulse = null
                if (hasFocus) {
                    val (ring, white) = focusRing(dp)
                    frame.foreground = ring
                    if (animationsOn(root.context)) pulse = Pulse(white, frame).also { it.start() }
                } else {
                    frame.foreground = null
                }
                label.setTextColor(if (hasFocus) TEXT else MUTED)
                label.typeface = if (hasFocus) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                frame.animate().scaleX(if (hasFocus) 1.06f else 1f).scaleY(if (hasFocus) 1.06f else 1f).setDuration(120).start()
                app?.let { decorate?.invoke(root, frame, it, hasFocus) }
            }
        }

        fun bind(entry: AppEntry) {
            app = entry
            root.tag = entry.packageName
            root.contentDescription = entry.label
            art.setImageDrawable(entry.artwork)
            label.text = entry.label
            decorate?.invoke(root, frame, entry, root.hasFocus())
        }
    }

    companion object {
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
        const val CORNER_DP = 8f

        /**
         * A rounded stroke whose centre line is [insetDp] inside the artwork, with the radius that keeps
         * it concentric with the artwork's corners.
         */
        private fun ring(dp: Float, insetDp: Float, strokeDp: Float, color: Int) = GradientDrawable().apply {
            cornerRadius = (CORNER_DP - insetDp) * dp
            setStroke((strokeDp * dp).toInt(), color)
        }

        /** "Remove animations" sets the animator duration scale to 0. */
        fun animationsOn(context: android.content.Context): Boolean =
            ValueAnimator.areAnimatorsEnabled() &&
                android.provider.Settings.Global.getFloat(
                    context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
                ) != 0f

        fun focusRing(dp: Float): Pair<LayerDrawable, GradientDrawable> {
            // Both rings are drawn inside the artwork's rounded edge (CORNER_DP) and share its corner
            // centres: each ring's radius is the corner radius less its inset. Rings with their own
            // centres drift apart at the corners and let the artwork show through there.
            // Black from the edge to 5 dp in, with white from 1 to 3.5 dp: a white ring with a thin
            // black edge on each side, which shows on dark backgrounds and light banners alike.
            val black = ring(dp, insetDp = 2.5f, strokeDp = 5f, Color.BLACK)
            val white = ring(dp, insetDp = 2.25f, strokeDp = 2.5f, Color.WHITE)
            val ring = LayerDrawable(arrayOf(black, white)).apply {
                // A GradientDrawable draws its stroke inside its bounds, so a ring's bounds are inset
                // by its centre line less half its stroke: black from the edge, white from 1 dp in.
                setLayerInset(0, 0, 0, 0, 0)
                dp.toInt().let { setLayerInset(1, it, it, it, it) }
            }
            return ring to white
        }
    }
}

/**
 * The selected tile's outline: the white part brightens and dims about once a second for about
 * 5 seconds, then stays at full brightness (docs/design/home-screen.md, Selection; WCAG 2.2.2).
 * Driven by the main thread's clock rather than an animator, so it is the same on the TV and in tests.
 */
class Pulse(private val white: GradientDrawable, private val view: View) {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var startedAt = 0L

    private val step = object : Runnable {
        override fun run() {
            val elapsed = android.os.SystemClock.uptimeMillis() - startedAt
            if (elapsed >= DURATION_MS) {
                white.alpha = 255
            } else {
                // 255 at the start of each second, 110 at its middle.
                val phase = (elapsed % PERIOD_MS).toFloat() / PERIOD_MS
                val depth = (1 - kotlin.math.cos(phase * 2 * Math.PI).toFloat()) / 2
                white.alpha = (255 - depth * (255 - LOW)).toInt()
                handler.postDelayed(this, FRAME_MS)
            }
            view.invalidate()
        }
    }

    fun start() {
        startedAt = android.os.SystemClock.uptimeMillis()
        handler.post(step)
    }

    fun cancel() {
        handler.removeCallbacks(step)
        white.alpha = 255
    }

    private companion object {
        const val DURATION_MS = 5_000L
        const val PERIOD_MS = 1_000L
        const val FRAME_MS = 40L
        const val LOW = 110
    }
}
