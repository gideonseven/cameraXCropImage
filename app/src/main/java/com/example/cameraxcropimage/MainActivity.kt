package com.example.cameraxcropimage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxcropimage.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

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
    }

    // take your photo
    @SuppressLint("RestrictedApi")
    private fun takePhoto() {

        binding.tvProcessingImage.visibility = View.VISIBLE
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
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    onImageCaptured(savedUri, photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            /**
             * use viewport to match previewview and captured image size
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
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
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

    // do other thing after object has been captured
    private fun onImageCaptured(uri: Uri, photoFile: File) {

        // set to bitmap image, with rotation,
        // by default image will be rotated if saved to bitmap, so the image should be rotated back
        // to it's default angle
        val bmImg = rotateBitmap(BitmapFactory.decodeFile(uri.path!!), isBackCamera = true)
        println("IMAGE SAVED WIDTH ${bmImg.width}")
        println("IMAGE SAVED HEIGHT ${bmImg.height}")

        // do the cropping
        val bytes = cropImage(
            bitmap = bmImg,
            guideline = binding.borderView
        )

        val croppedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // save cropped image
        saveImageBitmap(croppedImage, photoFile)

        binding.tvProcessingImage.visibility = View.GONE
        binding.btnOk.visibility = View.VISIBLE

        // open preview activity with extra uri
        val intent = Intent(applicationContext, PreviewActivity::class.java)
        intent.putExtra("key", uri.toString())
        startActivity(intent)
    }

    companion object {
        private const val TAG = "AddTaskDialog"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}