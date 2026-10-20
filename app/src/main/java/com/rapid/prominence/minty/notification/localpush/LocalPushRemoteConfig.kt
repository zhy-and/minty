package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import com.rapid.prominence.minty.BuildConfig


import com.rapid.prominence.minty.runtime.UserSegmentHelper
import com.blankj.utilcode.util.LogUtils
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 本地推送 FRC 拉取封装。
 *
 * - Key [KEY_LOCAL_PUSH_CONFIG]：双分段外壳 `{ version, organic, paid }`
 * - 选段应用由 [UserSegmentHelper] 门禁触发；冷/热启动可并行 fetch
 * - [RefreshReason.COLD_START]：本进程冷启只 fetch 一次（挡住 Bootstrap 连调）
 * - [RefreshReason.HOT_START]：每次热启都可 fetch，仅 in-flight 去重；与冷启门禁互不影响
 * - 真网间隔由 [LocalPushRemoteConfig] 的 minimumFetchInterval 控制（Debug=0 / Release=3600）
 *
 * **临时联调**：[FORCE_LOCAL_JSON]=true 时不走 FRC，只用内置默认 JSON；测完请改回 false。
 */
object LocalPushRemoteConfig {
    private const val TAG = "LocalPushRemoteConfig"
    const val KEY_LOCAL_PUSH_CONFIG = "local_push_config_v1"

    /**
     * TODO(测完改回 false)：强制走 [LocalPushConfig.defaultConfigJson]，跳过 FRC fetch/activate。
     * 内置段频控较松（daily_max 高、cooldown/install_age=0），便于本地场景联调。
     */
    private const val FORCE_LOCAL_JSON = false

    enum class RefreshReason {
        COLD_START,
        HOT_START,
    }

    /** 本进程冷启只发起一次；下次真冷启（新进程）自然清零。 */
    private val coldFetchStarted = AtomicBoolean(false)
    /** 热启进行中锁：结束后下次热启可再拉。 */
    private val hotFetchInFlight = AtomicBoolean(false)
    private val segmentListenerRegistered = AtomicBoolean(false)

    fun refreshIfNeeded(
        context: Context,
        reason: RefreshReason = RefreshReason.COLD_START,
    ) {
        val appContext = context.applicationContext
        if (segmentListenerRegistered.compareAndSet(false, true)) {
            UserSegmentHelper.addOnResolvedListener { applyForCurrentSegment(appContext) }
        }

        if (FORCE_LOCAL_JSON) {
            LogUtils.w(
                TAG,
                "FORCE_LOCAL_JSON=true：跳过 FRC，reason=$reason，应用内置默认 JSON",
            )
            applyLocalDefaultJson(appContext)
            return
        }

        ensureInitialized()

        when (reason) {
            RefreshReason.COLD_START -> {
                if (!coldFetchStarted.compareAndSet(false, true)) {
                    LogUtils.i(
                        TAG,
                        "跳过 FRC 拉取：reason=$reason 本进程冷启已拉过，当前生效 origin=${LocalPushConfig.currentOrigin()}",
                    )
                    return
                }
            }
            RefreshReason.HOT_START -> {
                if (!hotFetchInFlight.compareAndSet(false, true)) {
                    LogUtils.i(
                        TAG,
                        "跳过 FRC 拉取：reason=$reason 已有热启拉取进行中，当前生效 origin=${LocalPushConfig.currentOrigin()}",
                    )
                    return
                }
            }
        }

        if (UserSegmentHelper.isResolved()) {
            applyForCurrentSegment(appContext)
        } else {
            LogUtils.i(TAG, "分段未锁定：先保持内置 organic 单段，FRC fetch 并行进行")
        }

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        LogUtils.i(
            TAG,
            "发起 FRC fetchAndActivate：reason=$reason key=$KEY_LOCAL_PUSH_CONFIG " +
                "minimumFetchIntervalSec=${if (BuildConfig.DEBUG) 0L else 3600L} debug=${BuildConfig.DEBUG}",
        )
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { activated ->
                val info = remoteConfig.info
                LogUtils.i(
                    TAG,
                    "FRC fetchAndActivate 完成：reason=$reason activated=$activated " +
                        "lastFetchStatus=${fetchStatusLabel(info.lastFetchStatus)} " +
                        "lastFetchTimeMs=${info.fetchTimeMillis}",
                )
                if (UserSegmentHelper.isResolved()) {
                    val changed = applyForCurrentSegment(appContext)
                    logApplyOutcome(reason, changed)
                } else {
                    LogUtils.i(TAG, "分段尚未锁定，已缓存 FRC，等待 UserSegmentHelper 门禁应用")
                }
            }
            .addOnFailureListener { error ->
                LogUtils.e(
                    TAG,
                    "FRC fetchAndActivate 失败：reason=$reason type=${error.javaClass.simpleName} " +
                        "message=${error.message.orEmpty()}，继续使用 LocalPushConfig 当前快照 " +
                        "origin=${LocalPushConfig.currentOrigin()}",
                )
                if (UserSegmentHelper.isResolved()) {
                    applyForCurrentSegment(appContext)
                }
            }
            .addOnCompleteListener {
                if (reason == RefreshReason.HOT_START) {
                    hotFetchInFlight.set(false)
                }
                LogUtils.i(
                    TAG,
                    "FRC 拉取结束：reason=$reason；最终生效 origin=${LocalPushConfig.currentOrigin()} " +
                        "segmentResolved=${UserSegmentHelper.isResolved()}",
                )
            }
    }

