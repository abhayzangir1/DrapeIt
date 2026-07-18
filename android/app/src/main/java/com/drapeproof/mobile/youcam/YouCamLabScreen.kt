package com.drapeproof.mobile.youcam

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drapeproof.mobile.network.AcceptedTask
import com.drapeproof.mobile.network.CloudConnectionPolicy
import com.drapeproof.mobile.network.CreditStatus
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.network.DrapeProofApiException
import com.drapeproof.mobile.network.HealthStatus
import com.drapeproof.mobile.network.PaidOperationStatus
import com.drapeproof.mobile.network.RemoteTaskResult
import com.drapeproof.mobile.ui.ScreenHeader
import com.drapeproof.mobile.ui.theme.Cobalt
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import com.drapeproof.mobile.ui.theme.Plum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

private enum class TaskPhase { IDLE, UPLOADING, CREATING, POLLING, SAVING, SUCCESS, PAUSED, FAILED }

private data class TaskUiState(
    val phase: TaskPhase = TaskPhase.IDLE,
    val message: String = "Not run in this session.",
) {
    val busy: Boolean
        get() = phase == TaskPhase.UPLOADING || phase == TaskPhase.CREATING ||
            phase == TaskPhase.POLLING || phase == TaskPhase.SAVING
}

private data class FacialPalette(
    val skin: String,
    val eye: String?,
    val eyebrow: String?,
    val lip: String?,
)

private data class StyleOption(val value: String, val label: String)

private val scarfStyles = listOf(
    StyleOption("style_french_elegance", "French elegance"),
    StyleOption("style_light_luxury", "Light luxury"),
    StyleOption("style_cottagecore", "Cottagecore"),
    StyleOption("style_modern_chic", "Modern chic"),
    StyleOption("style_bohemian", "Bohemian"),
)

/**
 * Explicit, user-triggered access to the two optional YouCam features. Neither picker uploads
 * anything: upload and paid task creation begin only after consent and a separate action tap.
 */
