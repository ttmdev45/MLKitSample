package com.example.mlkitsample.presentation.main

//import android.graphics.BitmapFactory
//import android.os.Bundle
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.ImageProxy
//import androidx.core.content.ContextCompat
//import com.example.mlkitsample.databinding.ActivityMainBinding
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.face.FaceDetection
//import com.google.mlkit.vision.face.FaceDetectorOptions
//
//class MainActivity: AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val viewModel: MainViewModel by viewModels()
//
//    private val faceDetector by lazy {
//        FaceDetection.getClient(
//            FaceDetectorOptions.Builder()
//                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//                .enableTracking()
//                .build()
//        )
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        observeViewModel()
//
//        // You would normally start camera & permission flow here...
//        viewModel.requestNextFaceAction()
//    }
//
//    private fun observeViewModel() {
//        viewModel.livenessText.observe(this) { text ->
//            binding.livenessText.text = text
//        }
//
//        viewModel.livenessColor.observe(this) { colorRes ->
//            binding.livenessText.setTextColor(ContextCompat.getColor(this, colorRes))
//        }
//
//        viewModel.capturedImagePath.observe(this) { path ->
//            path?.let {
//                val bitmap = BitmapFactory.decodeFile(it)
//                binding.capturedImageView.setImageBitmap(bitmap)
//                binding.capturedImageView.visibility = android.view.View.VISIBLE
//                binding.livenessText.text = "Liveness check complete 🎉"
//            }
//        }
//    }
//
//    // Called from analyzer
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image ?: return
//        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//        faceDetector.process(image)
//            .addOnSuccessListener { faces ->
//                for (face in faces) {
//                    viewModel.checkLiveness(face)
//                }
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }
//    }
//}