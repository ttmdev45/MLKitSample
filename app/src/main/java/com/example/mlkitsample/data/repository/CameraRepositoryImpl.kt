package com.example.mlkitsample.data.repository

import com.example.mlkitsample.domain.model.FaceAction
import com.example.mlkitsample.domain.repository.CameraRepository

class CameraRepositoryImpl : CameraRepository {
    override fun getRandomAction(): FaceAction {
        return FaceAction.entries.random()
    }
}