@Composable
fun YouCamLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val api = remember { DrapeProofApiClient() }

    var accessCode by remember { mutableStateOf("") }
    var sessionReady by remember { mutableStateOf(false) }
    var sessionMessage by remember {
        mutableStateOf(
            if (api.cloudConfigured) {
                "Enter the required demo access code when you are ready to use a cloud feature."
            } else {
                "Cloud features are offline in this build. Camera and photo analysis remain available."
            },
        )
    }
    var connecting by remember { mutableStateOf(false) }
    var health by remember { mutableStateOf<HealthStatus?>(null) }
    var credits by remember { mutableStateOf<CreditStatus?>(null) }
    var consent by remember { mutableStateOf(false) }

    var faceUri by remember { mutableStateOf<Uri?>(null) }
    var scarfUri by remember { mutableStateOf<Uri?>(null) }
    var demoScarfSelected by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("female") }
    var selectedStyle by remember { mutableStateOf("style_modern_chic") }

    var facialState by remember { mutableStateOf(TaskUiState()) }
    var tryOnState by remember { mutableStateOf(TaskUiState()) }
    var facialPalette by remember { mutableStateOf<FacialPalette?>(null) }
    var facialTaskId by remember { mutableStateOf(YouCamLabStore.facialTaskId(context)) }
    var tryOnTaskId by remember { mutableStateOf(YouCamLabStore.tryOnTaskId(context)) }
    var facialOperationId by remember { mutableStateOf(YouCamLabStore.facialOperationId(context)) }
    var tryOnOperationId by remember { mutableStateOf(YouCamLabStore.tryOnOperationId(context)) }
    var localResultPath by remember { mutableStateOf(YouCamLabStore.tryOnResultPath(context)) }

    fun invalidateFacialOutcome(message: String) {
        facialPalette = null
        facialTaskId = null
        YouCamLabStore.clearFacialOutcome(context)
        facialState = TaskUiState(message = message)
    }

    fun invalidateTryOnOutcome(message: String) {
        tryOnTaskId = null
        localResultPath = null
        YouCamLabStore.clearTryOnOutcome(context)
        tryOnState = TaskUiState(message = message)
    }

    fun invalidateFaceOutcomes() {
        invalidateFacialOutcome("Face photo selected locally. Prior cloud evidence was cleared; nothing has been uploaded.")
        invalidateTryOnOutcome("Face photo changed. Prior VTO evidence was cleared; nothing has been uploaded.")
    }

    val facePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            faceUri = it
            invalidateFaceOutcomes()
        }
    }
    val scarfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scarfUri = it
            demoScarfSelected = false
            invalidateTryOnOutcome("Apparel reference selected locally. Prior VTO evidence was cleared; nothing has been uploaded.")
        }
    }

    fun friendlyError(error: Throwable): String = when (error) {
        is DrapeProofApiException -> error.message
        is IllegalArgumentException -> error.message ?: "The selected input was not accepted."
        else -> "The service could not complete this step. No task was recreated automatically."
    }

    suspend fun refreshCredits() {
        credits = try {
            withContext(Dispatchers.IO) { api.credits() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun pollFacial(taskId: String) {
        facialState = TaskUiState(TaskPhase.POLLING, "Processing. Polling every 2 seconds while this screen is open…")
        try {
            var attempts = 0
            while (currentCoroutineContext().isActive && attempts < MAX_POLL_ATTEMPTS) {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    facialState = TaskUiState(TaskPhase.PAUSED, "Polling paused while the app is outside the foreground. Resume with the saved task ID.")
                    return
                }
                delay(POLL_INTERVAL_MS)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    facialState = TaskUiState(TaskPhase.PAUSED, "Polling paused while the app is outside the foreground. Resume with the saved task ID.")
                    return
                }
                attempts++
                when (val result = withContext(Dispatchers.IO) { api.poll("facial-colors", taskId) }) {
                    is RemoteTaskResult.Running -> Unit
                    is RemoteTaskResult.FacialColors -> {
                        facialPalette = FacialPalette(result.skinColor, result.eyeColor, result.eyebrowColor, result.lipColor)
                        facialState = TaskUiState(TaskPhase.SUCCESS, "Facial colors returned. These are descriptive captured colors, not a beauty score.")
                        refreshCredits()
                        return
                    }
                    is RemoteTaskResult.Failed -> {
                        facialState = TaskUiState(TaskPhase.FAILED, result.message)
                        refreshCredits()
                        return
                    }
                    is RemoteTaskResult.TryOnImage -> {
                        facialState = TaskUiState(TaskPhase.FAILED, "The service returned the wrong result type.")
                        return
                    }
                }
            }
            if (currentCoroutineContext().isActive) {
                facialState = TaskUiState(TaskPhase.PAUSED, "Polling reached the 10-minute safety limit. The accepted task ID is saved to resume.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            facialState = TaskUiState(TaskPhase.PAUSED, "Polling paused: ${friendlyError(error)}")
        }
    }

    suspend fun pollTryOn(taskId: String) {
        tryOnState = TaskUiState(TaskPhase.POLLING, "Generating. Polling every 2 seconds while this screen is open…")
        try {
            var attempts = 0
            while (currentCoroutineContext().isActive && attempts < MAX_POLL_ATTEMPTS) {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    tryOnState = TaskUiState(TaskPhase.PAUSED, "Polling paused while the app is outside the foreground. Resume with the saved task ID.")
                    return
                }
                delay(POLL_INTERVAL_MS)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    tryOnState = TaskUiState(TaskPhase.PAUSED, "Polling paused while the app is outside the foreground. Resume with the saved task ID.")
                    return
                }
                attempts++
                when (val result = withContext(Dispatchers.IO) { api.poll("try-on", taskId) }) {
                    is RemoteTaskResult.Running -> Unit
                    is RemoteTaskResult.TryOnImage -> {
                        tryOnState = TaskUiState(TaskPhase.SAVING, "Copying the temporary result into private app storage…")
                        localResultPath = saveTryOnResult(context, result.imageUrl, taskId)
                        tryOnState = TaskUiState(TaskPhase.SUCCESS, "Saved privately on this device. The temporary result URL was not retained.")
                        refreshCredits()
                        return
                    }
                    is RemoteTaskResult.Failed -> {
                        tryOnState = TaskUiState(TaskPhase.FAILED, result.message)
                        refreshCredits()
                        return
                    }
                    is RemoteTaskResult.FacialColors -> {
                        tryOnState = TaskUiState(TaskPhase.FAILED, "The service returned the wrong result type.")
                        return
                    }
                }
            }
            if (currentCoroutineContext().isActive) {
                tryOnState = TaskUiState(TaskPhase.PAUSED, "Polling reached the 10-minute safety limit. The accepted task ID is saved to resume.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            tryOnState = TaskUiState(TaskPhase.PAUSED, "Polling paused: ${friendlyError(error)}")
        }
    }

    suspend fun acceptFacialTask(accepted: AcceptedTask) {
        check(accepted.feature == "facial-colors") { "The service returned the wrong operation feature." }
        facialTaskId = accepted.taskId
        facialOperationId = null
        withContext(Dispatchers.IO) {
            YouCamLabStore.saveFacialTaskId(context, accepted.taskId)
            YouCamLabStore.clearFacialOperationId(context)
        }
        refreshCredits()
        pollFacial(accepted.taskId)
    }

    suspend fun acceptTryOnTask(accepted: AcceptedTask) {
        check(accepted.feature == "try-on") { "The service returned the wrong operation feature." }
        tryOnTaskId = accepted.taskId
        tryOnOperationId = null
        withContext(Dispatchers.IO) {
            YouCamLabStore.saveTryOnTaskId(context, accepted.taskId)
            YouCamLabStore.clearTryOnOperationId(context)
        }
        refreshCredits()
        pollTryOn(accepted.taskId)
    }

    suspend fun recoverFacialTaskIdFromCommitError(error: Throwable): Boolean {
        val apiError = error as? DrapeProofApiException ?: return false
        val operationId = facialOperationId ?: return false
        val taskId = apiError.acceptedTaskId ?: return false
        if (apiError.errorCode != "operation_commit_unavailable" || apiError.operationId != operationId) return false
        facialTaskId = taskId
        withContext(Dispatchers.IO) { YouCamLabStore.saveFacialTaskId(context, taskId) }
        facialState = TaskUiState(
            TaskPhase.PAUSED,
            "YouCam accepted this task, but ledger confirmation needs reconciliation. The delivered task ID was saved; no replacement will be created.",
        )
        refreshCredits()
        pollFacial(taskId)
        return true
    }

    suspend fun recoverTryOnTaskIdFromCommitError(error: Throwable): Boolean {
        val apiError = error as? DrapeProofApiException ?: return false
        val operationId = tryOnOperationId ?: return false
        val taskId = apiError.acceptedTaskId ?: return false
        if (apiError.errorCode != "operation_commit_unavailable" || apiError.operationId != operationId) return false
        tryOnTaskId = taskId
        withContext(Dispatchers.IO) { YouCamLabStore.saveTryOnTaskId(context, taskId) }
        tryOnState = TaskUiState(
            TaskPhase.PAUSED,
            "YouCam accepted this task, but ledger confirmation needs reconciliation. The delivered task ID was saved; no replacement will be created.",
        )
        refreshCredits()
        pollTryOn(taskId)
        return true
    }

    suspend fun resolveFacialOperation(operationId: String) {
        facialState = TaskUiState(TaskPhase.CREATING, "Checking the saved paid operation without creating another task…")
        try {
            when (val status = withContext(Dispatchers.IO) { api.operationStatus(operationId) }) {
                is PaidOperationStatus.Accepted -> acceptFacialTask(status.task)
                is PaidOperationStatus.Pending -> facialState = TaskUiState(
                    TaskPhase.PAUSED,
                    "The original operation is still being finalized. Check again in ${status.retryAfterSeconds} seconds; no second task was created.",
                )
                is PaidOperationStatus.UnknownReconcile -> facialState = TaskUiState(
                    TaskPhase.FAILED,
                    "UNKNOWN / RECONCILE: ${status.message}",
                )
                is PaidOperationStatus.Rejected -> {
                    facialOperationId = null
                    withContext(Dispatchers.IO) { YouCamLabStore.clearFacialOperationId(context) }
                    facialState = TaskUiState(TaskPhase.FAILED, status.message)
                    refreshCredits()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (error is DrapeProofApiException && error.errorCode == "operation_not_found") {
                facialOperationId = null
                withContext(Dispatchers.IO) { YouCamLabStore.clearFacialOperationId(context) }
                facialState = TaskUiState(TaskPhase.PAUSED, "No server admission exists for the saved operation. Tap again to start a fresh task.")
            } else {
                facialState = TaskUiState(TaskPhase.PAUSED, "Operation status unavailable: ${friendlyError(error)}")
            }
        }
    }

    suspend fun resolveTryOnOperation(operationId: String) {
        tryOnState = TaskUiState(TaskPhase.CREATING, "Checking the saved paid operation without creating another task…")
        try {
            when (val status = withContext(Dispatchers.IO) { api.operationStatus(operationId) }) {
                is PaidOperationStatus.Accepted -> acceptTryOnTask(status.task)
                is PaidOperationStatus.Pending -> tryOnState = TaskUiState(
                    TaskPhase.PAUSED,
                    "The original operation is still being finalized. Check again in ${status.retryAfterSeconds} seconds; no second task was created.",
                )
                is PaidOperationStatus.UnknownReconcile -> tryOnState = TaskUiState(
                    TaskPhase.FAILED,
                    "UNKNOWN / RECONCILE: ${status.message}",
                )
                is PaidOperationStatus.Rejected -> {
                    tryOnOperationId = null
                    withContext(Dispatchers.IO) { YouCamLabStore.clearTryOnOperationId(context) }
                    tryOnState = TaskUiState(TaskPhase.FAILED, status.message)
                    refreshCredits()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (error is DrapeProofApiException && error.errorCode == "operation_not_found") {
                tryOnOperationId = null
                withContext(Dispatchers.IO) { YouCamLabStore.clearTryOnOperationId(context) }
                tryOnState = TaskUiState(TaskPhase.PAUSED, "No server admission exists for the saved operation. Tap again to start a fresh task.")
            } else {
                tryOnState = TaskUiState(TaskPhase.PAUSED, "Operation status unavailable: ${friendlyError(error)}")
            }
        }
    }

    fun connect() {
        if (connecting || !api.cloudConfigured) return
        val requiredAccessCode = accessCode.trim()
        if (!CloudConnectionPolicy.isAccessCodeValid(requiredAccessCode)) {
            sessionMessage = "Enter the required access code (at least 8 characters)."
            return
        }
        scope.launch {
            connecting = true
            sessionMessage = "Opening a short-lived secure session…"
            try {
                val status = withContext(Dispatchers.IO) { api.health() }
                health = status
                if (!status.ready) {
                    sessionReady = false
                    credits = null
                    sessionMessage = "Server ${api.serviceHost} is not ready: ${status.configurationDiagnostic()}."
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    val expires = api.createSession(requiredAccessCode)
                    val balance = api.credits()
                    Pair(expires, balance)
                }
                credits = result.second
                sessionReady = true
                sessionMessage = "Secure session ready for ${result.first / 60} minutes. The API key stays on the server."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                sessionReady = false
                credits = null
                sessionMessage = friendlyError(error)
            } finally {
                connecting = false
            }
        }
    }

    fun createFacialTask() {
        facialOperationId?.let { operationId ->
            if (!facialState.busy) scope.launch { resolveFacialOperation(operationId) }
            return
        }
        val savedFacialTaskId = facialTaskId
        if (savedFacialTaskId != null && facialState.phase != TaskPhase.SUCCESS && facialState.phase != TaskPhase.FAILED) {
            if (!facialState.busy) scope.launch { pollFacial(savedFacialTaskId) }
            return
        }
        val source = faceUri ?: return
        if (facialState.busy) return
        scope.launch {
            facialPalette = null
            try {
                facialState = TaskUiState(TaskPhase.UPLOADING, "Preparing and uploading one JPEG…")
                val ticket = withContext(Dispatchers.IO) {
                    val input = prepareUpload(context, source, role = "face", jpegOnly = true)
                    api.requestUploadTickets("facial-colors", listOf(input)).single().also {
                        api.upload(it, input.bytes)
                    }
                }
                facialState = TaskUiState(TaskPhase.CREATING, "Creating one Facial Color Tones task…")
                val operationId = UUID.randomUUID().toString()
                withContext(Dispatchers.IO) { YouCamLabStore.saveFacialOperationId(context, operationId) }
                facialOperationId = operationId
                val accepted = withContext(Dispatchers.IO) { api.startFacialColors(ticket.fileId, operationId) }
                acceptFacialTask(accepted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!recoverFacialTaskIdFromCommitError(error)) {
                    facialOperationId?.let { resolveFacialOperation(it) }
                        ?: run { facialState = TaskUiState(TaskPhase.FAILED, friendlyError(error)) }
                }
            }
        }
    }

    fun createTryOnTask() {
        tryOnOperationId?.let { operationId ->
            if (!tryOnState.busy) scope.launch { resolveTryOnOperation(operationId) }
            return
        }
        val savedTryOnTaskId = tryOnTaskId
        if (savedTryOnTaskId != null && tryOnState.phase != TaskPhase.SUCCESS && tryOnState.phase != TaskPhase.FAILED) {
            if (!tryOnState.busy) scope.launch { pollTryOn(savedTryOnTaskId) }
            return
        }
        val source = faceUri ?: return
        val reference = scarfUri ?: return
        if (tryOnState.busy) return
        scope.launch {
            try {
                tryOnState = TaskUiState(TaskPhase.UPLOADING, "Preparing and uploading the face and apparel reference…")
                val tickets = withContext(Dispatchers.IO) {
                    val face = prepareUpload(context, source, role = "person", jpegOnly = false)
                    val scarf = prepareUpload(context, reference, role = "scarf", jpegOnly = false)
                    val inputs = listOf(face, scarf)
                    api.requestUploadTickets("try-on", inputs).also { issued ->
                        check(issued.size == 2) { "The service did not return both upload tickets." }
                        issued.zip(inputs).forEach { (ticket, input) -> api.upload(ticket, input.bytes) }
                    }
                }
                tryOnState = TaskUiState(TaskPhase.CREATING, "Creating one deterministic virtual try-on task…")
                val operationId = UUID.randomUUID().toString()
                withContext(Dispatchers.IO) { YouCamLabStore.saveTryOnOperationId(context, operationId) }
                tryOnOperationId = operationId
                val activeProvider = health?.vtoProvider?.takeIf { it == "scarf" || it == "clothes" }
                    ?: throw IllegalStateException("The Worker did not report a supported VTO provider.")
                val accepted = withContext(Dispatchers.IO) {
                    api.startTryOn(
                        sourceFileId = tickets[0].fileId,
                        referenceFileId = tickets[1].fileId,
                        garmentCategory = "auto",
                        provider = activeProvider,
                        gender = selectedGender,
                        style = selectedStyle,
                        operationId = operationId,
                    )
                }
                acceptTryOnTask(accepted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!recoverTryOnTaskIdFromCommitError(error)) {
                    tryOnOperationId?.let { resolveTryOnOperation(it) }
                        ?: run { tryOnState = TaskUiState(TaskPhase.FAILED, friendlyError(error)) }
                }
            }
        }
    }

    val activeProvider = health?.vtoProvider?.takeIf { it == "scarf" || it == "clothes" } ?: "scarf"
    val providerReady = health?.let {
        (it.vtoProvider == "scarf" || it.vtoProvider == "clothes") && it.vtoProviderConfigured
    } == true
    val paidActionsReady = sessionReady && credits != null
    val facialTaskNeedsResolution = facialTaskId != null &&
        facialState.phase != TaskPhase.SUCCESS && facialState.phase != TaskPhase.FAILED
    val tryOnTaskNeedsResolution = tryOnTaskId != null && localResultPath == null &&
        tryOnState.phase != TaskPhase.SUCCESS && tryOnState.phase != TaskPhase.FAILED
    val faceInputEnabled = !facialState.busy && !tryOnState.busy &&
        facialOperationId == null && tryOnOperationId == null &&
        !facialTaskNeedsResolution && !tryOnTaskNeedsResolution
    val tryOnInputEnabled = !tryOnState.busy && tryOnOperationId == null && !tryOnTaskNeedsResolution

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "YouCam Lab", evidence = "OPT-IN CLOUD", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text("Two cloud proofs, under your control.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Use Facial Color Tones as a secondary measurement, then preview the selected apparel reference. DrapeProof's local contrast result remains the primary evidence.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))

            SessionCard(
                accessCode = accessCode,
                onAccessCodeChange = { accessCode = it.take(512) },
                connecting = connecting,
                connected = sessionReady,
                message = sessionMessage,
                health = health,
                credits = credits,
                serviceHost = api.serviceHost,
                cloudConfigured = api.cloudConfigured,
                onConnect = ::connect,
                onRefreshCredits = { scope.launch { refreshCredits() } },
            )
            Spacer(Modifier.height(12.dp))

            ConsentCard(checked = consent, credits = credits, onCheckedChange = { consent = it })
            Spacer(Modifier.height(18.dp))

            FacePhotoCard(
                faceUri = faceUri,
                enabled = faceInputEnabled,
                onPick = { facePicker.launch(arrayOf("image/jpeg")) },
            )
            Spacer(Modifier.height(18.dp))

            FacialColorsCard(
                state = facialState,
                palette = facialPalette,
                taskId = facialTaskId,
                canCreate = if (facialOperationId != null) sessionReady else paidActionsReady && consent &&
                    !facialTaskNeedsResolution && faceUri != null &&
                    credits?.facialColorCost?.let { it <= credits!!.availableForTasks } == true,
                canResume = sessionReady && facialTaskId != null && !facialState.busy,
                hasPendingOperation = facialOperationId != null,
                unitCost = credits?.facialColorCost,
                onCreate = ::createFacialTask,
                onResume = { facialTaskId?.let { id -> scope.launch { pollFacial(id) } } },
            )
            Spacer(Modifier.height(18.dp))

            ScarfTryOnCard(
                scarfUri = scarfUri,
                state = tryOnState,
                taskId = tryOnTaskId,
                resultPath = localResultPath,
                demoScarfSelected = demoScarfSelected,
                selectedGender = selectedGender,
                onGenderChange = {
                    if (selectedGender != it) {
                        selectedGender = it
                        invalidateTryOnOutcome("VTO fit profile changed. Prior VTO evidence was cleared.")
                    }
                },
                selectedStyle = selectedStyle,
                onStyleChange = {
                    if (selectedStyle != it) {
                        selectedStyle = it
                        invalidateTryOnOutcome("VTO drape style changed. Prior VTO evidence was cleared.")
                    }
                },
                provider = activeProvider,
                providerReady = providerReady,
                inputsEnabled = tryOnInputEnabled,
                canCreate = if (tryOnOperationId != null) sessionReady else paidActionsReady && providerReady &&
                    consent && !tryOnTaskNeedsResolution && faceUri != null && scarfUri != null &&
                    credits?.tryOnCost?.let { it <= credits!!.availableForTasks } == true,
                canResume = sessionReady && tryOnTaskId != null && !tryOnState.busy,
                hasPendingOperation = tryOnOperationId != null,
                unitCost = credits?.tryOnCost,
                onPickScarf = { scarfPicker.launch(arrayOf("image/jpeg", "image/png")) },
                onUseDemoScarf = {
                    scope.launch {
                        scarfUri = demoScarfUri(context)
                        demoScarfSelected = true
                        invalidateTryOnOutcome("Visualization-only demo apparel drape selected locally. Prior VTO evidence was cleared; nothing has been uploaded.")
                    }
                },
                onCreate = ::createTryOnTask,
                onResume = { tryOnTaskId?.let { id -> scope.launch { pollTryOn(id) } } },
            )

            Spacer(Modifier.height(22.dp))
            Text(
                "Task creation is single-shot: DrapeProof never automatically creates a second paid task after a network error. Polling stops when you leave this screen; the accepted task ID remains available to resume.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SessionCard(
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    connecting: Boolean,
    connected: Boolean,
    message: String,
    health: HealthStatus?,
    credits: CreditStatus?,
    serviceHost: String,
    cloudConfigured: Boolean,
    onConnect: () -> Unit,
    onRefreshCredits: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (connected) Moss else MaterialTheme.colorScheme.outline)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(if (connected) "Secure session connected" else "Secure server session", style = MaterialTheme.typography.titleMedium)
                    Text("Server · $serviceHost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
            health?.let { status ->
                Spacer(Modifier.height(8.dp))
                Text(
                    if (status.ready) "Health ready · ${status.service} ${status.version}" else "Health degraded · ${status.configurationDiagnostic()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.ready) Moss else DrapeCoral,
                )
            }
            if (!cloudConfigured) {
                Spacer(Modifier.height(10.dp))
                Notice("Cloud backend is not configured in this build. Use the on-device camera and photo analysis flows.")
            }
            if (!connected) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = onAccessCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Demo access code (required)") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text("At least 8 characters; held only for this session.", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onConnect,
                    enabled = cloudConfigured && !connecting && CloudConnectionPolicy.isAccessCodeValid(accessCode),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (connecting) "Connecting…" else "Connect securely")
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("App budget", credits?.remaining?.toString() ?: "—", Modifier.weight(1f))
                    MetricPill("Reserve", credits?.protectedFloor?.toString() ?: "—", Modifier.weight(1f))
                    MetricPill("Usable", credits?.availableForTasks?.toString() ?: "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val provider = health?.vtoProvider?.uppercase(Locale.US) ?: "UNKNOWN"
                    Text("VTO provider: $provider", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onRefreshCredits) { Text("Refresh units") }
                }
            }
        }
    }
}

private fun HealthStatus.configurationDiagnostic(): String {
    val missing = buildList {
        if (!accessGateConfigured) add("access gate")
        if (!stateStoreConfigured) add("state store")
        if (!paidLedgerConfigured) add("paid-task ledger")
        if (!vtoProviderConfigured) add("VTO provider")
    }
    return if (missing.isEmpty()) "service reported a degraded status" else "missing ${missing.joinToString()}"
}

@Composable
private fun ConsentCard(checked: Boolean, credits: CreditStatus?, onCheckedChange: (Boolean) -> Unit) {
    val facialCost = credits?.facialColorCost
    val tryOnCost = credits?.tryOnCost
    val costNotice = if (facialCost != null && tryOnCost != null) {
        "LIVE COST · Running both optional proofs currently reserves ${facialCost + tryOnCost} API units total ($facialCost Facial Color + $tryOnCost apparel VTO)."
    } else {
        "LIVE COST UNAVAILABLE · Paid task creation stays disabled until the secure server returns a verified per-feature quote."
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DrapeCoral.copy(alpha = 0.10f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(Modifier.padding(start = 8.dp)) {
                Text("I choose to send these selected photos", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Only after I tap a run button, DrapeProof may send the selected face and/or apparel-reference image through its secure server to Perfect Corp for this result.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$costNotice The local demo budget and protected reserve are checked server-side before creation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DrapeCoral,
                )
            }
        }
    }
}

@Composable
private fun FacePhotoCard(faceUri: Uri?, enabled: Boolean, onPick: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Text("Shared face source", style = MaterialTheme.typography.titleLarge)
            Text("Front-facing JPEG, even natural light, no filters. The same source keeps both cloud proofs comparable.", style = MaterialTheme.typography.bodySmall)
            Text("If needed, only EXIF orientation and upload dimensions are normalized—never skin, colour, contrast, or sharpness.", style = MaterialTheme.typography.labelSmall, color = Moss)
            Spacer(Modifier.height(12.dp))
            if (faceUri != null) UriImage(faceUri, "Selected face photo") else EmptyImage("FACE JPEG")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (faceUri == null) "Choose face JPEG" else "Replace face JPEG")
            }
            Text("Picking a file stays local. It is not an upload action.", style = MaterialTheme.typography.labelSmall, color = Moss)
        }
    }
}

