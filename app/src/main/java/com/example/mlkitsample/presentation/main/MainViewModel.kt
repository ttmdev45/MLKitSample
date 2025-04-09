package com.example.mlkitsample.presentation.main

import android.app.Application
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mlkitsample.data.repository.CameraRepositoryImpl
import com.example.mlkitsample.domain.model.FaceAction
import com.example.mlkitsample.domain.usecase.LivenessUseCase
import com.google.mlkit.vision.face.Face
import java.io.File
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val useCase = LivenessUseCase(CameraRepositoryImpl())

    private var actionCompleted = false
    private var completedActions = 0
    private val requiredActions = 3

    private var currentAction: FaceAction? = null

    private val _livenessText = MutableLiveData<String>()
    val livenessText: LiveData<String> get() = _livenessText

    private val _livenessColor = MutableLiveData<Int>()
    val livenessColor: LiveData<Int> get() = _livenessColor

    private val _capturedImagePath = MutableLiveData<String?>()
    val capturedImagePath: LiveData<String?> get() = _capturedImagePath

    fun requestNextFaceAction() {
        currentAction = useCase.requestNextAction()
        actionCompleted = false
        _livenessColor.postValue(android.R.color.black)
        _livenessText.postValue(
            when (currentAction) {
                FaceAction.BLINK -> "Please blink your eyes."
                FaceAction.SMILE -> "Please smile."
                FaceAction.HEAD_SHAKE -> "Please shake your head."
                else -> ""
            }
        )
    }

    fun checkLiveness(face: Face) {
        if (actionCompleted) return

        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
        val smilingProb = face.smilingProbability ?: 0.0f
        val headYaw = face.headEulerAngleY

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

    private fun onActionCompleted(message: String) {
        _livenessText.postValue("$message ✅")
        _livenessColor.postValue(android.R.color.holo_green_light)

        actionCompleted = true
        completedActions++

        if (completedActions >= requiredActions) {
            takePhoto()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                requestNextFaceAction()
            }, 1500)
        }
    }

    private fun onActionWrong(message: String) {
        _livenessText.postValue(message)
        _livenessColor.postValue(android.R.color.holo_red_light)
    }

    private fun takePhoto() {
        val file = File(getApplication<Application>().externalCacheDir, "captured_face.jpg")
        // Normally imageCapture would trigger this. Simulate success:
        _capturedImagePath.postValue(file.absolutePath)
    }
}