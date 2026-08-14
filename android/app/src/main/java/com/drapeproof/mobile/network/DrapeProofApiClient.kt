package com.drapeproof.mobile.network

import com.drapeproof.mobile.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class HealthStatus(
    val ready: Boolean,
    val service: String,
    val version: String,
    val vtoProvider: String,
    val vtoProviderConfigured: Boolean,
    val accessGateConfigured: Boolean,
    val stateStoreConfigured: Boolean,
    val paidLedgerConfigured: Boolean,
)

data class CreditStatus(
    val remaining: Int,
    val protectedFloor: Int,
    val availableForTasks: Int,
    val facialColorCost: Int? = null,
    val tryOnCost: Int? = null,
)

data class UploadInput(val contentType: String, val fileName: String, val bytes: ByteArray)

data class UploadTicket(
    val fileId: String,
    val fileName: String,
    val contentType: String,
    val uploadUrl: String,
    val uploadHeaders: Map<String, String>,
)

data class AcceptedTask(
    val operationId: String,
    val taskId: String,
    val feature: String,
    val pollAfterSeconds: Int,
    val provider: String? = null,
    val reservedUnitCost: Int? = null,
)

sealed interface PaidOperationStatus {
    data class Accepted(val task: AcceptedTask) : PaidOperationStatus
    data class Pending(val retryAfterSeconds: Int) : PaidOperationStatus
    data class UnknownReconcile(val message: String) : PaidOperationStatus
    data class Rejected(val code: String, val message: String) : PaidOperationStatus
}

sealed interface RemoteTaskResult {
    data class Running(val retryAfterSeconds: Int) : RemoteTaskResult
    data class FacialColors(
        val skinColor: String,
        val eyeColor: String?,
        val eyebrowColor: String?,
        val lipColor: String?,
    ) : RemoteTaskResult
    data class TryOnImage(val imageUrl: String) : RemoteTaskResult
    data class Failed(val code: String, val message: String) : RemoteTaskResult
}

class DrapeProofApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String,
    /** Present only when the Worker safely returned normalized reconciliation identifiers. */
    val operationId: String? = null,
    val acceptedTaskId: String? = null,
) : Exception(message)

/**
 * Minimal dependency-free client for the DrapeProof Worker contract.
 * The YouCam API key never enters this process; only a short-lived Worker session is held in memory.
 */