@Composable
private fun FacialColorsCard(
    state: TaskUiState,
    palette: FacialPalette?,
    taskId: String?,
    canCreate: Boolean,
    canResume: Boolean,
    hasPendingOperation: Boolean,
    unitCost: Int?,
    onCreate: () -> Unit,
    onResume: () -> Unit,
) {
    FeatureCard(kicker = "OPTION 01", title = "Facial Color Tones", accent = Cobalt, state = state) {
        Text("A secondary API reading of recorded skin, eye, brow and lip colours. It does not decide which colour ‘looks best.’", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        palette?.let { FacialPaletteView(it) }
        taskId?.let { TaskIdRow(it) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCreate, enabled = canCreate && !state.busy, modifier = Modifier.fillMaxWidth()) {
            val label = unitCost?.let { "quoted $it unit${if (it == 1) "" else "s"}" } ?: "live quote unavailable"
            Text(
                when {
                    state.busy -> "Task in progress…"
                    hasPendingOperation -> "Check saved operation · no new charge"
                    else -> "Send face + create task · $label"
                },
            )
        }
        if (canResume && state.phase != TaskPhase.SUCCESS) {
            OutlinedButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Resume saved task polling") }
        }
    }
}

@Composable
private fun ScarfTryOnCard(
    scarfUri: Uri?,
    state: TaskUiState,
    taskId: String?,
    resultPath: String?,
    demoScarfSelected: Boolean,
    selectedGender: String,
    onGenderChange: (String) -> Unit,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    provider: String,
    providerReady: Boolean,
    inputsEnabled: Boolean,
    canCreate: Boolean,
    canResume: Boolean,
    hasPendingOperation: Boolean,
    unitCost: Int?,
    onPickScarf: () -> Unit,
    onUseDemoScarf: () -> Unit,
    onCreate: () -> Unit,
    onResume: () -> Unit,
) {
    FeatureCard(kicker = "OPTION 02", title = "Exact-reference apparel VTO", accent = Plum, state = state) {
        Text("Preview the selected apparel reference on the same face source. The result is illustrative VTO evidence, not a physical colour measurement.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        if (!providerReady) {
            Notice("VTO is gated until the secure Worker reports a configured apparel provider.")
            Spacer(Modifier.height(12.dp))
        }
        if (scarfUri != null) UriImage(scarfUri, "Selected apparel reference") else EmptyImage("APPAREL REFERENCE")
        if (demoScarfSelected) {
            Spacer(Modifier.height(8.dp))
            Notice("Demo cobalt apparel drape · visualization-only reference. Never use this generated asset as measurement or real-product colour evidence.")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onPickScarf, enabled = inputsEnabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (scarfUri == null) "Choose apparel JPEG or PNG" else "Replace apparel reference")
        }
        Text("Oversize references receive geometry-only normalization; no colour or contrast adjustment.", style = MaterialTheme.typography.labelSmall, color = Moss)
        OutlinedButton(onClick = onUseDemoScarf, enabled = inputsEnabled, modifier = Modifier.fillMaxWidth()) {
            Text("Use demo cobalt apparel drape · visualization only")
        }

        Spacer(Modifier.height(14.dp))
        if (provider == "scarf") {
            Text("VTO fit profile (selected, never inferred)", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("female" to "Female", "male" to "Male").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedGender == value,
                        onClick = { onGenderChange(value) },
                        enabled = inputsEnabled,
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Deterministic drape style", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scarfStyles.forEach { option ->
                    FilterChip(
                        selected = selectedStyle == option.value,
                        onClick = { onStyleChange(option.value) },
                        enabled = inputsEnabled,
                        label = { Text(option.label) },
                    )
                }
            }
        }
        taskId?.let { TaskIdRow(it) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCreate, enabled = canCreate && !state.busy, modifier = Modifier.fillMaxWidth()) {
            val label = unitCost?.let { "quoted $it unit${if (it == 1) "" else "s"}" } ?: "live quote unavailable"
            Text(
                when {
                    state.busy -> "Task in progress…"
                    hasPendingOperation -> "Check saved operation · no new charge"
                    else -> "Send 2 photos + create task · $label"
                },
            )
        }
        if (canResume && state.phase != TaskPhase.SUCCESS) {
            OutlinedButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Resume saved VTO polling") }
        }
        resultPath?.let { path ->
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))
            Text("Private on-device result", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FileImage(path, "Saved apparel virtual try-on result")
            Text("Saved in app-private storage; the temporary network URL is not retained.", style = MaterialTheme.typography.labelSmall, color = Moss)
        }
    }
}

