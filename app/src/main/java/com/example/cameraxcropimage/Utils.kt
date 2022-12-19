package com.example.cameraxcropimage

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.view.View
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

fun cropImage(bitmap: Bitmap, containerImage: View, containerOverlay: View): ByteArray {
    val heightOriginal = containerImage.height
    val widthOriginal = containerImage.width
    val heightFrame = containerOverlay.height
    val widthFrame = containerOverlay.width
    val leftFrame = containerOverlay.left
    val topFrame = containerOverlay.top
    val heightReal = bitmap.height
    val widthReal = bitmap.width
    val widthFinal = widthFrame * widthReal / widthOriginal
    val heightFinal = heightFrame * heightReal / heightOriginal
    val leftFinal = leftFrame * widthReal / widthOriginal
    val topFinal = topFrame * heightReal / heightOriginal
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

fun cropImage(bitmap: Bitmap, containerImage: View, width: Int, height: Int, left: Int, top: Int): ByteArray {
    val heightOriginal = containerImage.height
    val widthOriginal = containerImage.width
    val heightReal = bitmap.height
    val widthReal = bitmap.width
    val widthFinal = width * widthReal / widthOriginal
    val heightFinal = height * heightReal / heightOriginal
    val leftFinal = left * widthReal / widthOriginal
    val topFinal = top * heightReal / heightOriginal
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
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}