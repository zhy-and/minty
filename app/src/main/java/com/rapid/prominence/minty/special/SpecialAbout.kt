package com.rapid.prominence.minty.special

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.databinding.MintyAboutBinding
import com.rapid.prominence.minty.privacy.Privacys

class SpecialAbout : MintyBaseActivity() {

    private lateinit var binding: MintyAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pp.setOnClickListener {
            startActivity(Intent(this, Privacys::class.java))
            finish()
        }
        binding.titles.apply {
            lists.visibility = View.GONE
            nowPlayingText.text = "About"
            closeButton.setOnClickListener {
                finish()
            }
        }



    }

}
