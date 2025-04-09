package com.example.mlkitsample

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitsample.databinding.ActivitySecondMainBinding
import com.example.mlkitsample.selfieProcess.SelfieFragment

class SecondMainActivity : AppCompatActivity() {

    private lateinit var activitySecondMainBinding: ActivitySecondMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activitySecondMainBinding = ActivitySecondMainBinding.inflate(layoutInflater)
        setContentView(activitySecondMainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        activitySecondMainBinding.btnLiveness.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(activitySecondMainBinding.fragmentContainer.id, SelfieFragment())
                .addToBackStack(null)
                .commit()

        }
    }
}