@Composable
private fun FeatureCard(
    kicker: String,
    title: String,
    accent: Color,
    state: TaskUiState,
    content: @Composable () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                Text(kicker, style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(start = 8.dp).weight(1f))
                PhasePill(state.phase)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
            if (state.busy) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = phaseColor(state.phase))
        }
    }
}

@Composable
private fun FacialPaletteView(palette: FacialPalette) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorRow("Recorded skin", palette.skin)
        palette.eye?.let { ColorRow("Recorded eyes", it) }
        palette.eyebrow?.let { ColorRow("Recorded brows", it) }
        palette.lip?.let { ColorRow("Recorded lips", it) }
    }
}

@Composable
private fun ColorRow(label: String, value: String) {
    val color = parseColor(value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun UriImage(uri: Uri, description: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = decodePreview(context, uri)
    }
    PreviewFrame(description, bitmap)
}

@Composable
private fun FileImage(path: String, description: String) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path, File(path).lastModified()) {
        value = decodePreview(path)
    }
    PreviewFrame(description, bitmap)
}

@Composable
private fun PreviewFrame(description: String, bitmap: android.graphics.Bitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("Loading local preview…", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EmptyImage(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
}

@Composable
private fun TaskIdRow(taskId: String) {
    Spacer(Modifier.height(10.dp))
    Text("Saved task · ${taskId.take(8)}…${taskId.takeLast(4)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.background).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable
private fun PhasePill(phase: TaskPhase) {
    val text = when (phase) {
        TaskPhase.IDLE -> "LOCAL"
        TaskPhase.UPLOADING -> "UPLOAD"
        TaskPhase.CREATING -> "CREATE"
        TaskPhase.POLLING -> "POLLING"
        TaskPhase.SAVING -> "SAVING"
        TaskPhase.SUCCESS -> "SAVED"
        TaskPhase.PAUSED -> "PAUSED"
        TaskPhase.FAILED -> "STOPPED"
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = phaseColor(phase),
        modifier = Modifier.clip(CircleShape).background(phaseColor(phase).copy(alpha = 0.10f)).padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = DrapeCoral,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DrapeCoral.copy(alpha = 0.10f)).padding(12.dp),
    )
}

private fun phaseColor(phase: TaskPhase): Color = when (phase) {
    TaskPhase.SUCCESS -> Moss
    TaskPhase.FAILED, TaskPhase.PAUSED -> DrapeCoral
    TaskPhase.IDLE -> Color(0xFF77736B)
    else -> Cobalt
}

private fun parseColor(raw: String): Color? = runCatching {
    val cleaned = raw.trim()
    val android = when {
        cleaned.matches(Regex("#?[0-9A-Fa-f]{6}")) ->
            AndroidColor.parseColor(if (cleaned.startsWith('#')) cleaned else "#$cleaned")
        else -> {
            val values = Regex("\\d{1,3}").findAll(cleaned).map { it.value.toInt() }.take(3).toList()
            require(values.size == 3 && values.all { it in 0..255 })
            AndroidColor.rgb(values[0], values[1], values[2])
        }
    }
    Color(android)
}.getOrNull()

private const val POLL_INTERVAL_MS = 2_000L
private const val MAX_POLL_ATTEMPTS = 300
