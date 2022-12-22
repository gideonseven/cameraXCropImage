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

    var mTop = 0f
    var mBot = 0f
    var mLeft = 0f
    var mRight = 0f
    var mWidth = 0f
    var mHeight = 0f


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

            val leftDraw = 0f
            val rightDraw = width.toFloat()
//            val topDraw = ctx.resources.getDimension(R.dimen.margin_big_card_camera_guideline) + actionBarHeight + 500
            val topDraw =
                height.toFloat() / 2 - ctx.resources.getDimension(R.dimen.card_height_half)
//            val bottomDraw = (height - ctx.resources.getDimension(R.dimen.camera_control_height) - ctx.resources.getDimension(R.dimen.margin_small_card_camera_guideline))
            val bottomDraw =
                height.toFloat() / 2 + ctx.resources.getDimension(R.dimen.card_height_half)

            cardRect.set(
                RectF(
                    leftDraw,
                    topDraw,
                    rightDraw,
                    bottomDraw
                )
            )

            mLeft = leftDraw
            mTop = topDraw
            mRight = rightDraw
            mBot = bottomDraw

            mWidth = cardRect.width()
            mHeight = cardRect.height()

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
                "Pastikan gambar ada pada area frame",
                (width / 2).toFloat(),
                topDraw - ctx.resources.getDimension(R.dimen._60dp) - ctx.resources.getDimension(R.dimen._16dp),
                textPaint
            )

            // draw 2nd text on top of border
            canvas.drawText(
                "dan dapat terbaca dengan jelas.",
                (width / 2).toFloat(),
                topDraw - ctx.resources.getDimension(R.dimen._60dp),
                textPaint
            )

            bitmap.recycle()
        }
    }
}