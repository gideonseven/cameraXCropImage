package com.example.cameraxcropimage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cameraxcropimage.databinding.ActivityPreviewBinding

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.extras?.let {
            val path = it.getString("key") ?: ""
            println("MY PATH ==== $path")
            Glide.with(applicationContext).load(path).into(binding.iv)
        }

        binding.btnRetake.setOnClickListener {
            finish()
        }
    }
}