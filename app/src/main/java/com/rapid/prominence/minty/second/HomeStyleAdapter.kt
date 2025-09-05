package com.rapid.prominence.minty.second

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.homeMoodDetInter
import com.rapid.prominence.minty.databinding.MintyItemMoodBinding
import com.rapid.prominence.minty.extensions.firebaseLog
import com.zionhuang.innertube.pages.MoodAndGenres

class HomeStyleAdapter(var homeActivity: MintyBaseActivity) : RecyclerView.Adapter<HomeStyleAdapter.MoodAndGenresViewHolder>() {

    private var moodAndGenres = listOf<MoodAndGenres.Item>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodAndGenresViewHolder {
        val binding = MintyItemMoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MoodAndGenresViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodAndGenresViewHolder, position: Int) {
        val moodAndGenre = moodAndGenres[position]
        holder.bind(moodAndGenre,position)
    }

    override fun getItemCount(): Int = moodAndGenres.size

    fun submitList(newMoodAndGenres: List<MoodAndGenres.Item>) {
        moodAndGenres = newMoodAndGenres
        notifyDataSetChanged()
    }

    inner class MoodAndGenresViewHolder(private val binding: MintyItemMoodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(moodAndGenre: MoodAndGenres.Item, position: Int) {
            when(position%8){
                0 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_ones)
                }
                1 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_tws)
                }
                2 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_thr)
                }
                3 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_for)
                }
                4 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_fv)
                }
                5 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_sx)
                }
                6 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_sv)
                }
                7 ->{
                    binding.ivs.setImageResource(R.mipmap.mood_eg)
                }

            }
            binding.button.text = moodAndGenre.title
            binding.container.setOnClickListener {
                if (homeActivity?.isFinishing == true || homeActivity?.isDestroyed == true){
                    return@setOnClickListener
                }
                homeActivity?.startInterActivity(Intent(homeActivity, MintySecMdDetailPage::class.java).apply {
                    moodAndGenre?.endpoint?.let {
                        putExtra("browseId", it.browseId)
                        putExtra("params", it.params)
                    }
                }, homeMoodDetInter(),false)
                firebaseLog("home_to_style_detail")

            }

        }
    }
}
