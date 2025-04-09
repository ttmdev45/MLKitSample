package com.example.mlkitsample

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitsample.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    // Face action tracking
    private var currentAction: FaceAction? = null
    private var actionCompleted = false

    private var completedActions = 0
    private var requiredAction = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
       // setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Keep the screen on while running the camera

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Check and request camera permission
        requestCameraPermission()

        // Initialize background thread
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_LONG).show()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            // Use front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            // Image analysis for face detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ::analyzeImage)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this,
                    cameraSelector, preview, imageAnalyzer,imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

            requestNextFaceAction()

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    checkLiveness(face)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    private fun checkLiveness(face: Face) {
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
        val smilingProb = face.smilingProbability ?: 0.0f
        val headYaw = face.headEulerAngleY

        if (actionCompleted) return
       // Log.d("main act","check actionCompleted status $actionCompleted  $currentAction")
        when (currentAction) {
            FaceAction.BLINK -> {
                if (leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) {
                    onActionCompleted("Blink detected!")
                } else if (smilingProb > 0.7f || abs(headYaw) > 15) {
                    onActionWrong("Wrong action! Expected: Blink")
                }
            }
            FaceAction.SMILE -> {
                if (smilingProb > 0.7f) {
                    onActionCompleted("Smile detected!")
                } else if ((leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) || abs(headYaw) > 15) {
                    onActionWrong("Wrong action! Expected: Smile")
                }
            }
            FaceAction.HEAD_SHAKE -> {
                if (abs(headYaw) > 15) {
                    onActionCompleted("Head shake detected!")
                } else if ((leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) || smilingProb > 0.7f) {
                    onActionWrong("Wrong action! Expected: Head shake")
                }
            }
            else -> {}
        }
    }

//    private fun checkLiveness(face: Face) {
//        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
//        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
//        val smilingProb = face.smilingProbability ?: 0.0f
//        val headYaw = face.headEulerAngleY  // Left (-) & Right (+)
////            if (actionCompleted) return
////
////        when(currentAction){
////            FaceAction.BLINK -> {
////                if (leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) {
////                    onActionCompleted("Blink detected!")
////                }
////            }
////            FaceAction.SMILE -> {
////                if (smilingProb > 0.7f) {
////                    onActionCompleted("Smile detected!")
////                }
////            }
////            FaceAction.HEAD_SHAKE -> {
////                if (abs(headYaw) > 15) {
////                    onActionCompleted("Head shake detected!")
////                }
////            }
////            else -> {}
////
////        }
//        var livenessResult = ""
//
//        // Blink detection
//        if (leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) {
//            livenessResult += "Blink detected! "
//        }
//
//        // Smile detection
//        if (smilingProb > 0.7f) {
//            livenessResult += "Smiling detected! "
//        }
//
//        // Head shake detection (detect left-right movement)
//        if (abs(headYaw) > 15) { // If head angle is more than ±15 degrees
//            livenessResult += "Head Shake detected! "
//        }
//
//        if (livenessResult.isEmpty()) {
//            livenessResult = "Detecting..."
//        }
//
//        // Update UI
//        binding.livenessText.text = livenessResult
//        binding.livenessText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
//
//
//
//
//    }

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    @SuppressLint("SetTextI18n")
    private fun onActionCompleted(message: String){

        binding.livenessText.text = "$message ✅"
        binding.livenessText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))

        actionCompleted = true
        completedActions++

        if(completedActions >= requiredAction)
        {
            takePhoto()
        }else{
            Handler(Looper.getMainLooper()).postDelayed({
                requestNextFaceAction()
            }, 1500)
        }



    }

    private fun requestNextFaceAction(){
        currentAction = FaceAction.entries.random()
        actionCompleted = false
        when (currentAction) {
            FaceAction.BLINK -> binding.livenessText.text = "Please blink your eyes."
            FaceAction.SMILE -> binding.livenessText.text = "Please smile."
            FaceAction.HEAD_SHAKE -> binding.livenessText.text = "Please shake your head."
            else -> {

            }
        }
    }


    private fun onActionWrong(message: String) {
        binding.livenessText.text = message
        binding.livenessText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
    }

    private fun takePhoto(){
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            File(externalCacheDir,"captured_face.jpg")

        ).build()

        imageCapture.takePicture(outputOptions,ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val file = File(externalCacheDir,"captured_face.jpg")
                    showCapturedImage(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }


            })

    }

    private fun showCapturedImage(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        binding.capturedImageView.setImageBitmap(bitmap)
        binding.capturedImageView.visibility = View.VISIBLE
        binding.livenessText.text = "Liveness check complete 🎉"
    }
}
