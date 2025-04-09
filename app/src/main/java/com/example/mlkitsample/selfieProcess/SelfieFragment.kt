package com.example.mlkitsample.selfieProcess
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.mlkitsample.FaceDetectionListener
import com.example.mlkitsample.R
import com.example.mlkitsample.databinding.FragmentSelfieBinding
import com.google.mlkit.vision.face.Face
import java.util.concurrent.Executors
import androidx.core.graphics.drawable.toDrawable

class SelfieFragment : Fragment(), FaceDetectionListener {

    private lateinit var binding: FragmentSelfieBinding
    private val selfieViewModel: SelfieViewModel by activityViewModels()
    private lateinit var cameraManager: CameraManager
    private var loadingDialog: AlertDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSelfieBinding.inflate(
            inflater,container,false
        )
        cameraManager = CameraManager(
            requireContext(),
            viewLifecycleOwner,
            binding,
            selfieViewModel,
            this
        ) // Initialize CameraManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupObservers()
        actions()
        requestCameraPermission()
    }

    private fun initViewModel() {
        binding.viewModel = selfieViewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun onResume() {
        super.onResume()

        selfieViewModel.setCameraActive(
            selfieViewModel.isCameraActive.value ?: true
        )
    }


    private fun requestCameraPermission(){
        when{
            ContextCompat.checkSelfPermission(
                requireContext(),
               Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                selfieViewModel.setCameraActive(true)
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)->{
                Toast.makeText(context,"Camera permission is needed!", Toast.LENGTH_SHORT).show()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted->
            if(isGranted)
            {
                selfieViewModel.setCameraActive(true)
            }
            else{
                Toast.makeText(context, "Camera permission is required!", Toast.LENGTH_LONG)
                    .show()
            }
        }

    private fun setupObservers(){
        selfieViewModel.isCameraActive.observe(viewLifecycleOwner){ isActive->

            if(isActive) cameraManager.startCamera() else cameraManager.stopCamera()
        }
    }

    private fun actions(){

//        binding.btnReady.setOnClickListener {
//
//            Handler(Looper.getMainLooper()).postDelayed({
//                cameraManager.requestNextFaceAction()
//                binding.btnReady.visibility = View.GONE
//            }, 1000)
//        }
//
//        binding.btnTryAgain.setOnClickListener {
//
//            retakePhoto()
//        }

        binding.btnReady.setOnClickListener {
            cameraManager.resetLiveness()
            Handler(Looper.getMainLooper()).postDelayed({
                cameraManager.requestNextFaceAction()
                binding.btnReady.visibility = View.GONE
            }, 1000)
        }

        binding.btnTryAgain.setOnClickListener {
            retakePhoto()
          //  cameraManager.startCamera()
        }

    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showLoader()
            cameraManager.takePhoto()
        } else {
            requestCameraPermission()
        }
    }
    private fun retakePhoto() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            selfieViewModel.retakePhoto() // Delete cached photo
            selfieViewModel.setCameraActive(true) // Restart camera
            binding.livenessText.text = ""
            cameraManager.resetLiveness()
        } else {
            requestCameraPermission()
        }

    }
    override fun onDestroy() {
        super.onDestroy()
        selfieViewModel.onClearedForSelfieForm()
        cameraManager.cameraExecutor?.shutdown()
    }

    fun showLoader() {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = AlertDialog.Builder(requireContext()).setCancelable(false)
            .setView(R.layout.loading_fragment).show()

        loadingDialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
    fun hideLoader() {
        loadingDialog?.dismiss()
    }

    override fun onFaceDetected(face: Face) {

    }

    override fun onRequestMessage(msg: String) {
        binding.livenessText.text = msg
    }

    @SuppressLint("SetTextI18n")
    override fun onActionCompleted(msg: String) {
        binding.livenessText.text = "$msg \u2705"
        binding.livenessText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))

    }

    @SuppressLint("SetTextI18n")
    override fun onActionWrong(msg: String) {
        binding.livenessText.text = "$msg \u274C"
        binding.livenessText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))

    }

    override fun onDetectActionCompleted(msg: String) {
        binding.livenessText.text = msg
        takePhoto()
    }

    override fun onSuccessUpload(msg: String) {
        binding.livenessText.text = msg
       // binding.btnTryAgain.visibility = View.VISIBLE
      //  binding.btnReady.visibility = View.GONE
        hideLoader()
    }

    override fun onFailUpload(msg: String) {
        binding.livenessText.text = msg
       // binding.btnTryAgain.visibility = View.VISIBLE
      //  binding.btnReady.visibility = View.GONE
        hideLoader()
    }

}