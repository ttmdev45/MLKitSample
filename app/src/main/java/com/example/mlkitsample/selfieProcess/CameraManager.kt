package com.example.mlkitsample.selfieProcess

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.mlkitsample.FaceDetectionListener
import com.example.mlkitsample.data.repository.CameraRepositoryImpl
import com.example.mlkitsample.databinding.FragmentSelfieBinding
import com.example.mlkitsample.domain.model.FaceAction
import com.example.mlkitsample.domain.usecase.LivenessUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs


class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val binding: FragmentSelfieBinding,
    private val viewModel: SelfieViewModel,
    private val faceDetectionListener: FaceDetectionListener
) {
    private val useCase = LivenessUseCase(CameraRepositoryImpl())
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Face action tracking
    private var currentAction: FaceAction? = null
    private var actionCompleted = false

    private var completedActions = 0
    private var requiredAction = 3

    // Executor for background tasks
    var cameraExecutor: ExecutorService? = null//Executors.newSingleThreadExecutor()

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraExecutor = Executors.newSingleThreadExecutor()  // Initialize background thread
            cameraProvider?.unbindAll()
           // cameraExecutor.shutdown()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.previewView.surfaceProvider
            }



          //  cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

            // Image analysis for face detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    cameraExecutor?.let { executor -> it.setAnalyzer(executor, ::analyzeImage) }
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider?.unbindAll()
              //  cameraExecutor.shutdown()
                cameraProvider?.bindToLifecycle(lifecycleOwner,
                    cameraSelector, preview, imageAnalyzer,imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

           // requestNextFaceAction()

        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    checkLiveness(face)
                    faceDetectionListener.onFaceDetected(face)
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
         Log.d("main act","check actionCompleted status $actionCompleted  $currentAction")
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
    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    fun stopCamera() {
        cameraProvider?.unbindAll() // Unbind all camera use cases
        cameraExecutor?.shutdown()
        imageCapture = null // Release image capture resources
        cameraProvider = null // Release camera provider resources
    }
    @SuppressLint("SetTextI18n")
    private fun onActionCompleted(message: String){

        faceDetectionListener.onActionCompleted(message)
        actionCompleted = true
        completedActions++

        if(completedActions >= requiredAction)
        {
            Handler(Looper.getMainLooper()).postDelayed({
                faceDetectionListener.onDetectActionCompleted("Awesome! Your face was detected 🎉\nNow, let’s snap a photo 📸")
               // takePhoto() //Liveness check complete
            }, 3000)

        }else{
            Handler(Looper.getMainLooper()).postDelayed({
                requestNextFaceAction()
            }, 3000)
        }



    }

    fun requestNextFaceAction(){
        currentAction = useCase.requestNextAction()
        actionCompleted = false
        when (currentAction) {
            FaceAction.BLINK -> faceDetectionListener.onRequestMessage("Please blink your eyes.")
            FaceAction.SMILE -> faceDetectionListener.onRequestMessage("Please smile.")
            FaceAction.HEAD_SHAKE -> faceDetectionListener.onRequestMessage("Please shake your head.")

            else -> {

            }
        }
    }

    fun resetLiveness() {
        completedActions = 0
        currentAction = null
        actionCompleted = false
    }
    private fun onActionWrong(message: String) {

        faceDetectionListener.onActionWrong(message)
    }
    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        viewModel.deleteCachedPhoto() // Delete last cached photo

        val photoFile = createPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val capturedUri = Uri.fromFile(photoFile)

                    viewModel.takePhoto(capturedUri)

                    if(capturedUri != null)
                    faceDetectionListener.onSuccessUpload("Photo capture succeed")
                    else faceDetectionListener.onFailUpload("Photo capture failed")


                }

                override fun onError(exception: ImageCaptureException) {
                   // Timber.tag("CameraX").e(exception, "Photo capture failed: ${exception.message}")
                    faceDetectionListener.onFailUpload("Photo capture failed : ${exception.message}")
                }
            })
    }

    private fun createPhotoFile(): File {
        return File(
            context.externalCacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
    }
}