    /**
     * 按当前分段从已激活 FRC（或 defaults）取出子 JSON 并应用到 [LocalPushConfig]。
     * @return true 表示快照有变化（已热重启调度）
     */
    fun applyForCurrentSegment(context: Context): Boolean {
        val appContext = context.applicationContext
        if (FORCE_LOCAL_JSON) {
            LogUtils.w(TAG, "FORCE_LOCAL_JSON=true：applyForCurrentSegment 改用内置默认 JSON")
            return applyLocalDefaultJson(appContext)
        }

        ensureInitialized()

        val preferPaid = UserSegmentHelper.isResolved() && UserSegmentHelper.currentSegmentKey() == "paid"
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val value = remoteConfig.getValue(KEY_LOCAL_PUSH_CONFIG)
        val valueSource = valueSourceLabel(value.source)
        val origin = when (value.source) {
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> LocalPushConfig.ConfigOrigin.REMOTE
            FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> LocalPushConfig.ConfigOrigin.FRC_DEFAULT
            else -> LocalPushConfig.ConfigOrigin.BUILTIN
        }
        var raw = value.asString()
        if (raw.isBlank()) {
            raw = LocalPushConfig.defaultConfigJson()
            LogUtils.w(TAG, "FRC 值为空，改用内置双分段默认")
        }

        val segmentJson = DualSegmentJson.extract(raw, preferPaid = preferPaid)
            ?: DualSegmentJson.extract(
                LocalPushConfig.defaultConfigJson(),
                preferPaid = false,
            )
        if (segmentJson.isNullOrBlank()) {
            LogUtils.e(TAG, "分段 JSON 为空，保留当前 LocalPushConfig 快照")
            return false
        }

        LogUtils.i(
            TAG,
            "应用分段配置：preferPaid=$preferPaid segment=${UserSegmentHelper.currentSegmentKey()} " +
                "resolved=${UserSegmentHelper.isResolved()} valueSource=$valueSource " +
                "mappedOrigin=$origin wrapperLen=${raw.length} segmentLen=${segmentJson.length} " +
                "preview=${preview(segmentJson)}",
        )

        return commitSegmentJson(appContext, segmentJson, origin)
    }

    /** 强制内置默认：按当前分段取 organic/paid，origin 固定 BUILTIN。 */
    private fun applyLocalDefaultJson(context: Context): Boolean {
        val preferPaid = UserSegmentHelper.isResolved() && UserSegmentHelper.currentSegmentKey() == "paid"
        val wrapper = LocalPushConfig.defaultConfigJson()
        val segmentJson = DualSegmentJson.extract(wrapper, preferPaid = preferPaid)
            ?: DualSegmentJson.extract(wrapper, preferPaid = false)
        if (segmentJson.isNullOrBlank()) {
            LogUtils.e(TAG, "内置默认分段 JSON 为空，保留当前快照")
            return false
        }
        LogUtils.i(
            TAG,
            "应用内置默认 JSON：preferPaid=$preferPaid segment=${UserSegmentHelper.currentSegmentKey()} " +
                "resolved=${UserSegmentHelper.isResolved()} segmentLen=${segmentJson.length} " +
                "preview=${preview(segmentJson)}",
        )
        return commitSegmentJson(context, segmentJson, LocalPushConfig.ConfigOrigin.BUILTIN)
    }

    private fun commitSegmentJson(
        context: Context,
        segmentJson: String,
        origin: LocalPushConfig.ConfigOrigin,
    ): Boolean {
        val changed = LocalPushConfig.applyFromJson(segmentJson, origin)
        if (changed) {
            LogUtils.i(TAG, "配置快照已更新，热重启 LocalPushScheduler + LocalPushAlarmScheduler")
            LocalPushScheduler.restart(context.applicationContext)
            LocalPushAlarmScheduler.restart(context.applicationContext)
        }
        return changed
    }

    private fun logApplyOutcome(reason: RefreshReason, changed: Boolean) {
        val origin = LocalPushConfig.currentOrigin()
        when {
            changed -> LogUtils.i(TAG, "配置已切换：reason=$reason origin=$origin")
            origin == LocalPushConfig.ConfigOrigin.REMOTE ->
                LogUtils.i(TAG, "远端 JSON 与快照一致，不重启调度 reason=$reason")
            else ->
                LogUtils.w(
                    TAG,
                    "当前仍是本地兜底（origin=$origin，reason=$reason）。" +
                        "常见原因：Console 未配置 key=$KEY_LOCAL_PUSH_CONFIG。",
                )
        }
    }

    private val initialized = AtomicBoolean(false)

    private fun ensureInitialized() {
        if (!initialized.compareAndSet(false, true)) return
        val config = FirebaseRemoteConfig.getInstance()
        config.setConfigSettingsAsync(
            com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                .build(),
        )
        config.setDefaultsAsync(mapOf(KEY_LOCAL_PUSH_CONFIG to LocalPushConfig.defaultConfigJson()))
    }

    private fun valueSourceLabel(source: Int): String {
        return when (source) {
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "REMOTE"
            FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "DEFAULT"
            FirebaseRemoteConfig.VALUE_SOURCE_STATIC -> "STATIC"
            else -> "UNKNOWN($source)"
        }
    }

    private fun fetchStatusLabel(status: Int): String {
        return when (status) {
            FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS -> "SUCCESS"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE -> "FAILURE"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED -> "THROTTLED"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET -> "NO_FETCH_YET"
            else -> "UNKNOWN($status)"
        }
    }

    private fun preview(raw: String, max: Int = 160): String {
        val compact = raw.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= max) compact else compact.take(max) + "…"
    }
}
