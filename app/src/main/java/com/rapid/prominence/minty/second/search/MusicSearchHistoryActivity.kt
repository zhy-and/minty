package com.rapid.prominence.minty.second.search


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.rapid.prominence.minty.database.entities.SearchHistory
import kotlinx.coroutines.launch
import com.blankj.utilcode.util.ToastUtils
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.gms.ads.nativead.NativeAd
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.searchHistoryBanner
import com.rapid.prominence.minty.cons.searchRecordInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.databinding.MintySecSearchHisBinding
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.second.SearchHisItemAdapter
import com.rapid.prominence.minty.second.SearchBean
import com.rapid.prominence.minty.second.SearchSuggestAdapter
import com.rapid.prominence.minty.second.databean.OnlineSearchSuggestionViewModel
import com.tencent.mmkv.MMKV
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool


class MusicSearchHistoryActivity : MintyBaseActivity() {
    private  var searchViewArtAdapter: SearchSuggestAdapter ?= null
    private  var newHistoryList: List<SearchHistory> ?= null
    private var data: MusicDatabase?= null
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                MESSAGE_MSG_CODE -> {
                    loadSpeNative()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSpeNative()
    }
    override fun onPause() {
        super.onPause()
        if (handler?.hasMessages(MESSAGE_MSG_CODE) == true){
            handler?.removeMessages(MESSAGE_MSG_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
    }


    private fun loadSpeNative() {
        val normalBan: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            searchHistoryBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("searchHistoryBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("searchHistoryBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(searchHistoryBanner())) {
                LogUtils.e("searchHistoryBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(searchHistoryBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("searchHistoryBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("searchHistoryBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(searchHistoryBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }

    private lateinit var binding: MintySecSearchHisBinding

    var isFirst = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(searchRecordInter(),this)
        
        enableEdgeToEdge()
        binding = MintySecSearchHisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tops.apply {
            search.setOnClickListener {
                searchAction()
            }
            closeButton.setOnClickListener {
                finishCurrent()
            }
            data =  InternalDatabase.newInstance(this@MusicSearchHistoryActivity)
            val homeViewModel = OnlineSearchSuggestionViewModel(data!!)
            searchEditText.requestFocus()
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {

                    if (s.isNullOrEmpty() || binding.tops.searchEditText.toString().isEmpty()) {
                        binding.searchHistoryAll.visibility = View.VISIBLE
                        binding.searchSuggest.visibility = View.GONE
                        return
                    }
                    isFirst = true
                    homeViewModel.query.value = s.toString()

                }

            })
            lifecycleScope.launch {
                homeViewModel.viewState.collect {
                    if (isFinishing || isDestroyed || !isFirst) {
                        return@collect
                    }

                    it.suggestions.isNotEmpty().apply {
                        if (  binding.searchHistoryAll.isVisible){
                            binding.searchHistoryAll.visibility = View.GONE

                        }
                        if (  binding.searchSuggest.isGone){
                            binding.searchSuggest.visibility = View.VISIBLE

                        }
                        if (searchViewArtAdapter == null){
                            binding.searchSuggest.layoutManager = LinearLayoutManager(this@MusicSearchHistoryActivity)
                            searchViewArtAdapter = SearchSuggestAdapter(emptyList()) { songItem ->
                                startInterActivity(Intent(this@MusicSearchHistoryActivity, MusicSearchActivity::class.java).apply {
                                    putExtra("search_txt",songItem)
                                }, searchRecordInter(),true)
                            }
                            binding.searchSuggest.adapter = searchViewArtAdapter
                        }
                        searchViewArtAdapter?.addData(it.suggestions)
                    }
                }
            }
            searchEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {

                    // 处理确认事件
                    searchAction()
                    // 隐藏软键盘
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)

                    true // 消费事件
                } else {
                    false
                }
            }
        }
        ImmersionBar.with(this).navigationBarColor(R.color.new_color).init()

//        InternalDatabase.newInstance(this@MusicSearchActivity).searchHistory().map { history ->
        lifecycleScope.launch {
            try {

                data?.searchHistory()?.collect{
//                Log.e("history----", it.toString())
                    val list = ArrayList<SearchBean>()
                    newHistoryList = it
                    it.forEach { ios->
                        list.add(SearchBean(ios.query))
                    }
// 配置Flexbox布局
                    val layoutManager =  FlexboxLayoutManager(this@MusicSearchHistoryActivity)
                    layoutManager.setFlexDirection(FlexDirection.ROW)
                    layoutManager.setJustifyContent(JustifyContent.FLEX_START)
                    layoutManager.maxLine = 3
                    binding.flowList.setLayoutManager(layoutManager)

                    val adapter =
                        SearchHisItemAdapter(
                            list
                        )
                    adapter.setOnItemClickListener(object :
                        SearchHisItemAdapter.OnItemClickListener{
                        override fun onItemClick(item: SearchBean?) {
                            startInterActivity(Intent(this@MusicSearchHistoryActivity, MusicSearchActivity::class.java).apply {
                                putExtra("search_txt",item?.text)
                            }, searchRecordInter(),true)

                        }

                        override fun onDeleteClick(position: Int) {
                            newHistoryList?.let {
                                if (it.isNullOrEmpty()){
                                    return
                                }
                            }
                            newHistoryList?.forEach {
                                if (it.query == adapter.data.getOrNull(position)?.text){
                                    data?.query {
                                        delete(it)
                                    }
                                }
                            }
                            adapter.removeItem(position)

                        }

                    })

                    binding.flowList.setAdapter(adapter)

                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }

    }

    private fun searchAction() {
        if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())) {
            ToastUtils.showShort("Please input content")
            return
        }
        firebaseLog("search_enter_to_search")
        startInterActivity(Intent(this@MusicSearchHistoryActivity, MusicSearchActivity::class.java).apply {
            putExtra("search_txt",binding.tops.searchEditText.text.toString())
        }, searchRecordInter(),true)
    }





}