package com.sametyigit.stepscape.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.sametyigit.stepscape.R

class NeonShadowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val SHADOW_PADDING_DP = 16f

        private const val TEAL_COLOR = 0xFFBCE9E6.toInt()
        private const val TEAL_BLUR_DP = 20f
        private const val TEAL_DX_DP = -4f
        private const val TEAL_DY_DP = -4f

        private const val PINK_COLOR = 0xFFEFCAEE.toInt()
        private const val PINK_BLUR_DP = 16f
        private const val PINK_DX_DP = 4f
        private const val PINK_DY_DP = 4f
    }

    private val density = resources.displayMetrics.density
    private val shadowPaddingPx = SHADOW_PADDING_DP * density
    private var cornerRadiusPx: Float

    private val tealShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pinkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val cardRect = RectF()

    init {
        // Read custom attribute
        val a = context.obtainStyledAttributes(attrs, R.styleable.NeonShadowLayout)
        cornerRadiusPx = a.getDimension(
            R.styleable.NeonShadowLayout_neonCornerRadius,
            32f * density
        )
        a.recycle()

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)

        tealShadowPaint.style = Paint.Style.FILL
        tealShadowPaint.color = Color.WHITE
        tealShadowPaint.setShadowLayer(
            TEAL_BLUR_DP * density,
            TEAL_DX_DP * density,
            TEAL_DY_DP * density,
            TEAL_COLOR
        )

        pinkShadowPaint.style = Paint.Style.FILL
        pinkShadowPaint.color = Color.WHITE
        pinkShadowPaint.setShadowLayer(
            PINK_BLUR_DP * density,
            PINK_DX_DP * density,
            PINK_DY_DP * density,
            PINK_COLOR
        )

        whiteFillPaint.style = Paint.Style.FILL
        whiteFillPaint.color = Color.WHITE

        val pad = shadowPaddingPx.toInt()
        setPadding(pad, pad, pad, pad)
    }

    override fun onDraw(canvas: Canvas) {
        val pad = shadowPaddingPx
        cardRect.set(pad, pad, width.toFloat() - pad, height.toFloat() - pad)

        canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, tealShadowPaint)
        canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, pinkShadowPaint)

        canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, whiteFillPaint)

        super.onDraw(canvas)
    }
}
