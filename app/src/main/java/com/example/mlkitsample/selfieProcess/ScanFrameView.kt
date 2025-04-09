package com.example.mlkitsample.selfieProcess

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ScanFrameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cornerRadius = 30f // Balanced corner radius

    private val strokeWidth = 10f // Visible stroke width
    private val cornerStrokeSize = 80f // **Equal size for all four corners**
    private val spacing = 30f // Space between red and blue strokes

    private val fullStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#902BAAD8")
        style = Paint.Style.STROKE
        strokeWidth = this@ScanFrameView.strokeWidth
    }

    private val cornerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#902BAAD8")
        style = Paint.Style.STROKE
        strokeWidth = this@ScanFrameView.strokeWidth
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val outerRect = RectF(
            strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth
        )

        val innerRect = RectF(
            outerRect.left + spacing, outerRect.top + spacing,
            outerRect.right - spacing, outerRect.bottom - spacing
        )

        // 1. Draw Red Corner Strokes (Equal on All 4 Corners)
        drawCornerStrokes(canvas, outerRect)

        // 2. Draw Full Blue Stroke (Inset with Spacing)
       // canvas.drawRoundRect(innerRect, cornerRadius, cornerRadius, fullStrokePaint)
    }

    private fun drawCornerStrokes(canvas: Canvas, rect: RectF) {
        val path = Path()

        // Top Left Corner
        path.moveTo(rect.left, rect.top + cornerStrokeSize)
        path.lineTo(rect.left, rect.top + cornerRadius)
        path.quadTo(rect.left, rect.top, rect.left + cornerRadius, rect.top)
        path.lineTo(rect.left + cornerStrokeSize, rect.top)

        // Top Right Corner
        path.moveTo(rect.right - cornerStrokeSize, rect.top)
        path.lineTo(rect.right - cornerRadius, rect.top)
        path.quadTo(rect.right, rect.top, rect.right, rect.top + cornerRadius)
        path.lineTo(rect.right, rect.top + cornerStrokeSize)

        // Bottom Left Corner
        path.moveTo(rect.left, rect.bottom - cornerStrokeSize)
        path.lineTo(rect.left, rect.bottom - cornerRadius)
        path.quadTo(rect.left, rect.bottom, rect.left + cornerRadius, rect.bottom)
        path.lineTo(rect.left + cornerStrokeSize, rect.bottom)

        // Bottom Right Corner
        path.moveTo(rect.right - cornerStrokeSize, rect.bottom)
        path.lineTo(rect.right - cornerRadius, rect.bottom)
        path.quadTo(rect.right, rect.bottom, rect.right, rect.bottom - cornerRadius)
        path.lineTo(rect.right, rect.bottom - cornerStrokeSize)

        canvas.drawPath(path, cornerStrokePaint)
    }
}

