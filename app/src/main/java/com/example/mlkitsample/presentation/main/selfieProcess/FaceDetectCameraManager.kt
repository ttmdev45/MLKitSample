package com.example.mlkitsample.presentation.main.selfieProcess

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.mlkitsample.data.repository.CameraRepositoryImpl
import com.example.mlkitsample.databinding.FragmentSelfieBinding
import com.example.mlkitsample.domain.model.FaceAction
import com.example.mlkitsample.domain.usecase.LivenessUseCase
import com.example.mlkitsample.presentation.main.FaceDetectionListener
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

class FaceDetectCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val binding: FragmentSelfieBinding,
    private val viewModel: SelfieViewModel,
    private val faceDetectionListener: FaceDetectionListener
) : DefaultLifecycleObserver{
    private val useCase = LivenessUseCase(CameraRepositoryImpl())
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    internal var cameraExecutor: ExecutorService? = null
    private var currentAction: FaceAction? = null
    private var actionCompleted = false
    private var completedActions = 0
    private val requiredAction = 3

    private var currentStep = 1


    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    fun startCamera() {
       // lifecycleOwner.lifecycle.addObserver(this)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraExecutor = Executors.newSingleThreadExecutor()
            cameraProvider?.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    cameraExecutor?.let { executor -> it.setAnalyzer(executor, ::analyzeImage) }
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }
    private var isFaceDetectionActive = false
    fun startFaceDetection() {
        isFaceDetectionActive = true
    }

    fun stopFaceDetection() {
        isFaceDetectionActive = false
    }
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {

        if (!isFaceDetectionActive) {
            imageProxy.close()
            return
        }


        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                handleFaceFlow(faces)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    private fun handleFaceFlow(faces: List<Face>) {
        when (currentStep) {
            1 -> {
                if (faces.isEmpty()) {
                    faceDetectionListener.onNoFaceDetected("No face detected. Please move into the frame.")
                } else {
                    currentStep++
                }
            }
            2 -> {
                if (faces.isEmpty()) {
                    currentStep = 1
                    // faceDetectionListener.onNoFaceDetected("No face detected. Please move into the frame.")
                } else if (faces.size > 1) {
                    faceDetectionListener.onMultipleFaceDetected("Multiple faces detected. Only one person allowed.")
                } else {
                    currentStep++
                }
            }
            3 -> {
                if (faces.isEmpty()) {
                    currentStep = 1
                    //  faceDetectionListener.onNoFaceDetected("No face detected. Please move into the frame.")
                } else if (faces.size > 1) {
                    currentStep = 2
                } else {
                    val face = faces.first()
                    if (!isFaceCentered(face)) {
                        faceDetectionListener.onFaceNotCentered("Please center your face.")
                    } else {
                        currentStep++
                    }
                }
            }
            4 -> {
                if (faces.isEmpty()) {
                    currentStep = 1
                    // faceDetectionListener.onNoFaceDetected("No face detected. Please move into the frame.")
                }  else if (faces.size > 1) {
                    currentStep = 2
                }else {
                    val face = faces.first()
                    if (!isFaceCentered(face)) {
                        currentStep = 3
                    } else if (isFaceTooFar(face)) {

                        val targetDistance = "Please come to 1 meter" // You can change 1 meter to a target distance
                        faceDetectionListener.onTooFarFaceDetected("You are too far. $targetDistance")

                    }else {
                        currentStep++
                    }

                }
            }
            5 -> {
                if (faces.isEmpty()) {
                    currentStep = 1
                }else if (faces.size > 1) {
                    currentStep = 2
                } else {
                    val face = faces.first()

                    if (!isFaceCentered(face)) {
                        currentStep = 3
                    } else if (isFaceTooFar(face)) {
                        currentStep = 4
                    }else {

                        goToRandomAction(faces.first())
                    }


                }
            }
        }
    }


    private fun isFaceCentered(face: Face): Boolean {
        val previewCenterX = binding.previewView.width / 2
        val previewCenterY = binding.previewView.height / 2
        val faceCenterX = face.boundingBox.centerX()
        val faceCenterY = face.boundingBox.centerY()
        return abs(previewCenterX - faceCenterX) < 150 && abs(previewCenterY - faceCenterY) < 200
    }

    private fun isFaceTooFar(face: Face): Boolean {
        val distanceMeters = estimateDistanceFromFace(face)
       // val formattedDistance = String.format("%.2f", distanceMeters)
      //  Log.d("facedetect", "estimated distance = $formattedDistance meters")

        //too far ➔ distance > 1.5m
        //
        //too close ➔ distance < 0.3m
        //
        //perfect ➔ in between
        return distanceMeters > 1.5f
    }

    private fun estimateDistanceFromFace(face: Face): Float {
        val area = face.boundingBox.width() * face.boundingBox.height()
        val referenceArea = 50000f // area when face is 0.5 meters away
        val referenceDistance = 0.5f // meters

        return referenceArea / area * referenceDistance
    }

    fun requestNextFaceAction() {
        currentAction = useCase.requestNextAction()
        actionCompleted = false


    }

    private fun goToRandomAction(face: Face) {
        if (actionCompleted) return

        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
        val smilingProb = face.smilingProbability ?: 0.0f
        val headYaw = face.headEulerAngleY  // Left/Right
        val headPitch = face.headEulerAngleX // Up/Down
        val faceRotationY = face.headEulerAngleY // Left/right turn
        val faceRotationZ = face.headEulerAngleZ // Tilt

        val yaw = face.headEulerAngleY
        val roll = face.headEulerAngleZ


        when (currentAction) {

            FaceAction.SMILE -> {
                if (smilingProb > 0.7f) {
                    onActionCompleted("Detected: Smile")
                } else if ((leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) || abs(headYaw) > 15) {
                  //  onActionWrong("Wrong action! Expected: Smile")



                    faceDetectionListener.onRequestMessage("Please smile 😊.")


                }
            }
            FaceAction.HEAD_SHAKE -> {
                // Detect head shake (left ↔ right)
                if (abs(headYaw) > 15) {
                    onActionCompleted("Detected: Head shake (left ↔ right)")
                } else {
                  //  onActionWrong("Wrong action! Expected: Head shake (left ↔ right)")
                    faceDetectionListener.onRequestMessage("Please shake your head left ↔ right 🙆.")

                }


            }
            FaceAction.BLINK -> {
            // Detect Blink
                if(leftEyeOpenProb == 0.0f || rightEyeOpenProb == 0.0f){
                    onActionCompleted("Detected: Blink")
                } else {
                   // onActionWrong("Wrong action! Expected: Blink")
                    faceDetectionListener.onRequestMessage("Please blink your eyes 😉.")
                }
            }


            else -> {
                if (smilingProb > 0.7f) {
                    onActionCompleted("Detected: Smile")
                } else if ((leftEyeOpenProb < 0.4f && rightEyeOpenProb < 0.4f) || abs(headYaw) > 15) {
                  //  onActionWrong("Wrong action! Expected: Smile")
                    faceDetectionListener.onRequestMessage("Please smile 😊.")
                }

            }
        }




    }

    private fun onActionCompleted(message: String) {
        faceDetectionListener.onActionCompleted(message)
        actionCompleted = true
        completedActions++

        if (completedActions >= requiredAction) {
            Handler(Looper.getMainLooper()).postDelayed({
                faceDetectionListener.onDetectActionCompleted("All done! Let's take a photo.")

            }, 2000)
        }else{
            Handler(Looper.getMainLooper()).postDelayed({
                requestNextFaceAction()
            }, 3000)
        }
    }

    private fun onActionWrong(message: String) {
        faceDetectionListener.onActionWrong(message)
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        viewModel.deleteCachedPhoto()

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



                   // faceDetectionListener.onSuccessUpload("Photo captured ✅")
                    // stopFaceDetection()
                }

                override fun onError(exception: ImageCaptureException) {
                    faceDetectionListener.onFailUpload("Photo capture failed: ${exception.message}")
                    //  stopFaceDetection()
                }
            }
        )
    }

    private fun createPhotoFile(): File {
        return File(
            context.externalCacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        imageCapture = null
        cameraProvider = null
         stopFaceDetection()
    }

    fun resetLiveness() {
        completedActions = 0
        currentAction = null
        actionCompleted = false
        currentStep = 1
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cameraExecutor?.shutdown()
        super.onDestroy(owner)

    }
}