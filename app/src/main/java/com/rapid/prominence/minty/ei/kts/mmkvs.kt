package com.rapid.prominence.minty.ei.kts

import com.google.gson.Gson
import com.rapid.prominence.minty.bean.Song
import com.tencent.mmkv.MMKV

object mmkvs {

        fun loadSongs(bean: Song): ArrayList<Song> {
            val rizeStatusCreateMmkv = MMKV.defaultMMKV()
            val createDataBean = ArrayList<Song>()
            val size = rizeStatusCreateMmkv.getInt(name + "size", 0)
            for (i in 0 until size) {
                if (rizeStatusCreateMmkv.getString(name + i, null) != null) {
                    try {
                        createDataBean.add(
                            Gson().fromJson(
                                rizeStatusCreateMmkv.getString(name + i, null),
                                bean.javaClass
                            ) as Song
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return createDataBean
        }


    fun initCreateData(choreoViewModels: List<Song?>?) {
        val genemkAreaMmkv = MMKV.defaultMMKV()
        if (choreoViewModels.isNullOrEmpty()) {

            genemkAreaMmkv.putInt(name + "size", 0)
            val size = genemkAreaMmkv.getInt(name + "size", 0)
            for (i in 0 until size) {
                if (genemkAreaMmkv.getString(name + i, null) != null) {
                    genemkAreaMmkv.remove(name + i)
                }
            }
        } else {
            genemkAreaMmkv.putInt(name + "size", choreoViewModels.size)
            for (i in choreoViewModels.indices) {
                choreoViewModels[i]?.index = i
                genemkAreaMmkv.remove(name + i)
                genemkAreaMmkv.remove(Gson().toJson(choreoViewModels[i]))
                genemkAreaMmkv.putString(name + i, Gson().toJson(choreoViewModels[i]))
            }
        }
    }
    const val name = "SpecialMMKV"

}