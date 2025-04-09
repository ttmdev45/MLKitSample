package com.example.mlkitsample.domain.repository

import com.example.mlkitsample.domain.model.FaceAction

interface CameraRepository {
    fun getRandomAction(): FaceAction
}
