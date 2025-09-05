package media.library.manager
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody


class MintyCommonInter : Interceptor {


    interface BasicRequestListener {
        fun loadFailed(message: String)
        fun loadSuccessConfig(configInfo: String)
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        mintyConfigIntersRes = ""
        val mintyConfigRequest = chain.request().newBuilder().build()
        val res = chain.proceed(mintyConfigRequest)
        try {
            res.body?.let {
                mintyConfigIntersRes = it.string()
                mintyConfigType = it.contentType()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return res.newBuilder().body(mintyConfigIntersRes.toResponseBody(mintyConfigType)).build()
    }
    private var mintyConfigType : MediaType?= null
    private var mintyConfigIntersRes = ""
}