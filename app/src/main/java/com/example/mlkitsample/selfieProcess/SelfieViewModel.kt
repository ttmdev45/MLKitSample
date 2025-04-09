package com.example.mlkitsample.selfieProcess

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SelfieViewModel : ViewModel(){



    /*selfie process*/
    private val _isCameraActive = MutableLiveData<Boolean>()
    val isCameraActive: LiveData<Boolean> = _isCameraActive

    private val _isPreviewVisible = MutableLiveData(true)
    val isPreviewVisible: LiveData<Boolean> = _isPreviewVisible

    private val _capturedImageUri = MutableLiveData<Uri?>()
    val capturedImageUri: LiveData<Uri?> = _capturedImageUri

    private val _base64Image = MutableLiveData<String?>()
    val base64Image: LiveData<String?> = _base64Image


    fun setBase64Image(base64String: String) {
        _base64Image.value = base64String
    }


    fun setCameraActive(isActive: Boolean) {
        _isCameraActive.value = isActive
        _isPreviewVisible.value = isActive
        _capturedImageUri.value = if (isActive) null else _capturedImageUri.value
    }

    fun takePhoto(imageUri: Uri) {
        _capturedImageUri.value = imageUri
        _isPreviewVisible.value = false
        _isCameraActive.value = false
    }

    fun retakePhoto() {
        deleteCachedPhoto()
        setCameraActive(true) // Restart camera
    }

    fun deleteCachedPhoto() {
        capturedImageUri.value?.let { uri ->
            val file = File(uri.path ?: return)
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("DataCollectionViewModel","Cached photo deleted: ${file.absolutePath}")
                } else {
                    Log.e("DataCollectionViewModel","Failed to delete cached photo: ${file.absolutePath}")
                }
            }
        }
    }


//    fun sendSelfiePhoto(
//        isActivationSource: Boolean,
//        cardToken: String?
//    ): LiveData<Resource<GeneralResponse>> {
//
//        return repository.sendSelfiePhoto(
//            selfieRequest = SelfiePhotoRequest(
//                cardToken = cardToken,
//                image = base64Image.value
//            ),
//            isActivationSource = isActivationSource
//        )
//    }

    fun clearSelfiePhoto() {
        _capturedImageUri.value = null
        _base64Image.value = ""
    }




    fun onClearedForSelfieForm(){
        super.onCleared()
        clearSelfiePhoto()
    }



}