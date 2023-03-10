package com.example.cameraxcropimage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat


/**
 * Created by gideon on 08 December 2022
 * gideon@cicil.co.id
 * https://www.cicil.co.id/
 */
class VerticalCardViewfinder @JvmOverloads constructor(
    private val ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(ctx, attrs, defStyleAttr) {

    private val cardRect = RectF()
    private val outerPaint = Paint()
    private val innerPaint = Paint()
    private val strokePaint = Paint()
    private val textPaint = Paint()
    private val radius = ResourcesCompat.getFloat(ctx.resources, R.dimen.guideline_round_radius)

    private var actionBarHeight = ctx.resources.getDimension(R.dimen.default_action_bar_height)
    private var mShow = true

    private var guidelineTop = 0f
    private var guidelineBot = 0f
    private var guidelineLeft = 0f
    private var guidelineRight = 0f
    private var guidelineWidth = 0f
    private var guidelineHeight = 0f


    init {
        outerPaint.color = Color.BLACK // mention any background color
        outerPaint.alpha = ctx.resources.getInteger(R.integer.outer_background_alpha)
        outerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        outerPaint.isAntiAlias = true

        innerPaint.color = Color.TRANSPARENT
        innerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        innerPaint.isAntiAlias = true

        strokePaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth =
            ResourcesCompat.getFloat(ctx.resources, R.dimen.guideline_stroke_width)

        textPaint.color = Color.WHITE
        textPaint.textSize = ctx.resources.getDimension(R.dimen.text_size_normal)
        textPaint.textAlign = Paint.Align.CENTER

        val tv = TypedValue()
        if (ctx.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, tv, true)) {
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, ctx.resources.displayMetrics)
                    .toFloat()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mShow) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val auxCanvas = Canvas(bitmap)

            //cr 80, width 86mm x height 54mm -  ratio 43:27
            // 43 width
            // 27 height
            val ratioWidth = 43
            val ratioHeight = 27

            // measurement
            val leftDraw = 0f
            val rightDraw = width.toFloat()
            val topDraw = (height.toFloat() - (width.toFloat() * ratioHeight / ratioWidth)) / 2
            val bottomDraw = height.toFloat() / 2 + topDraw / 2

            cardRect.set(
                RectF(
                    leftDraw,
                    topDraw,
                    rightDraw,
                    bottomDraw
                )
            )

            guidelineLeft = leftDraw
            guidelineTop = topDraw
            guidelineRight = rightDraw
            guidelineBot = bottomDraw

            guidelineWidth = guidelineRight - guidelineLeft
            guidelineHeight = guidelineBot - guidelineTop

            // draw black transparent overlay
            auxCanvas.drawRect(0.0f, 0.0f, width.toFloat(), height.toFloat(), outerPaint)

            // draw view inside border line
            auxCanvas.drawRoundRect(cardRect, radius, radius, innerPaint)

            // draw border line
//            auxCanvas.drawRoundRect(cardRect, radius, radius, strokePaint)

            // draw all together  on canvas
            canvas.drawBitmap(bitmap, 0f, 0f, strokePaint)

            // draw 1st text on top of border
            canvas.drawText(
                "Please make sure the image",
                (width / 2).toFloat(),
                topDraw - ctx.resources.getDimension(R.dimen._60dp) - ctx.resources.getDimension(R.dimen._16dp),
                textPaint
            )

            // draw 2nd text on top of border
            canvas.drawText(
                "is clear, and readable",
                (width / 2).toFloat(),
                topDraw - ctx.resources.getDimension(R.dimen._60dp),
                textPaint
            )

            bitmap.recycle()
        }
    }

    fun getGuidelineWidth() = guidelineWidth.toInt()
    fun getGuidelineHeight() = guidelineHeight.toInt()
    fun getGuidelineLeft() = guidelineLeft.toInt()
    fun getGuidelineTop() = guidelineTop.toInt()
}