package com.example.mlkitsample.domain.usecase

import com.example.mlkitsample.domain.model.FaceAction
import com.example.mlkitsample.domain.repository.CameraRepository

class LivenessUseCase(private val repository: CameraRepository) {
    fun requestNextAction(): FaceAction {
        return repository.getRandomAction()
    }
}