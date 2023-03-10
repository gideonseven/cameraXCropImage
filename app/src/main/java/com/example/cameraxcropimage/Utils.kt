package com.example.cameraxcropimage

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


/**
 * Created by gideon on 09 December 2022
 * gideon@cicil.co.id
 * https://www.cicil.co.id/
 */
fun rotateBitmap(bitmap: Bitmap, isBackCamera: Boolean = false): Bitmap {
    val matrix = Matrix()
    return if (isBackCamera) {
        matrix.postRotate(90f)
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    } else {
        matrix.postRotate(-90f)
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}

fun cropImage(
    bitmap: Bitmap,
    verticalCardViewFinder: VerticalCardViewfinder,
): ByteArray {
    val heightOriginal = verticalCardViewFinder.height
    val widthOriginal = verticalCardViewFinder.width
    val heightReal = bitmap.height
    val widthReal = bitmap.width

    val widthFinal = verticalCardViewFinder.getGuidelineWidth() * widthReal / widthOriginal
    val heightFinal = verticalCardViewFinder.getGuidelineHeight() * heightReal / heightOriginal
    val leftFinal = verticalCardViewFinder.getGuidelineLeft() * widthReal / widthOriginal
    val topFinal = verticalCardViewFinder.getGuidelineTop() * heightReal / heightOriginal


    println("widthOriginal $widthOriginal")
    println("heightOriginal $heightOriginal")
    println("widthReal $widthReal")
    println("heightReal $heightReal")
    println("widthFinal $widthFinal")
    println("heightFinal $heightFinal")
    println("leftFinal $leftFinal")
    println("topFinal $topFinal")

    val bitmapFinal = Bitmap.createBitmap(
        bitmap,
        leftFinal, topFinal, widthFinal, heightFinal
    )
    val stream = ByteArrayOutputStream()
    bitmapFinal.compress(
        Bitmap.CompressFormat.JPEG,
        100,
        stream
    ) //100 is the best quality possibe
    return stream.toByteArray()
}

fun saveImageBitmap(finalBitmap: Bitmap, file: File) {
    if (file.exists()) file.delete()
    try {
        val out = FileOutputStream(file)
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}