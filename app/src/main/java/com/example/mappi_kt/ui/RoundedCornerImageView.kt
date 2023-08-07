package com.example.mappi_kt.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class RoundedCornerImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var cornerRadius = 20f

    init {
        scaleType = ScaleType.CENTER_CROP
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val clipPath = Path()
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
    }

}
