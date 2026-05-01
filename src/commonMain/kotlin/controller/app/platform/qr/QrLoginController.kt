package controller.app.platform.qr

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.client.dto.ConfirmQrSceneResponse
import com.netonstream.privchat.application.module.privchat.client.dto.CreateQrSceneRequest
import com.netonstream.privchat.application.module.privchat.client.dto.QrSceneResponse
import com.netonstream.privchat.application.module.privchat.client.dto.QrSceneStatus
import com.netonstream.privchat.application.module.privchat.client.dto.RejectQrSceneResponse
import com.netonstream.privchat.application.module.privchat.client.dto.ScanQrSceneResponse
import com.netonstream.privchat.application.module.privchat.client.dto.WebDeviceInfoInput
import controller.admin.auth.dto.LoginDeviceInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logic.MemberAuthLogic
import controller.app.auth.dto.MemberLoginResponse
import neton.core.annotations.AllowAnonymous
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Query
import neton.core.interfaces.Identity
import neton.logging.Logger
import neton.logging.LoggerFactory

/**
 * Application 侧 Web 扫码登录编排（spec MODULE_PRIVCHAT_SPEC §3.3 + QR_API §5）。
 *
 * 路由：
 * - POST /platform/qr-login/scenes  (匿名)：Web 申请二维码
 * - GET  /platform/qr-login/scenes  (匿名)：Web 兜底轮询（正常路径走 server unauth RPC 推送）
 * - POST /platform/qr-login/scan    (member token)：scanner_uid 取自 token
 * - POST /platform/qr-login/confirm (member token)：成功后 application 给 Web 设备签发
 *   登录返回，并显式调用 server `push_qr_login_authorized` 把 `MemberLoginResponse`
 *   原样推回 Web 的 unauth 连接（spec QR_API §5）
 * - POST /platform/qr-login/reject  (member token)
 *
 * 控制器从 `:main` 模块迁来本模块，是为了直接复用 `MemberAuthLogic` 构造登录返回；
 * server 端推送通道不变，仍由 [PrivchatServiceClient.pushQrLoginAuthorized] 调过去。
 */
@Controller("/platform/qr-login")
class QrLoginController(
    private val privchatService: PrivchatServiceClient,
    private val memberAuthLogic: MemberAuthLogic,
    loggerFactory: LoggerFactory,
) {

    private val log: Logger = loggerFactory.get("controller.qr-login")

    // 与 server 推送 payload 一致的 Json 配置；不需要排版，只确保字段完整序列化。
    private val pushJson: Json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Post("/scenes")
    @AllowAnonymous
    suspend fun createScene(@Body request: PlatformCreateQrSceneRequest): QrSceneResponse {
        val deviceInfo = WebDeviceInfoInput(
            appId = request.deviceInfo?.appId ?: "web",
            deviceName = request.deviceInfo?.deviceName,
            userAgent = request.deviceInfo?.userAgent,
            ipAddress = request.deviceInfo?.ipAddress,
        )
        return privchatService.createQrScene(
            CreateQrSceneRequest(
                purpose = request.purpose,
                deviceId = request.deviceId,
                deviceInfo = deviceInfo,
                ttl = request.ttl,
            )
        )
    }

    @Get("/scenes")
    @AllowAnonymous
    suspend fun getScene(@Query sceneId: String): QrSceneStatus =
        privchatService.getQrScene(sceneId)

    @Post("/scan")
    suspend fun scan(
        identity: Identity,
        @Body request: PlatformScanQrSceneRequest,
    ): ScanQrSceneResponse =
        privchatService.scanQrScene(
            sceneId = request.sceneId,
            scannerUid = identity.id.toLong(),
            scannerDeviceId = request.scannerDeviceId,
            qrToken = request.qrToken,
            scannerAvatar = request.scannerAvatar,
            scannerDisplayName = request.scannerDisplayName,
        )

    @Post("/confirm")
    suspend fun confirm(
        identity: Identity,
        @Body request: PlatformConfirmQrSceneRequest,
    ): ConfirmQrSceneResponse {
        val scannerUid = identity.id.toLong()
        // 1) state=scanned → authorized；server 内存状态机推进
        val confirmed = privchatService.confirmQrScene(
            sceneId = request.sceneId,
            scannerUid = scannerUid,
            scannerDeviceId = request.scannerDeviceId,
            confirmToken = request.confirmToken,
        )

        // 2 + 3) 给 Web 设备签发完整登录返回 + 推到 Web unauth 连接（spec QR_API §5）。
        //
        // 整段保护性 try/catch：scene 已经 authorized，如果 issueImToken 或 push 失败，
        // 不应该让 mobile 端看到 5xx（mobile 已经"按下确认"是不可撤销的）；用户感知是
        // Web 端没收到 token → 重新刷二维码。spec §5：NoSubscriber / 推送失败都不回滚。
        try {
            val webDevice = LoginDeviceInfo(
                deviceId = confirmed.webDeviceId,
                platform = "web",
                deviceName = confirmed.webDeviceInfo.deviceName,
                // WebDeviceSnapshot 没有 model；server 的 NOT NULL 字段需占位（spec TOKEN_API §3.2）
                deviceModel = "unknown",
                osVersion = confirmed.webDeviceInfo.userAgent?.takeIf { it.isNotBlank() },
                appVersion = "1.0.0",
                ipAddress = confirmed.webDeviceInfo.ipAddress,
            )
            val loginResponse = memberAuthLogic.issueLoginResponseForUid(scannerUid, webDevice)
            val payload = pushJson.encodeToJsonElement(MemberLoginResponse.serializer(), loginResponse)
            val push = privchatService.pushQrLoginAuthorized(request.sceneId, payload)
            log.info(
                "qr_login.authorized.pushed",
                mapOf(
                    "sceneId" to request.sceneId,
                    "scannerUid" to scannerUid,
                    "delivered" to push.delivered,
                ),
            )
        } catch (t: Throwable) {
            log.warn(
                "qr_login.authorized.push_failed",
                mapOf(
                    "sceneId" to request.sceneId,
                    "scannerUid" to scannerUid,
                    "error" to (t.message ?: t::class.simpleName.orEmpty()),
                ),
                cause = t,
            )
        }

        return confirmed
    }

    @Post("/reject")
    suspend fun reject(
        identity: Identity,
        @Body request: PlatformRejectQrSceneRequest,
    ): RejectQrSceneResponse =
        privchatService.rejectQrScene(
            sceneId = request.sceneId,
            scannerUid = identity.id.toLong(),
            confirmToken = request.confirmToken,
        )
}

@Serializable
data class PlatformCreateQrSceneRequest(
    val purpose: String = "login",
    val deviceId: String,
    val deviceInfo: PlatformWebDeviceInfo? = null,
    val ttl: Long? = null,
)

@Serializable
data class PlatformWebDeviceInfo(
    val appId: String? = null,
    val deviceName: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
)

@Serializable
data class PlatformScanQrSceneRequest(
    val sceneId: String,
    val scannerDeviceId: String,
    val qrToken: String,
    val scannerAvatar: String? = null,
    val scannerDisplayName: String? = null,
)

@Serializable
data class PlatformConfirmQrSceneRequest(
    val sceneId: String,
    val scannerDeviceId: String,
    val confirmToken: String,
)

@Serializable
data class PlatformRejectQrSceneRequest(
    val sceneId: String,
    val confirmToken: String,
)
