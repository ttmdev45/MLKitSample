package com.example.mlkitsample.presentation.main

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("android:preview_visibility")
fun setVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("android:image_visibility")
fun setImageVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.GONE else View.VISIBLE
}


//@BindingAdapter("imageUri")
//fun bindImageUri(imageView: ImageView, uri: Uri?) {
//    // Using Glide to load the image
//    uri?.let {
//        Glide.with(imageView.context)
//            .load(it) // The Uri from LiveData
//            .into(imageView)
//    }
//}

@BindingAdapter("imageUri")
fun ImageView.setImageUri(imageUri: Uri?) {
    imageUri?.let {
        this.setImageURI(it)
        this.scaleX = -1f // Flip the image horizontally
    }
}