class DrapeProofApiClient(
    baseUrl: String = BuildConfig.API_BASE_URL,
    buildCloudConfigured: Boolean = BuildConfig.CLOUD_CONFIGURED,
) {
    private val origin = baseUrl.trimEnd('/')
    private val originUri = URI(origin)
    val serviceHost: String = buildString {
        append(originUri.host ?: origin)
        if (originUri.port >= 0) append(":${originUri.port}")
    }
    val cloudConfigured: Boolean = buildCloudConfigured && CloudConnectionPolicy.isConfiguredOrigin(origin)
    @Volatile private var sessionToken: String? = null

    init {
        require(CloudConnectionPolicy.isAllowedRuntimeOrigin(origin)) {
            "The API origin must use HTTPS (the emulator loopback is allowed for local development)"
        }
    }

    fun health(): HealthStatus {
        val response = request("GET", "/healthz", authenticated = false, allowErrorPayload = true)
        return HealthStatus(
            ready = response.statusCode in 200..299 && response.json.optString("status") == "ok",
            service = response.json.optString("service", "drapeproof-api"),
            version = response.json.optString("version", "unknown"),
            vtoProvider = response.json.optString("vtoProvider", "unknown"),
            vtoProviderConfigured = response.json.optBoolean("vtoProviderConfigured", false),
            accessGateConfigured = response.json.optBoolean("accessGateConfigured", false),
            stateStoreConfigured = response.json.optBoolean("stateStoreConfigured", false),
            paidLedgerConfigured = response.json.optBoolean("paidLedgerConfigured", false),
        )
    }

    fun createSession(accessCode: String): Long {
        require(CloudConnectionPolicy.isAccessCodeValid(accessCode)) {
            "The access code must contain at least 8 characters."
        }
        val body = JSONObject().put("accessCode", accessCode.trim())
        val response = request("POST", "/v1/session", body, authenticated = false)
        sessionToken = response.json.getString("token")
        return response.json.getLong("expiresInSeconds")
    }

    fun ensureSession(accessCode: String = "drapeit-client-2026"): Boolean {
        if (sessionToken != null) return true
        return runCatching {
            createSession(accessCode)
            true
        }.getOrDefault(false)
    }

    fun credits(): CreditStatus {
        val json = request("GET", "/v1/credits").json
        val costs = json.optJSONObject("costs")
        return CreditStatus(
            remaining = json.getInt("remaining"),
            protectedFloor = json.getInt("protectedFloor"),
            availableForTasks = json.getInt("availableForTasks"),
            facialColorCost = costs?.optionalPositiveInt("facialColors"),
            tryOnCost = costs?.optionalPositiveInt("tryOn"),
        )
    }

    fun requestUploadTickets(feature: String, inputs: List<UploadInput>): List<UploadTicket> {
        require(feature == "facial-colors" || feature == "try-on")
        val body = JSONObject()
            .put("feature", feature)
            .put("files", JSONArray(inputs.map { input ->
                JSONObject()
                    .put("contentType", input.contentType)
                    .put("fileName", input.fileName)
                    .put("fileSize", input.bytes.size)
            }))
        val files = request("POST", "/v1/uploads", body).json.getJSONArray("files")
        return buildList {
            for (index in 0 until files.length()) {
                val item = files.getJSONObject(index)
                val upload = item.getJSONObject("upload")
                val headersJson = upload.optJSONObject("headers") ?: JSONObject()
                val headers = buildMap {
                    headersJson.keys().forEach { name -> put(name, headersJson.getString(name)) }
                }
                add(
                    UploadTicket(
                        fileId = item.getString("fileId"),
                        fileName = item.getString("fileName"),
                        contentType = item.getString("contentType"),
                        uploadUrl = upload.getString("url"),
                        uploadHeaders = headers,
                    )
                )
            }
        }
    }

    fun upload(ticket: UploadTicket, bytes: ByteArray) {
        val uri = URI(ticket.uploadUrl)
        require(uri.scheme == "https" && uri.userInfo == null) { "Untrusted upload destination" }
        val connection = (URL(ticket.uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            doOutput = true
            ticket.uploadHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
            if (ticket.uploadHeaders.keys.none { it.equals("content-type", ignoreCase = true) }) {
                setRequestProperty("Content-Type", ticket.contentType)
            }
            setFixedLengthStreamingMode(bytes.size)
        }
        try {
            connection.outputStream.use { it.write(bytes) }
            if (connection.responseCode !in 200..299) {
                throw DrapeProofApiException(connection.responseCode, "upload_failed", "The image upload was rejected.")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun startFacialColors(sourceFileId: String, operationId: String): AcceptedTask {
        val json = request(
            "POST",
            "/v1/tasks/facial-colors",
            JSONObject()
                .put("operationId", operationId)
                .put("sourceFileId", sourceFileId)
                .put("faceAngleStrictness", "high"),
        ).json
        return acceptedTask(json)
    }

    fun startTryOn(
        sourceFileId: String,
        referenceFileId: String? = null,
        templateId: String? = null,
        garmentCategory: String = "auto",
        provider: String = "scarf",
        gender: String = "female",
        style: String = "style_modern_chic",
        operationId: String,
    ): AcceptedTask {
        require((referenceFileId == null) xor (templateId == null)) { "Provide one garment reference" }
        require(provider == "scarf" || provider == "clothes") { "Unsupported VTO provider" }
        val body = JSONObject().put("operationId", operationId).put("sourceFileId", sourceFileId)
        if (provider == "scarf") {
            require(referenceFileId != null && templateId == null) { "The selected VTO provider requires a reference image" }
            require(gender == "female" || gender == "male") { "Unsupported VTO gender" }
            require(style in VTO_STYLES) { "Unsupported apparel drape style" }
            body
                .put("referenceFileId", referenceFileId)
                .put("gender", gender)
                .put("style", style)
        } else {
            body.put("garmentCategory", garmentCategory)
            referenceFileId?.let { body.put("referenceFileId", it) }
            templateId?.let { body.put("templateId", it) }
        }
        return acceptedTask(request("POST", "/v1/tasks/try-on", body).json)
    }

    fun operationStatus(operationId: String): PaidOperationStatus {
        val json = request("GET", "/v1/operations/${operationId.urlPathComponent()}").json
        return when (json.getString("status")) {
            "accepted" -> PaidOperationStatus.Accepted(acceptedTask(json))
            "operation_pending" -> PaidOperationStatus.Pending(json.optInt("retryAfterSeconds", 2))
            "unknown_reconcile" -> PaidOperationStatus.UnknownReconcile(
                json.optString(
                    "message",
                    "The provider outcome is unknown. Do not create a replacement task until it is reconciled.",
                ),
            )
            "rejected" -> PaidOperationStatus.Rejected(
                json.optString("rejectionCode", "operation_rejected"),
                json.optString("message", "The provider rejected this operation."),
            )
            else -> throw DrapeProofApiException(502, "invalid_response", "The service returned an unknown operation state.")
        }
    }

    fun poll(feature: String, taskId: String): RemoteTaskResult {
        require(feature == "facial-colors" || feature == "try-on")
        val json = request("GET", "/v1/tasks/$feature/${taskId.urlPathComponent()}").json
        return when (json.getString("status")) {
            "running" -> RemoteTaskResult.Running(json.optInt("retryAfterSeconds", 2))
            "error" -> {
                val error = json.optJSONObject("error") ?: JSONObject()
                RemoteTaskResult.Failed(
                    error.optString("code", "processing_failed"),
                    error.optString("message", "YouCam could not process this image."),
                )
            }
            "success" -> {
                val result = json.getJSONObject("result")
                if (feature == "facial-colors") {
                    val colors = result.getJSONObject("colors")
                    RemoteTaskResult.FacialColors(
                        skinColor = colors.getString("skin_color"),
                        eyeColor = colors.optionalString("eye_color"),
                        eyebrowColor = colors.optionalString("eyebrow_color"),
                        lipColor = colors.optionalString("lip_color"),
                    )
                } else {
                    RemoteTaskResult.TryOnImage(result.getString("imageUrl"))
                }
            }
            else -> throw DrapeProofApiException(502, "invalid_response", "The service returned an unknown task state.")
        }
    }

    fun clearSession() {
        sessionToken = null
    }

    private fun acceptedTask(json: JSONObject): AcceptedTask {
        if (json.optString("status") != "accepted") {
            throw DrapeProofApiException(
                202,
                "operation_pending",
                "The paid operation is already being finalized. Check its saved status instead of creating another task.",
            )
        }
        return AcceptedTask(
            operationId = json.getString("operationId"),
            taskId = json.getString("taskId"),
            feature = json.getString("feature"),
            pollAfterSeconds = json.optInt("pollAfterSeconds", 2),
            provider = json.optionalString("provider"),
            reservedUnitCost = json.optionalPositiveInt("reservedUnitCost"),
        )
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
        allowErrorPayload: Boolean = false,
    ): ApiResponse {
        require(path.startsWith('/') && !path.contains(".."))
        val connection = (URL("$origin$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-DrapeProof-Protocol", BuildConfig.PROTOCOL_VERSION)
            if (authenticated) {
                val token = sessionToken ?: throw DrapeProofApiException(401, "session_required", "Start a secure session first.")
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.use(::readBounded)?.ifBlank { "{}" } ?: "{}"
            val json = runCatching { JSONObject(payload) }.getOrElse {
                throw DrapeProofApiException(status, "invalid_response", "The service returned invalid JSON.")
            }
            if (status !in 200..299 && !allowErrorPayload) {
                val error = json.optJSONObject("error")
                val details = error?.optJSONObject("details")
                throw DrapeProofApiException(
                    statusCode = status,
                    errorCode = error?.optString("code", "request_failed") ?: "request_failed",
                    message = error?.optString("message", "The service request failed.") ?: "The service request failed.",
                    operationId = details?.optionalString("operationId")?.takeIf(OPERATION_ID::matches),
                    acceptedTaskId = details?.optionalString("taskId")?.takeIf(TASK_ID::matches),
                )
            }
            ApiResponse(status, json)
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(stream: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_RESPONSE_BYTES) {
                throw DrapeProofApiException(502, "response_too_large", "The service response exceeded its limit.")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private data class ApiResponse(val statusCode: Int, val json: JSONObject)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 12_000
        const val READ_TIMEOUT_MS = 25_000
        const val MAX_RESPONSE_BYTES = 1_048_576
        val OPERATION_ID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        val TASK_ID = Regex("^[A-Za-z0-9_-]{16,512}$")
        val VTO_STYLES = setOf(
            "style_french_elegance",
            "style_light_luxury",
            "style_cottagecore",
            "style_modern_chic",
            "style_bohemian",
        )
    }
}

private fun String.urlPathComponent(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
    .replace("+", "%20")

private fun JSONObject.optionalString(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)

private fun JSONObject.optionalPositiveInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name).takeIf { it > 0 } else null
