package com.sametyigit.stepscape.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sametyigit.stepscape.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Int = 0
    private var maxProgress: Int = 100
    private var strokeWidth: Float = 36f // dp converted to px

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    init {
        // Convert 18dp to pixels for stroke width
        strokeWidth = 18f * resources.displayMetrics.density
        
        trackPaint.strokeWidth = strokeWidth
        trackPaint.color = ContextCompat.getColor(context, R.color.progress_track)
        
        progressPaint.strokeWidth = strokeWidth
        progressPaint.color = ContextCompat.getColor(context, R.color.orange_primary)
    }

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, maxProgress)
        invalidate()
    }

    fun getProgress(): Int = progress

    fun setMax(max: Int) {
        maxProgress = max
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - (strokeWidth / 2f)

        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        canvas.drawArc(rectF, 0f, 360f, false, trackPaint)

        val sweepAngle = (progress.toFloat() / maxProgress) * 360f
        if (sweepAngle > 0) {
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (209 * resources.displayMetrics.density).toInt()
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, heightSize)
            else -> desiredSize
        }

        setMeasuredDimension(width, height)
    }
}
