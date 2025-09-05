package media.library.manager
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import media.library.flys.tools.MintyStuTools
import media.library.flys.tools.MintyStuTools.loadHeadInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit


class MintyWorkQueue private constructor() {
    private fun paramsRequest(baseUrl: String, specialHeads: Headers.Builder): Request {
        return Request.Builder()
            .url(baseUrl)
            .headers(specialHeads.build())
            .build()
    }

    private fun createOk(): Boolean {
        if (requestObj == null) {
            return true
        }
        return false
    }

    fun referTranslateInfo(map:HashMap<String,String>?, speUrl:String, mintyConfiglateNetRequest: MintyCommonInter.BasicRequestListener){
        if (createOk()) return
        var originalConfig = ""
        var mintyConfigUrl = ""
        if (map?.isNotEmpty() == true){
            val list = ArrayList(map.keys)
            mintyConfigUrl = "$speUrl?"
            for (configKy in map.keys){
                originalConfig += if (list[map.size - 1].equals(configKy)){
                    configKy + "=" + map[configKy]
                }else{
                    configKy + "=" + map[configKy] + "&"
                }
            }
            mintyConfigUrl += originalConfig
        }else{
            mintyConfigUrl = speUrl
        }
        val heads: Headers.Builder = Headers.Builder()
        val headMap: HashMap<String, String> = MintyStuTools.getRInfos()
        for (key in headMap.keys) {
            heads.add(key, headMap[key]!!)
        }
        val mintyConfigListener: Call? = requestObj?.newCall(paramsRequest(mintyConfigUrl, heads))
        mintyConfigListener?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mintyConfiglateNetRequest.loadFailed(e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { mintyConfiglateNetRequest.loadSuccessConfig(it.string()) }
            }

        })

    }

    fun upValues(paramsOut:HashMap<String,String>, mintyConfigUrls:String, netRequest: MintyCommonInter.BasicRequestListener){
        if (createOk()) return
//        val gson = GsonBuilder().disableHtmlEscaping().create()
        val jsonParams: String = Gson().toJson(paramsOut)
        LogUtils.e("jsonParams",jsonParams)
        val requestBody: RequestBody = jsonParams.toRequestBody("application/json".toMediaTypeOrNull())
        val builder: Headers.Builder = Headers.Builder()
        val stringStringHashMap: HashMap<String, String> =
            loadHeadInfo()
        for (key in stringStringHashMap.keys) {
            builder.add(key, stringStringHashMap[key]!!)
        }

        val postSpecialR1: Request = Request.Builder()
            .url( mintyConfigUrls)
            .headers(builder.build())
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        val listeners: Call? = requestObj?.newCall(postSpecialR1)
        listeners?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netRequest.loadFailed(e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    netRequest.loadSuccessConfig(it.string())
                }
            }

        })
    }


    fun initNetWork() {
        requestObj = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS) //连接超时
            .addInterceptor(MintyCommonInter())
            .readTimeout(15, TimeUnit.SECONDS) //读取超时
            .writeTimeout(15, TimeUnit.SECONDS) //写超时
            .build()
    }
    companion object {
        private var ins: MintyWorkQueue? = null
            get() {
                if (field == null) {
                    field = MintyWorkQueue()
                }
                return field
            }
        @Synchronized
        fun get(): MintyWorkQueue {
            return ins!!
        }
    }

    private var requestObj: OkHttpClient?= null



    fun requestConfigInfo(map:HashMap<String,String>?, speUrl:String, netRequest: MintyCommonInter.BasicRequestListener){
        if (createOk()) return
        var originalConfig = ""
        var mintyConfigUrl = ""
        if (map?.isNotEmpty() == true){
            val list = ArrayList(map.keys)
            mintyConfigUrl = "$speUrl?"
            for (configKy in map.keys){
                originalConfig += if (list[map.size - 1].equals(configKy)){
                    configKy + "=" + map[configKy]
                }else{
                    configKy + "=" + map[configKy] + "&"
                }
            }
            mintyConfigUrl += originalConfig
        }else{
            mintyConfigUrl = speUrl
        }
        val heads: Headers.Builder = Headers.Builder()
        val headMap: HashMap<String, String> = loadHeadInfo()
        for (key in headMap.keys) {
            heads.add(key, headMap[key]!!)
        }
        val mintyConfigListener: Call? = requestObj?.newCall(paramsRequest(mintyConfigUrl, heads))
        mintyConfigListener?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netRequest.loadFailed(e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { netRequest.loadSuccessConfig(it.string()) }
            }

        })

    }
}
