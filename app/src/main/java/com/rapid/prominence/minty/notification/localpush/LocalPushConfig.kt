package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.graphics.Color

import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tencent.mmkv.MMKV
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalPushConfig {
    private const val TAG = "LocalPushConfig"

    /**
     * 单段默认正文（GP 送审画像）。FRC 外壳由 [defaultConfigJson] 包成 organic/paid 双段。
     */

    private val DEFAULT_SEGMENT_JSON = """
        {
          "version": 0,
          "enabled": true,
          "global": {
            "first_max": 5,
            "daily_max": 500,
            "cooldown_sec": 0,
            "max_visible": 200,
            "skip_foreground": true,
            "install_age_sec": 0
          },
          "buckets": {
            "normal_ntf": {
              "enabled": true,
              "interval_sec": 3600,
              "daily_max": 300,
              "cooldown_sec": 0,
              "repetitions": 1,
              "max_visible": 200
            },
            "res_ntf": {
              "enabled": true,
              "interval_sec": 14400,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "repetitions": 1,
              "max_visible": 1
            },
            "apk_ntf": {
              "enabled": true,
              "interval_sec": -1,
              "daily_max": 2,
              "cooldown_sec": 28800,
              "repetitions": 1,
              "max_visible": 1
            }
          },
          "scenes": {
            "app_inactive": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": true,
              "priority": 62,
              "daily_max": 1,
              "cooldown_sec": 86400,
              "template_ids": ["style_yellow_clean"],
              "router": "home"
            },
            "battery_below_50": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 70,
              "daily_max": 1,
              "cooldown_sec": 21600,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "battery_below_20": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 85,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "template_ids": ["style_red_event"],
              "router": "home"
            },
            "charging": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 65,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_green_event"],
              "router": "home"
            },
            "charging_to_80": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 72,
              "daily_max": 1,
              "cooldown_sec": 21600,
              "template_ids": ["style_green_event"],
              "router": "home"
            },
            "charging_disconnect": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 64,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "network_change": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 50,
              "daily_max": 1,
              "cooldown_sec": 7200,
              "template_ids": ["style_blue_event"],
              "router": "home"
            },
            "screen_lock": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 55,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "screen_on_after_lock": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 54,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "template_ids": ["style_blue_event"],
              "router": "home"
            },
            "user_unlock": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 53,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "template_ids": ["style_blue_event"],
              "router": "home"
            },
            "boot_completed": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 52,
              "daily_max": 1,
              "cooldown_sec": 86400,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "wired_headset_connect": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 73,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_green_event"],
              "router": "player"
            },
            "wired_headset_disconnect": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 71,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "bluetooth_connect": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 74,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_green_event"],
              "router": "player"
            },
            "bluetooth_disconnect": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 70,
              "daily_max": 1,
              "cooldown_sec": 14400,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "audio_focus_lost": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 76,
              "daily_max": 1,
              "cooldown_sec": 21600,
              "template_ids": ["style_blue_event"],
              "router": "player"
            },
            "volume_above_50": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 48,
              "daily_max": 1,
              "cooldown_sec": 7200,
              "template_ids": ["style_blue_event"],
              "router": "home"
            },
            "volume_below_20": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 47,
              "daily_max": 1,
              "cooldown_sec": 7200,
              "template_ids": ["style_blue_event"],
              "router": "home"
            },
            "scheduled_push_1": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 58,
              "daily_max": 2,
              "cooldown_sec": 14400,
              "template_ids": ["style_blue_event"],
              "router": "home",
              "times": ["09:00", "20:50"]
            },
            "scheduled_push_2": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 57,
              "daily_max": 2,
              "cooldown_sec": 14400,
              "template_ids": ["style_yellow_event"],
              "router": "home",
              "times": ["20:51", "22:30"]
            },
            "scheduled_push_3": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 56,
              "daily_max": 2,
              "cooldown_sec": 14400,
              "template_ids": ["style_green_event"],
              "router": "widget_pin",
              "times": ["20:52", "16:00"]
            },
            "playback_paused": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 66,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "delay_sec": 5,
              "template_ids": ["style_green_event"],
              "router": "player"
            },
            "playback_background": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 67,
              "daily_max": 1,
              "cooldown_sec": 10800,
              "delay_sec": 5,
              "template_ids": ["style_blue_event"],
              "router": "player"
            },
            "app_background": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 52,
              "daily_max": 3,
              "cooldown_sec": 1800,
              "delay_sec": 5,
              "template_ids": ["style_yellow_event"],
              "router": "home"
            },
            "audio_download": {
              "enabled": true,
              "bucket": "normal_ntf",
              "periodic": false,
              "priority": 80,
              "daily_max": 2,
              "cooldown_sec": 21600,
              "template_ids": ["style_green_event"],
              "router": "home"
            }
          },
          "templates": {
            "style_blue_event": {
              "layout": "small",
              "button_bg": "#0B7CFF",
              "button_text": "#FFFFFF",
              "button_anim": "shimmer"
            },
            "style_green_event": {
              "layout": "small",
              "button_bg": "#14B86A",
              "button_text": "#FFFFFF",
              "button_anim": "shimmer"
            },
            "style_red_event": {
              "layout": "small",
              "button_bg": "#FF5A3D",
              "button_text": "#FFFFFF",
              "button_anim": "shimmer"
            },
            "style_yellow_event": {
              "layout": "small",
              "button_bg": "#FFE66D",
              "button_text": "#222222",
              "button_anim": "shimmer"
            },
            "style_yellow_clean": {
              "layout": "reminder_day",
              "button_bg": "#FFE66D",
              "button_text": "#222222",
              "button_anim": "shimmer"
            }
          }
        }
    """.trimIndent()

    /** FRC setDefaults / 运营对照用的双分段外壳（organic=paid=送审段）。 */
    private val DEFAULT_CONFIG_JSON: String by lazy {
        DualSegmentJson.wrapIdentical(DEFAULT_SEGMENT_JSON)
    }

    private const val KEY_FIRST_SEEN_AT = "local_push_first_seen_at"
    private const val KEY_FIRST_24_HOURS_GLOBAL = "local_push_first_24_hours_global"
    private const val KEY_GLOBAL_LAST_SHOWN_AT = "local_push_global_last_shown_at"
    private const val KEY_DAILY_DATE = "local_push_daily_date"
    private const val KEY_DAILY_GLOBAL = "local_push_daily_global"
    private const val KEY_PREFIX_BUCKET_DAILY = "local_push_daily_bucket_"
    private const val KEY_PREFIX_SCENE_DAILY = "local_push_daily_scene_"
    private const val KEY_PREFIX_BUCKET_LAST = "local_push_last_bucket_"
    private const val KEY_PREFIX_SCENE_LAST = "local_push_last_scene_"
    private const val KEY_PREFIX_TEMPLATE_INDEX = "local_push_template_index_"
    private const val KEY_PREFIX_TEMPLATE_USED = "local_push_template_used_"

    private val fallbackConfig = Config()
    private val applyLock = Any()

    enum class ConfigOrigin {
        /** 进程内置 DEFAULT_SEGMENT_JSON，尚未被 FRC 覆盖 */
        BUILTIN,
        /** Firebase setDefaults 的本地兜底（Console 未配该 key 时常见） */
        FRC_DEFAULT,
        /** Firebase Console 真正下发的远端值 */
        REMOTE,
    }

    @Volatile
    private var config: Config = parseConfig(DEFAULT_SEGMENT_JSON) ?: fallbackConfig

    @Volatile
    private var appliedRaw: String = DEFAULT_SEGMENT_JSON

    @Volatile
    private var origin: ConfigOrigin = ConfigOrigin.BUILTIN

    init {
        LogUtils.i(
            TAG,
            "本地推送配置初始化（内置 organic 单段）：origin=$origin version=${config.version} 总开关=${config.enabled} " +
                "bucket数量=${config.buckets.size} scene数量=${config.scenes.size} template数量=${config.templates.size}",
        )
    }

    /** 双分段外壳 JSON，供 FRC defaults / 文档同步。 */
    fun defaultConfigJson(): String = DEFAULT_CONFIG_JSON

    fun currentOrigin(): ConfigOrigin = origin

    fun currentConfig(): Config = config

    fun eventTypeForSceneKey(sceneKey: String): LocalEventPushHelper.EventType? {
        return LocalEventPushHelper.EventType.entries.firstOrNull { sceneKey(it) == sceneKey }
    }

    /**
     * 用 FRC / 本地 JSON 更新内存快照。
     * @return true 表示快照相对之前有变化（调用方据此决定是否热重启调度）
     */
    fun applyFromJson(raw: String, newOrigin: ConfigOrigin = ConfigOrigin.BUILTIN): Boolean {
        val normalized = raw.trim()
        if (normalized.isEmpty()) {
            LogUtils.e(TAG, "applyFromJson 失败：JSON 为空，保留当前快照 origin=$origin")
            return false
        }
        synchronized(applyLock) {
            if (normalized == appliedRaw) {
                val originChanged = origin != newOrigin
                if (originChanged) {
                    LogUtils.i(
                        TAG,
                        "applyFromJson：JSON 内容未变，但来源标记更新 $origin -> $newOrigin",
                    )
                    origin = newOrigin
                }
                LogUtils.i(
                    TAG,
                    "applyFromJson 跳过内容更新：origin=$origin length=${normalized.length} " +
                        "version=${config.version} enabled=${config.enabled} " +
                        "（内容相同≠已拉到新远端；请看 origin 是否为 REMOTE）",
                )
                return false
            }
            val parsed = parseConfig(normalized)
            if (parsed == null) {
                LogUtils.e(
                    TAG,
                    "applyFromJson 失败：解析失败，保留当前快照 origin=$origin length=${normalized.length}",
                )
                return false
            }
            val old = config
            val oldOrigin = origin
            config = parsed
            appliedRaw = normalized
            origin = newOrigin
            LogUtils.i(
                TAG,
                "applyFromJson 成功：origin $oldOrigin -> $newOrigin；旧 version=${old.version} enabled=${old.enabled} " +
                    "buckets=${old.buckets.size}/scenes=${old.scenes.size}/templates=${old.templates.size} -> " +
                    "新 version=${parsed.version} enabled=${parsed.enabled} " +
                    "buckets=${parsed.buckets.size}/scenes=${parsed.scenes.size}/templates=${parsed.templates.size} " +
                    "jsonLength=${normalized.length}",
            )
            LogUtils.i(
                TAG,
                "applyFromJson 详情：origin=$origin bucketKeys=${parsed.buckets.keys} sceneKeys=${parsed.scenes.keys} " +
                    "periodicBuckets=${parsed.buckets.filter { it.value.enabled && it.value.intervalSec >= 0 }.mapValues { it.value.intervalSec }}",
            )
            return true
        }
    }

    private fun parseConfig(raw: String): Config? {
        return runCatching { Gson().fromJson(raw, Config::class.java) }
            .onSuccess {
                LogUtils.i(
                    TAG,
                    "本地推送配置解析成功：version=${it.version} 总开关=${it.enabled} " +
                        "bucket数量=${it.buckets.size} scene数量=${it.scenes.size} template数量=${it.templates.size}",
                )
            }
            .onFailure {
                LogUtils.e(
                    TAG,
                    "本地推送配置解析失败：错误=${it.javaClass.simpleName}，信息=${it.message.orEmpty()}",
                )
            }
            .getOrNull()
    }

    fun sceneConfig(context: Context, type: LocalEventPushHelper.EventType): SceneRuntime? {
        ensureMmkv(context)
        val sceneKey = sceneKey(type)
        LogUtils.i(TAG, "开始读取场景配置：事件=${type.name} 映射scene=$sceneKey")
        val scene = config.scenes[sceneKey]
        if (scene == null) {
            LogUtils.w(TAG, "场景配置不存在：事件=${type.name} scene=$sceneKey，跳过本地推送")
            return null
        }
        val bucket = config.buckets[scene.bucket]
        if (bucket == null) {
            LogUtils.w(TAG, "桶配置不存在：scene=$sceneKey bucket=${scene.bucket}，跳过本地推送")
            return null
        }
        val template = selectTemplate(sceneKey, scene, bucket)
        LogUtils.i(
            TAG,
            "场景配置读取完成：scene=$sceneKey scene开关=${scene.enabled} bucket=${scene.bucket} " +
                "bucket开关=${bucket.enabled} periodic=${scene.periodic} priority=${scene.priority} scene每日上限=${scene.dailyMax} " +
                "scene冷却=${scene.cooldownSec}s bucket每日上限=${bucket.dailyMax} bucket冷却=${bucket.cooldownSec}s " +
                "bucket复用次数=${bucket.repetitions} templateLayout=${template.layout} buttonBg=${template.buttonBg} " +
                "buttonText=${template.buttonText} buttonAnim=${template.buttonAnim}",
        )
        return SceneRuntime(
            sceneKey = sceneKey,
            scene = scene,
            bucketKey = scene.bucket,
            bucket = bucket,
            template = template,
            globalMaxVisible = config.global.maxVisible,
        )
    }

    fun periodicBucketKeys(): List<String> {
        val keys = config.buckets
            .filter { (_, bucket) -> bucket.enabled && bucket.intervalSec >= 0 }
            .keys
            .toList()
        LogUtils.i(TAG, "读取周期调度桶：keys=$keys")
        return keys
    }

    fun bucketIntervalSec(bucketKey: String): Int {
        val interval = config.buckets[bucketKey]?.intervalSec ?: -1
        LogUtils.i(TAG, "读取桶周期间隔：bucket=$bucketKey intervalSec=$interval")
        return interval
    }

    fun eventTypesForBucket(bucketKey: String): List<LocalEventPushHelper.EventType> {
        val result = LocalEventPushHelper.EventType.entries
            .mapNotNull { type ->
                val sceneKey = sceneKey(type)
                val scene = config.scenes[sceneKey] ?: return@mapNotNull null
                if (!scene.enabled || scene.bucket != bucketKey || !scene.periodic) {
                    LogUtils.i(
                        TAG,
                        "周期调度过滤场景：bucket=$bucketKey type=${type.name} scene=$sceneKey " +
                            "sceneEnabled=${scene.enabled} sceneBucket=${scene.bucket} periodic=${scene.periodic}",
                    )
                    return@mapNotNull null
                }
                type to scene.priority
            }
            .sortedByDescending { it.second }
            .map { it.first }
        LogUtils.i(TAG, "读取桶内场景优先级队列：bucket=$bucketKey types=${result.map { it.name }}")
        return result
    }

    fun canShow(context: Context, runtime: SceneRuntime, appInForeground: Boolean): Boolean {
        ensureMmkv(context)
        LogUtils.i(
            TAG,
            "开始判断是否允许展示：scene=${runtime.sceneKey} bucket=${runtime.bucketKey} " +
                "app前台=$appInForeground 总开关=${config.enabled} scene开关=${runtime.scene.enabled} bucket开关=${runtime.bucket.enabled}",
        )
        if (!config.enabled) {
            LogUtils.w(TAG, "拦截本地推送：总开关关闭 scene=${runtime.sceneKey}")
            return false
        }
        if (!runtime.scene.enabled) {
            LogUtils.w(TAG, "拦截本地推送：场景开关关闭 scene=${runtime.sceneKey}")
            return false
        }
        if (!runtime.bucket.enabled) {
            LogUtils.w(TAG, "拦截本地推送：桶开关关闭 scene=${runtime.sceneKey} bucket=${runtime.bucketKey}")
            return false
        }
        if (config.global.skipForeground && appInForeground) {
            LogUtils.w(TAG, "拦截本地推送：App当前在前台，且skip_foreground=true scene=${runtime.sceneKey}")
            return false
        }
        // app_background：产品要求刚装也可触发，不受 global.install_age_sec 限制
        if (runtime.sceneKey != "app_background" && !hasReachedInstallAge(context)) {
            LogUtils.w(
                TAG,
                "拦截本地推送：安装/首次初始化时长未达到 install_age_sec=${config.global.installAgeSec}s scene=${runtime.sceneKey}",
            )
            return false
        }

        resetDailyIfNeeded()
        val now = System.currentTimeMillis()
        val store = MMKV.defaultMMKV()
        val globalQuota = globalQuota(context, now)
        val globalCountKey = when (globalQuota.period) {
            GlobalPushQuota.Period.FIRST_24_HOURS -> KEY_FIRST_24_HOURS_GLOBAL
            GlobalPushQuota.Period.DAILY -> KEY_DAILY_GLOBAL
        }
        val globalCount = store.getInt(globalCountKey, 0)
        val globalDaily = store.getInt(KEY_DAILY_GLOBAL, 0)
        val bucketDaily = store.getInt(KEY_PREFIX_BUCKET_DAILY + runtime.bucketKey, 0)
        val sceneDaily = store.getInt(KEY_PREFIX_SCENE_DAILY + runtime.sceneKey, 0)

        if (!underLimit("全局展示上限:${globalQuota.period}", globalQuota.max, globalCount)) return false
        if (!underLimit("桶每日上限:${runtime.bucketKey}", runtime.bucket.dailyMax, bucketDaily)) return false
        if (!underLimit("场景每日上限:${runtime.sceneKey}", runtime.scene.dailyMax, sceneDaily)) return false

        if (!cooldownPassed("全局冷却", KEY_GLOBAL_LAST_SHOWN_AT, config.global.cooldownSec, now)) return false
        if (!cooldownPassed("桶冷却:${runtime.bucketKey}", KEY_PREFIX_BUCKET_LAST + runtime.bucketKey, runtime.bucket.cooldownSec, now)) return false
        if (!cooldownPassed("场景冷却:${runtime.sceneKey}", KEY_PREFIX_SCENE_LAST + runtime.sceneKey, runtime.scene.cooldownSec, now)) return false

        LogUtils.i(
            TAG,
            "允许展示本地推送：scene=${runtime.sceneKey} bucket=${runtime.bucketKey} " +
                "globalPeriod=${globalQuota.period} globalCount=$globalCount/${globalQuota.max} " +
                "globalDaily=$globalDaily/${config.global.dailyMax} bucketDaily=$bucketDaily/${runtime.bucket.dailyMax} sceneDaily=$sceneDaily/${runtime.scene.dailyMax}",
        )
        return true
    }

    fun markShown(context: Context, runtime: SceneRuntime) {
        ensureMmkv(context)
        resetDailyIfNeeded()
        val store = MMKV.defaultMMKV()
        val now = System.currentTimeMillis()
        val globalQuota = globalQuota(context, now)
        store.putLong(KEY_GLOBAL_LAST_SHOWN_AT, now)
        store.putLong(KEY_PREFIX_BUCKET_LAST + runtime.bucketKey, now)
        store.putLong(KEY_PREFIX_SCENE_LAST + runtime.sceneKey, now)
        when (globalQuota.period) {
            GlobalPushQuota.Period.FIRST_24_HOURS -> store.putInt(
                KEY_FIRST_24_HOURS_GLOBAL,
                store.getInt(KEY_FIRST_24_HOURS_GLOBAL, 0) + 1,
            )
            GlobalPushQuota.Period.DAILY -> store.putInt(
                KEY_DAILY_GLOBAL,
                store.getInt(KEY_DAILY_GLOBAL, 0) + 1,
            )
        }
        store.putInt(
            KEY_PREFIX_BUCKET_DAILY + runtime.bucketKey,
            store.getInt(KEY_PREFIX_BUCKET_DAILY + runtime.bucketKey, 0) + 1,
        )
        store.putInt(
            KEY_PREFIX_SCENE_DAILY + runtime.sceneKey,
            store.getInt(KEY_PREFIX_SCENE_DAILY + runtime.sceneKey, 0) + 1,
        )
        markTemplateUsed(runtime.sceneKey, runtime.bucket)
        LogUtils.i(
            TAG,
            "记录展示成功：scene=${runtime.sceneKey} bucket=${runtime.bucketKey} now=$now " +
                "globalPeriod=${globalQuota.period} globalFirst24h=${store.getInt(KEY_FIRST_24_HOURS_GLOBAL, 0)} " +
                "globalDaily=${store.getInt(KEY_DAILY_GLOBAL, 0)} " +
                "bucketDaily=${store.getInt(KEY_PREFIX_BUCKET_DAILY + runtime.bucketKey, 0)} " +
                "sceneDaily=${store.getInt(KEY_PREFIX_SCENE_DAILY + runtime.sceneKey, 0)}",
        )
    }

    fun colorOrNull(raw: String?): Int? {
        return runCatching {
            if (raw.isNullOrBlank()) null else Color.parseColor(raw)
        }.onFailure {
            LogUtils.w(TAG, "颜色解析失败：raw=$raw 错误=${it.javaClass.simpleName} 信息=${it.message.orEmpty()}")
        }.getOrNull()
    }

    fun sceneKey(type: LocalEventPushHelper.EventType): String {
        return when (type) {
            LocalEventPushHelper.EventType.SCREENSHOT -> "screenshot_capture"
            LocalEventPushHelper.EventType.APP_INSTALL -> "app_install"
            LocalEventPushHelper.EventType.APP_UNINSTALL -> "app_uninstall"
            LocalEventPushHelper.EventType.BATTERY_BELOW_50 -> "battery_below_50"
            LocalEventPushHelper.EventType.BATTERY_BELOW_20 -> "battery_below_20"
            LocalEventPushHelper.EventType.CHARGING -> "charging"
            LocalEventPushHelper.EventType.CHARGING_FULL -> "charging_full"
            LocalEventPushHelper.EventType.CHARGING_TO_80 -> "charging_to_80"
            LocalEventPushHelper.EventType.CHARGING_DISCONNECT -> "charging_disconnect"
            LocalEventPushHelper.EventType.APP_UPDATE -> "app_update"
            LocalEventPushHelper.EventType.NETWORK_CHANGE -> "network_change"
            LocalEventPushHelper.EventType.SCREEN_LOCK -> "screen_lock"
            LocalEventPushHelper.EventType.SCREEN_ON_AFTER_LOCK -> "screen_on_after_lock"
            LocalEventPushHelper.EventType.STORAGE_FREE_DROP -> "storage_free_drop"
            LocalEventPushHelper.EventType.SCHEDULED_PUSH_1 -> "scheduled_push_1"
            LocalEventPushHelper.EventType.SCHEDULED_PUSH_2 -> "scheduled_push_2"
            LocalEventPushHelper.EventType.SCHEDULED_PUSH_3 -> "scheduled_push_3"
            LocalEventPushHelper.EventType.APP_INACTIVE -> "app_inactive"
            LocalEventPushHelper.EventType.WIRED_HEADSET_CONNECT -> "wired_headset_connect"
            LocalEventPushHelper.EventType.WIRED_HEADSET_DISCONNECT -> "wired_headset_disconnect"
            LocalEventPushHelper.EventType.BLUETOOTH_CONNECT -> "bluetooth_connect"
            LocalEventPushHelper.EventType.BLUETOOTH_DISCONNECT -> "bluetooth_disconnect"
            LocalEventPushHelper.EventType.AUDIO_FOCUS_LOST -> "audio_focus_lost"
            LocalEventPushHelper.EventType.VOLUME_ABOVE_50 -> "volume_above_50"
            LocalEventPushHelper.EventType.VOLUME_BELOW_20 -> "volume_below_20"
            LocalEventPushHelper.EventType.USER_UNLOCK -> "user_unlock"
            LocalEventPushHelper.EventType.BOOT_COMPLETED -> "boot_completed"
            LocalEventPushHelper.EventType.PLAYBACK_PAUSED -> "playback_paused"
            LocalEventPushHelper.EventType.PLAYBACK_BACKGROUND -> "playback_background"
            LocalEventPushHelper.EventType.APP_BACKGROUND -> "app_background"
            LocalEventPushHelper.EventType.AUDIO_DOWNLOAD -> "audio_download"
        }
    }

    private fun selectTemplate(sceneKey: String, scene: Scene, bucket: Bucket): Template {
        val ids = scene.templateIds.filter { config.templates.containsKey(it) }
        if (ids.isEmpty()) {
            LogUtils.w(TAG, "模板列表为空或模板不存在：scene=$sceneKey 原始templateIds=${scene.templateIds}，使用默认样式")
            return Template()
        }
        val store = MMKV.defaultMMKV()
        val indexKey = KEY_PREFIX_TEMPLATE_INDEX + sceneKey
        val safeIndex = store.getInt(indexKey, 0).floorMod(ids.size)
        val templateId = ids[safeIndex]
        val used = store.getInt(KEY_PREFIX_TEMPLATE_USED + sceneKey, 0)
        val template = config.templates[templateId] ?: Template()
        LogUtils.i(
            TAG,
            "选择样式模板：scene=$sceneKey templateId=$templateId index=$safeIndex/${ids.size} " +
                "当前已复用次数=$used bucketRepetitions=${bucket.repetitions} layout=${template.layout} " +
                "buttonBg=${template.buttonBg} buttonText=${template.buttonText} buttonAnim=${template.buttonAnim}",
        )
        return template
    }

    private fun markTemplateUsed(sceneKey: String, bucket: Bucket) {
        val store = MMKV.defaultMMKV()
        val runtime = config.scenes[sceneKey] ?: return
        val templateCount = runtime.templateIds.count { config.templates.containsKey(it) }
        if (templateCount <= 1) {
            LogUtils.i(TAG, "模板复用计数无需轮换：scene=$sceneKey 有效模板数量=$templateCount")
            return
        }
        val repetitions = bucket.repetitions.coerceAtLeast(1)
        val usedKey = KEY_PREFIX_TEMPLATE_USED + sceneKey
        val indexKey = KEY_PREFIX_TEMPLATE_INDEX + sceneKey
        val used = store.getInt(usedKey, 0) + 1
        if (used >= repetitions) {
            val nextIndex = (store.getInt(indexKey, 0) + 1).floorMod(templateCount)
            store.putInt(indexKey, nextIndex)
            store.putInt(usedKey, 0)
            LogUtils.i(
                TAG,
                "模板达到复用次数，切换到下一个：scene=$sceneKey repetitions=$repetitions nextIndex=$nextIndex templateCount=$templateCount",
            )
        } else {
            store.putInt(usedKey, used)
            LogUtils.i(
                TAG,
                "模板复用计数增加：scene=$sceneKey used=$used/$repetitions templateCount=$templateCount",
            )
        }
    }

    private fun hasReachedInstallAge(context: Context): Boolean {
        val firstSeenAt = installStartedAt(context)
        val elapsedMs = System.currentTimeMillis() - firstSeenAt
        val requiredMs = config.global.installAgeSec * 1000L
        val passed = elapsedMs >= requiredMs
        LogUtils.i(
            TAG,
            "安装时长判断：installStartedAt=$firstSeenAt elapsed=${elapsedMs}ms required=${requiredMs}ms install_age_sec=${config.global.installAgeSec} 通过=$passed",
        )
        return passed
    }

    private fun globalQuota(context: Context, now: Long): GlobalPushQuota.Selection {
        val installStartedAt = installStartedAt(context)
        val installAgeMs = (now - installStartedAt).coerceAtLeast(0L)
        return GlobalPushQuota.select(
            firstMax = config.global.firstMax,
            dailyMax = config.global.dailyMax,
            installAgeMs = installAgeMs,
        ).also { quota ->
            LogUtils.i(
                TAG,
                "全局展示窗口判断：installStartedAt=$installStartedAt installAgeMs=$installAgeMs " +
                    "period=${quota.period} max=${quota.max}",
            )
        }
    }

    private fun installStartedAt(context: Context): Long {
        val packageInstallTime = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
        if (packageInstallTime > 0L) {
            return packageInstallTime
        }
        val store = MMKV.defaultMMKV()
        val firstSeenAt = store.getLong(KEY_FIRST_SEEN_AT, 0L).let {
            if (it > 0L) it else System.currentTimeMillis().also { now ->
                store.putLong(KEY_FIRST_SEEN_AT, now)
                LogUtils.i(TAG, "首次初始化本地推送安装时长基准：firstSeenAt=$now")
            }
        }
        return firstSeenAt
    }

    private fun cooldownPassed(label: String, key: String, cooldownSec: Int, now: Long): Boolean {
        if (cooldownSec < 0) {
            LogUtils.i(TAG, "$label：cooldownSec=$cooldownSec，表示不限制，通过")
            return true
        }
        val last = MMKV.defaultMMKV().getLong(key, 0L)
        val elapsedMs = now - last
        val requiredMs = cooldownSec * 1000L
        val passed = elapsedMs >= requiredMs
        LogUtils.i(
            TAG,
            "$label 判断：key=$key last=$last elapsed=${elapsedMs}ms required=${requiredMs}ms cooldownSec=${cooldownSec}s 通过=$passed",
        )
        if (!passed) {
            LogUtils.w(TAG, "拦截本地推送：$label 未通过，还需等待 ${requiredMs - elapsedMs}ms")
        }
        return passed
    }

    private fun underLimit(label: String, limit: Int, count: Int): Boolean {
        val passed = limit < 0 || count < limit
        LogUtils.i(TAG, "$label 判断：count=$count limit=$limit 通过=$passed")
        if (!passed) {
            LogUtils.w(TAG, "拦截本地推送：$label 已达到上限 count=$count limit=$limit")
        }
        return passed
    }

    private fun resetDailyIfNeeded() {
        val store = MMKV.defaultMMKV()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val saved = store.getString(KEY_DAILY_DATE, "")
        if (saved == today) {
            LogUtils.i(TAG, "每日计数无需重置：today=$today saved=$saved")
            return
        }
        LogUtils.i(TAG, "每日计数重置：旧日期=$saved 新日期=$today，清空global/bucket/scene每日计数")
        store.putString(KEY_DAILY_DATE, today)
        store.putInt(KEY_DAILY_GLOBAL, 0)
        config.buckets.keys.forEach { store.putInt(KEY_PREFIX_BUCKET_DAILY + it, 0) }
        config.scenes.keys.forEach { store.putInt(KEY_PREFIX_SCENE_DAILY + it, 0) }
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }

    private fun Int.floorMod(divisor: Int): Int {
        return ((this % divisor) + divisor) % divisor
    }

    data class SceneRuntime(
        val sceneKey: String,
        val scene: Scene,
        val bucketKey: String,
        val bucket: Bucket,
        val template: Template,
        val globalMaxVisible: Int,
    )

    data class Config(
        @SerializedName("version") val version: Int = 1,
        @SerializedName("enabled") val enabled: Boolean = true,
        @SerializedName("global") val global: Global = Global(),
        @SerializedName("buckets") val buckets: Map<String, Bucket> = emptyMap(),
        @SerializedName("scenes") val scenes: Map<String, Scene> = emptyMap(),
        @SerializedName("templates") val templates: Map<String, Template> = emptyMap(),
    )

    data class Global(
        @SerializedName("first_max") val firstMax: Int? = null,
        @SerializedName("daily_max") val dailyMax: Int = 99,
        @SerializedName("cooldown_sec") val cooldownSec: Int = 120,
        @SerializedName("max_visible") val maxVisible: Int = 3,
        @SerializedName("skip_foreground") val skipForeground: Boolean = true,
        @SerializedName("install_age_sec") val installAgeSec: Int = 300,
    )

    data class Bucket(
        @SerializedName("enabled") val enabled: Boolean = true,
        @SerializedName("interval_sec") val intervalSec: Int = -1,
        @SerializedName("daily_max") val dailyMax: Int = -1,
        @SerializedName("cooldown_sec") val cooldownSec: Int = -1,
        @SerializedName("repetitions") val repetitions: Int = 1,
        @SerializedName("max_visible") val maxVisible: Int = 1,
    )

    data class Scene(
        @SerializedName("enabled") val enabled: Boolean = true,
        @SerializedName("bucket") val bucket: String = "",
        @SerializedName("periodic") val periodic: Boolean = false,
        @SerializedName("priority") val priority: Int = 0,
        @SerializedName("daily_max") val dailyMax: Int = -1,
        @SerializedName("cooldown_sec") val cooldownSec: Int = -1,
        @SerializedName("template_ids") val templateIds: List<String> = emptyList(),
        @SerializedName("router") val router: String = "",
        /** 定点推送时刻，本地时区 `HH:mm`；空列表 = 非定点场景 */
        @SerializedName("times") val times: List<String> = emptyList(),
        /** 满足场景条件后延迟展示秒数；&lt;0 表示未配置，由客户端用场景默认值（当前为 5） */
        @SerializedName("delay_sec") val delaySec: Int = -1,
    )

    data class Template(
        @SerializedName("layout") val layout: String = "small",
        @SerializedName("button_bg") val buttonBg: String = "",
        @SerializedName("button_text") val buttonText: String = "",
        @SerializedName("button_anim") val buttonAnim: String = "shimmer",
    )
}
