package com.example.cameraxcropimage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxcropimage.databinding.ActivityCameraBinding
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
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

    // create save directory for file
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

    // take your photo
    @SuppressLint("RestrictedApi")
    private fun takePhoto() {
        // show loading
        showLoading(true)

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
                    Timber.e(exc, "Photo capture failed: %s", exc.message)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    //get saved uri
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Timber.d(msg)
                    //go to preview activity
                    goToPreviewActivity(savedUri, photoFile)
                }
            })
    }


    // check permission first
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
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
                Timber.e(exc, "Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // crop image and go to preview activity
    private fun goToPreviewActivity(uri: Uri, photoFile: File) {
        uri.path?.let { path ->

            //to avoid heavy task related BITMAP on MainThread
            //assign to IO
            CoroutineScope(Dispatchers.IO).launch {
                //1. rotate bitmap
                val bmImg = rotateBitmap(BitmapFactory.decodeFile(path), isBackCamera = true)
                println("IMAGE SAVED WIDTH ${bmImg.width}")
                println("IMAGE SAVED HEIGHT ${bmImg.height}")

                //2. convert to bytes, applying cropping
                val bytes = cropImage(bmImg, binding.borderView)

                //3. decode to bitmap
                val croppedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                //4. rewrite photoFile
                saveImageBitmap(croppedImage, photoFile)

                launch(Dispatchers.Main) {
                    // open preview activity with extra uri
                    val intent = Intent(applicationContext, PreviewActivity::class.java)
                    intent.putExtra("key", uri.toString())
                    startActivity(intent)

                    // hide loading
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isShown: Boolean) {
        binding.tvProcessingImage.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}