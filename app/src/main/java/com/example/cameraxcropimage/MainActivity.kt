package com.example.cameraxcropimage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.example.cameraxcropimage.databinding.ActivityMainBinding
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    val PIC_CROP = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnOk.setOnClickListener {
            takePhoto()
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // setup output directory
        outputDirectory = getOutputDirectory()

        // set as cameraX single thread
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.iv.visibility = View.GONE
    }



    // check permission first
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // create / set save directory for file
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {
            File(
                it,
                resources.getString(R.string.app_name)
            ).apply { mkdirs() }
        }
        return if (mediaDir.exists())
            mediaDir else filesDir
    }


    // build cameraX with your desired attributes
    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .build()

            /**
             * by default cameraX capture and save image in landscape / wide view
             * and it is not match by what it appears on PreviewView (screen of
             * android phone)
             *
             * example :
             * android Screen (portrait position)
             *  -------
             *  - abc -
             *  - abc -
             *  -------
             *
             *  what cameraX captured and saved (landscape wide view) -
             *  without ViewPort
             *  ----------------
             *  -  abcdefghijk   -
             *  -  abcdefghijk   -
             *  -  abcdefghijk   -
             *  ----------------
             *
             * ViewPort come in handy to handle that, by cropping Rational using
             * ViewPortBuilder with PreviewView width and height,
             * so what it appear on android screen, is what image will be saved
             */
            val viewPort = ViewPort.Builder(
                Rational(
                    binding.previewView.width,
                    binding.previewView.height
                ), Surface.ROTATION_0
            )
                .setScaleType(ViewPort.FILL_CENTER)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture!!)
                .setViewPort(viewPort)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )

            } catch (exc: Exception) {
                Timber.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    // take your photo
    @SuppressLint("RestrictedApi")
    private fun takePhoto() {

        binding.btnOk.visibility = View.GONE


        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
//      val outputFileOption =   ImageCapture.OutputFileOptions.Builder(photoFile).build()

//      imageCapture.setViewPortCropRect(binding.borderView.getGuidelineRectF().toRect())
//        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageCapturedCallback() {
        imageCapture.takePicture(cameraExecutor, object: ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(image: ImageProxy) {
                val savedUri = Uri.fromFile(photoFile)

                Glide.with(applicationContext).load(image).into(binding.iv)

                Timber.e("=== GET IMAGE INFO ${image.imageInfo}")
                Timber.e("=== GET  IMAGE FORMAT ${image.format}")

        // Use the image, then make sure to close it.
//                val source : OutputTransform? = binding.previewView.outputTransform
//                val target = ImageProxyTransformFactory().getOutputTransform(image)
//
//
//
//                source?.let { src ->
//                    val coordinateTransform = CoordinateTransform(src, target)
//                    coordinateTransform.transform(getCorrectionMatrix(imageProxy = image, binding.previewView))
//                }
//                image.setCropRect(binding.borderView.getGuidelineRectF().toRect())
//                Timber.e("=== IMAGE WIDTH ${image.image?.width}")
//                Timber.e("=== IMAGE HEIGHT ${image.image?.height}")
//

//
                image.close()

//                finish()
            }
//
            override fun onError(exception: ImageCaptureException) {
                val errorType = exception.getImageCaptureError()
                Timber.e("=== ${errorType}")
            }
        })

        // Set up image capture listener, which is triggered after photo has
        // been taken
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }

//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val savedUri = Uri.fromFile(photoFile)
//                    goToPreviewView(savedUri, photoFile)
//                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//            })
    }

    //  do your thing when permission accepted / not
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // finish()
            }
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // don't forget to shut camera down
        cameraExecutor.shutdown()
    }

    // go to previewActivity
    private fun goToPreviewView(uri: Uri, photoFile: File) {

        // set to bitmap image, with rotation,
        // by default image will be rotated if saved to bitmap, so the image should be rotated back
        // to it's default angle
//        val bmImg = rotateBitmap(BitmapFactory.decodeFile(uri.path!!), isBackCamera = true)

        // do the cropping
//        val bytes = cropImage(
//            bitmap = bmImg,
//            guideline = binding.borderView
//        )

//        val croppedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // save cropped image
//        saveImageBitmap(croppedImage, photoFile)

        binding.tvProcessingImage.visibility = View.GONE
        binding.btnOk.visibility = View.VISIBLE

        // open preview activity with extra uri
        val intent = Intent(applicationContext, PreviewActivity::class.java)
        intent.putExtra("key", uri.toString())
        startActivity(intent)
    }

    fun getCorrectionMatrix(imageProxy: ImageProxy, previewView: PreviewView) : Matrix {
        val cropRect = imageProxy.cropRect
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()

        // A float array of the source vertices (crop rect) in clockwise order.
        val source = floatArrayOf(
            cropRect.left.toFloat(),
            cropRect.top.toFloat(),
            cropRect.right.toFloat(),
            cropRect.top.toFloat(),
            cropRect.right.toFloat(),
            cropRect.bottom.toFloat(),
            cropRect.left.toFloat(),
            cropRect.bottom.toFloat()
        )

        // A float array of the destination vertices in clockwise order.
        val destination = floatArrayOf(
            0f,
            0f,
            previewView.width.toFloat(),
            0f,
            previewView.width.toFloat(),
            previewView.height.toFloat(),
            0f,
            previewView.height.toFloat()
        )

        // The destination vertexes need to be shifted based on rotation degrees. The
        // rotation degree represents the clockwise rotation needed to correct the image.

        // Each vertex is represented by 2 float numbers in the vertices array.
        val vertexSize = 2
        // The destination needs to be shifted 1 vertex for every 90° rotation.
        val shiftOffset = rotationDegrees / 90 * vertexSize;
        val tempArray = destination.clone()
        for (toIndex in source.indices) {
            val fromIndex = (toIndex + shiftOffset) % source.size
            destination[toIndex] = tempArray[fromIndex]
        }
        matrix.setPolyToPoly(source, 0, destination, 0, 4)
        return matrix
    }

    companion object {
        private const val TAG = "AddTaskDialog"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}