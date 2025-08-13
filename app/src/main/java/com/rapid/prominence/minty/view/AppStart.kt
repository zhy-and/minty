package com.rapid.prominence.minty.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.databinding.ActivityMainBinding
import com.rapid.prominence.minty.second.view.MintySecMainPage


class AppStart : MintyBaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            jumpPages()
        }catch (e : Exception){
            e.printStackTrace()
        }
    }



    private var messManager : Handler?= null


    private fun jumpPages() {
        binding?.root?.postDelayed({
                startActivity(Intent(this, MintySecMainPage::class.java))
            finish()
        }, 2 * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        messManager?.removeCallbacksAndMessages(null)
        binding?.anims?.cancelAnimation()
    }



}