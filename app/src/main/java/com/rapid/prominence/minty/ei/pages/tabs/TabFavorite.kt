package com.rapid.prominence.minty.ei.pages.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.databinding.TabCollectBinding
import com.rapid.prominence.minty.ei.kts.mmkvs.loadSongs
import com.tencent.mmkv.MMKV


class TabFavorite : Fragment() {
    private  var _binding: TabCollectBinding ?= null
    private  val binding get() = _binding!!
    private var adapter: TabHome.SongAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabCollectBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onResume() {
        super.onResume()
        if (MMKV.defaultMMKV().getBoolean("isAuth",false)){
            loadData()
        }else{
            binding?.emptyLayout?.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = TabHome.SongAdapter()
        adapter?.setNormalType(1)
        binding?.recyclerView?.adapter = adapter
    }


    private fun loadData() {
        try {
            context?.let {
                val favsongs  = ArrayList<Song>()
                loadSongs(Song()).forEach {
                    if (it.isCollect){
                        favsongs.add(it)
                    }
                }
                if (favsongs.isEmpty()){
                    binding?.emptyLayout?.visibility = View.VISIBLE
                    adapter?.add(favsongs)
                    return@let
                }
                if (  binding?.emptyLayout?.visibility == View.VISIBLE){
                    binding?.emptyLayout?.visibility = View.GONE
                }
                adapter?.add(favsongs)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

        }
    }





