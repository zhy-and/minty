package com.rapid.prominence.minty.privacy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.databinding.MintyPriBinding

class Privacys : MintyBaseActivity() {

    private lateinit var binding: MintyPriBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyPriBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webs.loadUrl("https://feue.mintymu.com/privacyPolicy.html")
        binding.titles.apply {
            inputSearch.visibility = View.GONE

            lists.visibility = View.GONE
            nowPlayingText.text = "Privacy"
            closeButton.setOnClickListener {
                finish()
            }
        }

    }

}
