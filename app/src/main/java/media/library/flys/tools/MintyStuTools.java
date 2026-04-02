package media.library.flys.tools;

import static com.rapid.prominence.minty.cons.PosExtKt.MMKV_MINTY_COEFS;
import static com.rapid.prominence.minty.cons.PosExtKt.MMKV_MINTY_LINES;
import static com.rapid.prominence.minty.cons.PosExtKt.MMKV_MINTY_SYS;
import static com.rapid.prominence.minty.cons.PosExtKt.MINTY_APP_APP_REFER_INFO;
import static com.rapid.prominence.minty.utils.MintyDataKeeperKt.getInstallSys;
import static media.library.manager.MintyApi.commonKys;
import static media.library.manager.MintyApi.commonUniIds;

import com.appsflyer.AFAdRevenueData;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AdRevenueScheme;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.MediationNetwork;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.AdValue;
import com.rapid.prominence.minty.application.App;
import com.tencent.mmkv.MMKV;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import android.annotation.SuppressLint;

import media.library.manager.MintyApi;


public class MintyStuTools {



    public static String getCountryInfo(Context context) {
        if (context == null) {
            return "NULL";
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryIso = telephonyManager.getNetworkCountryIso();
        if (countryIso == null || "".equalsIgnoreCase(countryIso)) {
            return context.getResources().getConfiguration().locale.getCountry();
        } else {
            return countryIso;
        }

    }
    public static String addMd5(String text) throws NoSuchAlgorithmException {
        //获取摘要器 MessageDigest
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        //通过摘要器对字符串的二进制字节数组进行hash计算
        byte[] digest = messageDigest.digest(text.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            //循环每个字符 将计算结果转化为正整数;
            int digestInt = b & 0xff;
            //将10进制转化为较短的16进制
            String hexString = Integer.toHexString(digestInt);
            //转化结果如果是个位数会省略0,因此判断并补0
            if (hexString.length() < 2) {
                sb.append(0);
            }
            //将循环结果添加到缓冲区
            sb.append(hexString);
        }
        //返回整个结果
        return sb.substring(8, 24);
    }

    public static HashMap<String, String> loadHeadInfo() {
        if (App.instance == null){
            return null;
        }
        HashMap<String, String> params = new HashMap<>();
        try {
            long inMillis = Calendar.getInstance().getTimeInMillis();
            String author = JMTools.tperklyTranslateMd5Encrypt32Lower((commonUniIds()+ App.instance.getPackageName()+ commonKys()+inMillis));
            params.put("pkg",App.instance.getPackageName());
            params.put("timestamp",inMillis+"");
            params.put("dynKeyFlag","101");
            if (!TextUtils.isEmpty(author)) {
                params.put("Authorization",author);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    public static String uniqueId(Context context) {
        @SuppressLint("HardwareIds")
        // ANDROID_ID是设备第一次启动时产生和存储的64bit的一个数，当设备被wipe后该数重置。
        String androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        @SuppressLint("HardwareIds")
        String id = androidID + Build.SERIAL + Build.TIME; // +硬件序列号
        try {
            return addMd5(id);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return id;
        }
    }


    public static void valueUpload(AdValue adValue, String adType, String adId,Long pos) {
        try {
            initAFEvents(adValue,adId,adType);
            double configValues = 0;
            long distanceTimes = Long.parseLong(MMKV.defaultMMKV().getString(MMKV_MINTY_SYS,"100"));
            if (System.currentTimeMillis() -  getInstallSys() <=  (distanceTimes * 60 * 60 * 1000)) {
                BigDecimal value = new BigDecimal(String.valueOf(adValue.getValueMicros())).divide(new BigDecimal(1000000.0));
                double lines = Double.parseDouble((MMKV.defaultMMKV().getString(MMKV_MINTY_LINES,"0")));
                if (value.doubleValue() >= lines){
                    double coef = Double.parseDouble((MMKV.defaultMMKV().getString(MMKV_MINTY_COEFS,"1")));
                    AppEventsLogger logger =  AppEventsLogger.newLogger(App.instance);
                    logger.logPurchase(new BigDecimal(coef*value.doubleValue()), Currency.getInstance(adValue.getCurrencyCode()));
                    configValues = new BigDecimal(coef*value.doubleValue()).doubleValue();

                }else {
                }
            } else {
            }
                    //todo
            AdMobTaichiAroHandler.INSTANCE.handlePaidEvent(adValue);
            MintyApi.Companion.handleValues(adId,adType,adValue,configValues,pos);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void initAFEvents(AdValue adValue, String adId, String adType) {
        double adValuesDou = new BigDecimal(String.valueOf(adValue.getValueMicros())).divide(new BigDecimal(1000000.0)).doubleValue();
        AppsFlyerLib appsflyer = AppsFlyerLib.getInstance();
//

        Map<String, Object> eventValues = new HashMap<>();
        eventValues.put(AFInAppEventParameterName.CURRENCY,adValue.getCurrencyCode());
        eventValues.put(AFInAppEventParameterName.REVENUE, adValuesDou);
        appsflyer.logEvent(App.instance, AFInAppEventType.PURCHASE,eventValues);



//// Create an instance of AFAdRevenueData
        AFAdRevenueData adRevenueData = new AFAdRevenueData(
                "admob",       // monetizationNetwork
                MediationNetwork.GOOGLE_ADMOB, // mediationNetwork
                "USD",adValuesDou           // currencyIso4217Code
                      // revenue
        );

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put(AdRevenueScheme.COUNTRY, getCountryInfo(App.instance));
        additionalParameters.put(AdRevenueScheme.AD_UNIT, adId);
        additionalParameters.put(AdRevenueScheme.AD_TYPE, adType);

        appsflyer.logAdRevenue(adRevenueData, additionalParameters);
    }


    public static HashMap<String, String> getRInfos() {
        if (App.instance == null){
            return null;
        }
        HashMap<String, String> params = new HashMap<>();
        try {
            long inMillis = Calendar.getInstance().getTimeInMillis();
            String author = JMTools.tperklyTranslateMd5Encrypt32Lower((commonUniIds()+ App.instance.getPackageName()+ commonKys()+inMillis));
            String infos = MMKV.defaultMMKV().getString(MINTY_APP_APP_REFER_INFO, "");
            if (TextUtils.isEmpty(infos)) {
                infos = commonUniIds()+"_normal";
            }
            params.put("Original",infos);
            params.put("pkg", App.instance.getPackageName());
            params.put("timestamp",inMillis+"");
            params.put("dynKeyFlag","101");
            if (!TextUtils.isEmpty(author)) {
                params.put("Authorization",author);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

}
//
