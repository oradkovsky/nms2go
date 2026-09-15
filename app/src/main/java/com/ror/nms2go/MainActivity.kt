package com.ror.nms2go

import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ror.nms2go.ui.OrderWorkflowViewModel
import com.ror.nms2go.ui.SenderViewModel
import com.ror.nms2go.ui.Nms2GoApp
import com.ror.nms2go.ui.theme.Nms2GoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authorizationClient by lazy { Identity.getAuthorizationClient(this) }
    private val senderViewModel: SenderViewModel by viewModels()
    private val workflowViewModel: OrderWorkflowViewModel by viewModels()

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val sendIntentException = result.data?.let { data ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(
                    ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION,
                    IntentSender.SendIntentException::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                data.getSerializableExtra(
                    ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION
                ) as? IntentSender.SendIntentException
            }
        }
        if (sendIntentException != null) {
            workflowViewModel.onAuthorizationFailure(
                getString(
                    R.string.auth_ui_launch_failed,
                    sendIntentException.localizedMessage.orEmpty()
                )
            )
            return@registerForActivityResult
        }

        if (result.data == null) {
            workflowViewModel.onAuthorizationFailure(getString(R.string.auth_no_data))
            return@registerForActivityResult
        }

        try {
            workflowViewModel.onAuthorizationResult(
                authorizationClient.getAuthorizationResultFromIntent(result.data).accessToken
            )
        } catch (error: ApiException) {
            Log.w(TAG, "Authorization result parsing failed", error)
            FirebaseCrashlytics.getInstance().recordException(error)
            val baseMessage = when (error.statusCode) {
                16 -> getString(R.string.auth_cancelled)
                10 -> getString(R.string.auth_oauth_invalid)
                7 -> getString(R.string.auth_network_error)
                else -> getString(R.string.auth_failed)
            }
            workflowViewModel.onAuthorizationFailure(
                getString(
                    R.string.auth_failed_status,
                    baseMessage,
                    error.statusCode,
                    error.localizedMessage.orEmpty()
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val senders by senderViewModel.senders.collectAsState()
            val orders by senderViewModel.orders.collectAsState()
            val workflow by workflowViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                workflowViewModel.authorizationRequests.collect { requestAuthorization() }
            }

            Nms2GoTheme {
                Nms2GoApp(
                    senders = senders,
                    loading = workflow.loading,
                    statusText = workflow.statusText,
                    overviewResults = workflow.overviewResults,
                    onLoad = workflowViewModel::loadOverview,
                    parsedExcel = workflow.parsedExcel,
                    onParseItem = workflowViewModel::startOrderForOverview,
                    onOrder = workflowViewModel::startOrderFromOverview,
                    onSendOrders = workflowViewModel::sendOrders,
                    onDismissParsed = workflowViewModel::dismissParsed,
                    orderQuantities = workflow.orderQuantities,
                    onQuantityChange = workflowViewModel::onQuantityChange,
                    orders = orders,
                    orderSentStamp = workflow.orderSentStamp,
                    sendingOrders = workflow.sendingOrders,
                    orderSendError = workflow.orderSendError,
                    orderLoadingProgress = workflow.orderLoadingProgress
                )
            }
        }
    }

    private fun requestAuthorization() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(GMAIL_READONLY_SCOPE),
                    Scope(GMAIL_SEND_SCOPE)
                )
            )
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    launchResolution(result)
                } else {
                    workflowViewModel.onAuthorizationResult(result.accessToken)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Authorization request failed", error)
                FirebaseCrashlytics.getInstance().recordException(error)
                workflowViewModel.onAuthorizationFailure(
                    getString(R.string.auth_request_failed, error.localizedMessage.orEmpty())
                )
            }
    }

    private fun launchResolution(result: com.google.android.gms.auth.api.identity.AuthorizationResult) {
        val pendingIntent = result.pendingIntent
        if (pendingIntent == null) {
            workflowViewModel.onAuthorizationFailure(getString(R.string.auth_no_resolution))
            return
        }
        try {
            authorizationLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        } catch (error: IntentSender.SendIntentException) {
            workflowViewModel.onAuthorizationFailure(
                getString(R.string.auth_ui_launch_failed, error.localizedMessage.orEmpty())
            )
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
        const val GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send"
    }
}
