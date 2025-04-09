package com.example.mlkitsample

import com.google.mlkit.vision.face.Face

interface FaceDetectionListener {
    fun onFaceDetected(face: Face)
    fun onRequestMessage(msg: String)
    fun onActionCompleted(msg: String)
    fun onActionWrong(msg: String)
    fun onDetectActionCompleted(msg: String)

    fun onSuccessUpload(msg: String)
    fun onFailUpload(msg: String)

}