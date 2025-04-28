package com.example.mlkitsample.presentation.main

import com.google.mlkit.vision.face.Face

interface FaceDetectionListener {
    fun onFaceDetected(face: Face)
    fun onRequestMessage(msg: String)
    fun onActionCompleted(msg: String)
    fun onActionWrong(msg: String)

    fun onDetectActionCompleted(msg: String)

    fun onSuccessUpload(msg: String)
    fun onFailUpload(msg: String)

    fun onNoFaceDetected(msg: String)
    fun onMultipleFaceDetected(msg: String)
    fun onFaceNotCentered(msg: String)
    fun onTooFarFaceDetected(msg: String)


}