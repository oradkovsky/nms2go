package com.ror.nms2go.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.ExcelRow
import com.ror.nms2go.ParsedExcel
import com.ror.nms2go.R
import com.ror.nms2go.data.GmailAuthManager
import com.ror.nms2go.data.GmailAuthResult
import com.ror.nms2go.data.GmailSender
import com.ror.nms2go.data.OrderHistoryItem
import com.ror.nms2go.data.OrderHistoryRecord
import com.ror.nms2go.data.OrderHistoryRepository
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SenderRepository
import com.ror.nms2go.di.IoDispatcher
import com.ror.nms2go.utils.AppLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Content(
        val chosen: List<IndexedValue<ExcelRow>>,
        val quantities: Map<Int, Int>,
        val totalUnits: Int,
        val isSending: Boolean,
        val showConfirm: Boolean
    ) : ReviewUiState
    data class Error(
        val message: String
    ) : ReviewUiState
}

@HiltViewModel(assistedFactory = ReviewViewModel.Factory::class)
class ReviewViewModel @AssistedInject constructor(
    @Assisted parsed: ParsedExcel?,
    @Assisted quantities: Map<Int, Int>,
    private val gmailSender: GmailSender,
    private val senderRepository: SenderRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val authManager: GmailAuthManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _parsed = MutableStateFlow(parsed)
    private val _quantities = MutableStateFlow(quantities)
    private val _sending = MutableStateFlow(false)
    private val _showConfirm = MutableStateFlow(false)

    private val _quantityChangeEvent = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1, replay = 0)
    val quantityChangeEvent: SharedFlow<Pair<Int, Int>> = _quantityChangeEvent.asSharedFlow()

    private val _orderSent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val orderSent: SharedFlow<Unit> = _orderSent.asSharedFlow()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        updateDerivedState()
    }

    private fun updateDerivedState() {
        // The error lives in uiState exclusively: a shown error persists across
        // data updates until replaced. Handlers that can't fire from Error
        // (quantity/order/confirm) therefore preserve it implicitly.
        val error = (_uiState.value as? ReviewUiState.Error)?.message
        if (!error.isNullOrBlank()) {
            _uiState.value = ReviewUiState.Error(message = error)
            return
        }
        val parsed = _parsed.value
        if (parsed == null) {
            // Parent data hasn't arrived yet – wait for it instead of failing.
            _uiState.value = ReviewUiState.Loading
            return
        }
        val quantities = _quantities.value
        // All touched rows stay under review, including ones set back to 0 – a zeroed row
        // must remain visible (with locked sending) instead of vanishing to a blank screen.
        val chosen = parsed.rows.withIndex()
            .filter { (index, _) -> quantities.containsKey(index) }
            .sortedWith(
                compareBy<IndexedValue<ExcelRow>> { it.value.price ?: Double.MAX_VALUE }
                    .thenBy { it.value.counteragent.lowercase() }
            )
        if (chosen.isEmpty()) {
            // No empty state: nothing to review is reported as an error.
            _uiState.value = ReviewUiState.Error(message = "")
            return
        }
        val totalUnits = chosen.sumOf { quantities[it.index] ?: 0 }
        _uiState.value = ReviewUiState.Content(
            chosen = chosen,
            quantities = quantities,
            totalUnits = totalUnits,
            isSending = _sending.value,
            showConfirm = _showConfirm.value
        )
    }

    fun onQuantityChange(index: Int, quantity: Int) {
        // Quantities are locked while sending – the order snapshot must not change mid-send.
        if (_sending.value) return
        val coerced = quantity.coerceIn(0, MAX_QUANTITY)
        _quantityChangeEvent.tryEmit(index to coerced)
        // Optimistically update local quantities for immediate UI resort.
        val newMap = _quantities.value.toMutableMap()
        newMap[index] = coerced
        _quantities.value = newMap
        updateDerivedState()
    }

    fun onOrderRequested() {
        // Dialog may only be invoked from the enabled order button: never while sending,
        // and never when there is nothing to send (all quantities zero).
        if (_sending.value) return
        if (_quantities.value.none { it.value > 0 }) return
        _showConfirm.value = true
        updateDerivedState()
    }

    fun onDismissConfirm() {
        _showConfirm.value = false
        updateDerivedState()
    }

    fun onConfirmOrder() {
        // Ignore a stale confirm while sending to avoid a duplicate send,
        // or when there is nothing to send (all quantities zero).
        if (_sending.value || _quantities.value.none { it.value > 0 }) {
            _showConfirm.value = false
            updateDerivedState()
            return
        }
        _showConfirm.value = false
        // The order button shows "Sending…" and the dialog must never stay
        // visible on top of it.
        _sending.value = true
        updateDerivedState()
        viewModelScope.launch(ioDispatcher) {
            val items = sendableItems()
            if (items.isEmpty()) {
                _sending.value = false
                updateDerivedState()
                return@launch
            }
            when (val auth = authManager.authorize()) {
                is GmailAuthResult.Failed -> showError(auth.message)
                is GmailAuthResult.Authorized ->
                    executeSend(items, _quantities.value, auth.accessToken)
            }
        }
    }

    private fun sendableItems(): List<IndexedValue<ExcelRow>> {
        val parsed = _parsed.value ?: return emptyList()
        val quantities = _quantities.value
        return parsed.rows.withIndex()
            .filter { (index, _) -> (quantities[index] ?: 0) > 0 }
    }

    private fun showError(message: String) {
        _sending.value = false
        _uiState.value = ReviewUiState.Error(message = message)
    }

    private suspend fun executeSend(
        chosen: List<IndexedValue<ExcelRow>>,
        quantities: Map<Int, Int>,
        accessToken: String
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val groups = chosen.groupBy { it.value.receiver to it.value.company }
        try {
            val configuredSenders = senderRepository.getAll()
            val sent = mutableListOf<String>()
            val errors = mutableListOf<String>()
            for ((key, items) in groups) {
                coroutineContext.ensureActive()
                val receiver = key.first
                val company = key.second
                val name = company.ifBlank { items.first().value.counteragent }
                val senderEmail = configuredSenders
                    .firstOrNull { it.outboundEmail == receiver }
                    ?.inboundEmail.orEmpty()
                val subject = context.getString(R.string.order_subject, name, timestamp)
                if (receiver.isBlank()) {
                    errors += "$name: ${context.getString(R.string.order_no_receiver)}"
                    recordOrder(
                        name,
                        senderEmail,
                        receiver,
                        subject,
                        OrderStatus.FAILED,
                        "no receiver configured",
                        items,
                        quantities
                    )
                    continue
                }
                try {
                    gmailSender.sendMessage(
                        receiver,
                        subject,
                        buildOrderTableHtml(items, quantities),
                        accessToken
                    )
                    sent += name
                    recordOrder(
                        name,
                        senderEmail,
                        receiver,
                        subject,
                        OrderStatus.SENT,
                        null,
                        items,
                        quantities
                    )
                } catch (error: Exception) {
                    error.rethrowIfCancellation()
                    AppLog.w(TAG, "Order email to $receiver failed", error)
                    errors += "$name: ${error.localizedMessage.orEmpty()}"
                    recordOrder(
                        name,
                        senderEmail,
                        receiver,
                        subject,
                        OrderStatus.FAILED,
                        error.localizedMessage.orEmpty(),
                        items,
                        quantities
                    )
                }
            }
            val errorNote = if (errors.isEmpty()) {
                ""
            } else {
                context.getString(R.string.order_errors_prefix, errors.joinToString("; "))
            }
            if (sent.isEmpty()) {
                showError(errorNote)
            } else {
                _sending.value = false
                updateDerivedState()
                _orderSent.tryEmit(Unit)
            }
        } catch (error: Exception) {
            error.rethrowIfCancellation()
            AppLog.w(TAG, "Order sending failed", error)
            showError(
                context.getString(
                    R.string.order_send_failed,
                    error.localizedMessage.orEmpty()
                )
            )
        }
    }

    private suspend fun recordOrder(
        company: String,
        senderEmail: String,
        receiver: String,
        subject: String,
        status: String,
        error: String?,
        items: List<IndexedValue<ExcelRow>>,
        quantities: Map<Int, Int>
    ) {
        try {
            orderHistoryRepository.record(
                OrderHistoryRecord(
                    company = company,
                    senderEmail = senderEmail,
                    receiverEmail = receiver,
                    subject = subject,
                    status = status,
                    error = error,
                    items = items.map { (index, row) ->
                        OrderHistoryItem(
                            code = row.code,
                            name = row.article,
                            price = row.price,
                            quantity = quantities[index] ?: 0
                        )
                    }
                )
            )
        } catch (failure: Exception) {
            failure.rethrowIfCancellation()
            AppLog.w(TAG, "Could not record order history", failure)
        }
    }

    private fun buildOrderTableHtml(
        items: List<IndexedValue<ExcelRow>>,
        quantities: Map<Int, Int>
    ): String {
        val rowsHtml = items.joinToString("\n") { (index, row) ->
            val code = htmlEscape(row.code)
            val name = htmlEscape(row.article)
            val price = row.price?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
            val quantity = (quantities[index] ?: 0).toString()
            "            <tr><td>$code</td><td>$name</td><td>$price</td><td>$quantity</td></tr>"
        }
        return """
            <html>
            <body>
                <table border="1" cellspacing="0" cellpadding="6">
                    <tr><th>${context.getString(R.string.order_email_col_code)}</th><th>${
            context.getString(
                R.string.order_email_col_name
            )
        }</th><th>${context.getString(R.string.order_email_col_price)}</th><th>${context.getString(R.string.order_email_col_qty)}</th></tr>
        $rowsHtml
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun htmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun Exception.rethrowIfCancellation() {
        if (this is CancellationException) throw this
    }

    @AssistedFactory
    interface Factory {
        fun create(
            parsed: ParsedExcel?,
            quantities: Map<Int, Int>
        ): ReviewViewModel
    }

    private companion object {
        const val TAG = "ReviewViewModel"
    }
}
