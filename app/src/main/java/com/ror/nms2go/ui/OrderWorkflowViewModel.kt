package com.ror.nms2go.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.ExcelParser
import com.ror.nms2go.ExcelRow
import com.ror.nms2go.ParsedExcel
import com.ror.nms2go.R
import com.ror.nms2go.utils.AppLog
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.data.GmailRepository
import com.ror.nms2go.data.OrderHistoryItem
import com.ror.nms2go.data.OrderHistoryRecord
import com.ror.nms2go.data.OrderHistoryRepository
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import com.ror.nms2go.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Whether cached overview results must be dropped because the sender config changed
 * since the results were loaded. Pure in its inputs, so the contract is unit-testable
 * without Android dependencies.
 */
internal fun shouldInvalidateOverview(
    loadedForSenders: List<SenderEntity>?,
    latestSenders: List<SenderEntity>,
    hasResults: Boolean
): Boolean = loadedForSenders != null && hasResults && loadedForSenders != latestSenders

data class OrderWorkflowUiState(
    val loading: Boolean,
    val statusText: String,
    val overviewResults: List<SenderOverview> = emptyList(),
    val parsedExcel: ParsedExcel? = null,
    val orderQuantities: Map<Int, Int> = emptyMap(),
    val orderSentStamp: Int = 0,
    val sendingOrders: Boolean = false,
    val orderSendError: String? = null,
    val orderLoadingProgress: Pair<Int, Int>? = null
)

@HiltViewModel
class OrderWorkflowViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val senderRepository: SenderRepository,
    private val gmailRepository: GmailRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OrderWorkflowUiState(
            loading = false,
            statusText = context.getString(R.string.gmail_status_idle)
        )
    )
    val uiState: StateFlow<OrderWorkflowUiState> = _uiState.asStateFlow()

    private val _authorizationRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authorizationRequests: SharedFlow<Unit> = _authorizationRequests.asSharedFlow()

    private var pendingAuthorization: PendingAuthorization? = null

    // Senders the current overview results were loaded for. Null means "no fresh results":
    // either nothing was loaded yet or the cache was invalidated by a config change.
    private var overviewLoadedForSenders: List<SenderEntity>? = null

    init {
        viewModelScope.launch {
            // Any config change (add / edit / delete / QR import) flows through
            // SenderRepository – drop overview results loaded for older configs so the
            // overview never shows Gmail data fetched for stale sender emails.
            // Empty results re-trigger a fresh load via the overview auto-load.
            senderRepository.observeAll().collect { latestSenders ->
                if (
                    shouldInvalidateOverview(
                        loadedForSenders = overviewLoadedForSenders,
                        latestSenders = latestSenders,
                        hasResults = uiState.value.overviewResults.isNotEmpty()
                    )
                ) {
                    overviewLoadedForSenders = null
                    updateState {
                        it.copy(
                            overviewResults = emptyList(),
                            statusText = context.getString(R.string.gmail_status_idle)
                        )
                    }
                }
            }
        }
    }

    fun loadOverview() {
        viewModelScope.launch {
            val senders = senderRepository.getAll()
            if (senders.isEmpty()) {
                updateState {
                    it.copy(
                        overviewResults = emptyList(),
                        statusText = context.getString(R.string.overview_empty)
                    )
                }
                return@launch
            }
            pendingAuthorization = PendingAuthorization.Overview(senders)
            requestAuthorization(context.getString(R.string.gmail_requesting_readonly))
        }
    }

    fun startOrderForOverview(overview: SenderOverview) {
        viewModelScope.launch {
            val sender = senderRepository.getAll().firstOrNull { it.inboundEmail == overview.senderQuery }
            startOrder(listOf(OrderItem(overview, sender?.parser.orEmpty(), sender)))
        }
    }

    fun startOrderFromOverview() {
        viewModelScope.launch {
            val senders = senderRepository.getAll()
            val senderByEmail = senders.associateBy { it.inboundEmail }
            val items = uiState.value.overviewResults
                .filter { it.status == SenderOverview.Status.FOUND && it.messageId != null }
                .map { overview ->
                    val sender = senderByEmail[overview.senderQuery]
                    OrderItem(overview, sender?.parser.orEmpty(), sender)
                }
            startOrder(items)
        }
    }

    fun onQuantityChange(index: Int, quantity: Int) {
        updateState { it.copy(orderQuantities = it.orderQuantities + (index to quantity)) }
    }

    fun dismissParsed() {
        updateState {
            it.copy(
                parsedExcel = null,
                orderQuantities = emptyMap(),
                orderLoadingProgress = null
            )
        }
    }

    fun sendOrders() {
        val state = uiState.value
        val parsed = state.parsedExcel ?: return
        val chosen = parsed.rows.withIndex()
            .filter { (index, _) -> (state.orderQuantities[index] ?: 0) > 0 }
        if (chosen.isEmpty()) {
            updateState { it.copy(statusText = context.getString(R.string.review_empty)) }
            return
        }

        pendingAuthorization = PendingAuthorization.Send(chosen, state.orderQuantities)
        updateState { it.copy(sendingOrders = true, orderSendError = null) }
        requestAuthorization(context.getString(R.string.order_requesting_access))
    }

    fun onAuthorizationResult(accessToken: String?) {
        val action = pendingAuthorization ?: return
        pendingAuthorization = null
        if (accessToken.isNullOrBlank()) {
            failAuthorization(context.getString(R.string.auth_no_token))
            return
        }

        viewModelScope.launch(ioDispatcher) {
            when (action) {
                is PendingAuthorization.Overview -> loadOverview(action.senders, accessToken)
                is PendingAuthorization.Parse -> parsePriceLists(action.items, accessToken)
                is PendingAuthorization.Send -> sendOrders(
                    action.items,
                    action.quantities,
                    accessToken
                )
            }
        }
    }

    fun onAuthorizationFailure(message: String) {
        pendingAuthorization = null
        failAuthorization(message)
    }

    private fun startOrder(items: List<OrderItem>) {
        if (items.isEmpty()) {
            updateState { it.copy(statusText = context.getString(R.string.order_empty)) }
            return
        }
        pendingAuthorization = PendingAuthorization.Parse(items)
        updateState {
            it.copy(
                parsedExcel = null,
                orderQuantities = emptyMap(),
                orderLoadingProgress = null
            )
        }
        requestAuthorization(context.getString(R.string.gmail_requesting_access))
    }

    private fun requestAuthorization(status: String) {
        updateState { it.copy(loading = true, statusText = status) }
        _authorizationRequests.tryEmit(Unit)
    }

    private suspend fun loadOverview(senders: List<SenderEntity>, accessToken: String) {
        val label = if (senders.size == 1) senders.first().inboundEmail else "${senders.size} senders"
        updateState {
            it.copy(
                statusText = context.getString(
                    R.string.gmail_loading_emails,
                    label
                )
            )
        }
        try {
            val results = gmailRepository.loadOverviewForSenders(senders, accessToken)
            if (senderRepository.getAll() != senders) {
                // Config changed while loading – drop stale results and stay invalidated.
                updateState {
                    it.copy(
                        loading = false,
                        overviewResults = emptyList(),
                        statusText = context.getString(R.string.gmail_status_idle)
                    )
                }
                return
            }
            val found = results.count { it.status == SenderOverview.Status.FOUND }
            overviewLoadedForSenders = senders
            updateState {
                it.copy(
                    loading = false,
                    overviewResults = results,
                    statusText = context.getString(R.string.gmail_loaded_emails, found)
                )
            }
        } catch (error: Exception) {
            error.rethrowIfCancellation()
            AppLog.w(TAG, "Gmail lookup failed", error)
            updateState {
                it.copy(
                    loading = false,
                    statusText = context.getString(
                        R.string.gmail_lookup_failed,
                        error.localizedMessage.orEmpty()
                    )
                )
            }
        }
    }

    private suspend fun parsePriceLists(items: List<OrderItem>, accessToken: String) {
        updateState {
            it.copy(
                statusText = context.getString(
                    R.string.order_downloading_price_lists,
                    items.size
                )
            )
        }
        val isBulk = items.size > 1
        if (isBulk) {
            updateState { it.copy(orderLoadingProgress = 0 to items.size) }
        }

        try {
            val allRows = mutableListOf<ExcelRow>()
            val supplierWithDates = linkedMapOf<String, String>()
            val skipped = mutableListOf<String>()
            for ((index, item) in items.withIndex()) {
                coroutineContext.ensureActive()
                val attachment = item.overview.attachment
                if (attachment == null) {
                    skipped += item.overview.senderQuery
                    publishProgress(supplierWithDates.size, items.size, isBulk)
                    continue
                }
                val file = gmailRepository.downloadAttachment(
                    item.overview.messageId.orEmpty(),
                    attachment,
                    accessToken
                )
                val emailDate = item.overview.date?.substringBefore(" ")?.trim()?.takeIf {
                    it.matches(Regex("""\d{2}\.\d{2}\.\d{4}"""))
                }?.replace('.', '-')
                    ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(file.lastModified()))
                val parsed = ExcelParser.parseFile(file, item.parser, emailDate)
                if (parsed == null) {
                    skipped += item.overview.senderQuery
                    publishProgress(supplierWithDates.size, items.size, isBulk)
                    continue
                }

                val receiver = item.sender?.outboundEmail.orEmpty()
                val company = item.sender?.companyName.orEmpty()
                allRows += parsed.rows.map { it.copy(receiver = receiver, company = company) }
                supplierWithDates.putIfAbsent(parsed.supplier, emailDate)

                if (allRows.isNotEmpty() && isBulk) {
                    val supplierSummary = supplierSummary(supplierWithDates)
                    updateState {
                        it.copy(
                            parsedExcel = ParsedExcel(supplierSummary, "", allRows.toList()),
                            orderLoadingProgress = supplierWithDates.size to items.size,
                            statusText = context.getString(
                                R.string.order_downloading_price_lists,
                                items.size
                            ) + " (${supplierWithDates.size}/${items.size})"
                        )
                    }
                    if (index == 0) delay(FIRST_RESULT_DISPLAY_MILLIS)
                }
            }

            if (allRows.isEmpty()) {
                updateState {
                    it.copy(
                        loading = false,
                        orderLoadingProgress = null,
                        statusText = context.getString(R.string.order_download_failed)
                    )
                }
                return
            }

            val skipNote = if (skipped.isEmpty()) {
                ""
            } else {
                context.getString(R.string.order_skipped, skipped.size, skipped.joinToString(", "))
            }
            updateState {
                it.copy(
                    loading = false,
                    parsedExcel = ParsedExcel(
                        supplierSummary(supplierWithDates),
                        "",
                        allRows.toList()
                    ),
                    orderQuantities = emptyMap(),
                    orderLoadingProgress = null,
                    statusText = context.getString(
                        R.string.order_parsed,
                        allRows.size,
                        supplierWithDates.size,
                        skipNote
                    )
                )
            }
        } catch (error: Exception) {
            error.rethrowIfCancellation()
            AppLog.w(TAG, "Order download or parse failed", error)
            updateState {
                it.copy(
                    loading = false,
                    orderLoadingProgress = null,
                    statusText = context.getString(
                        R.string.order_parse_failed,
                        error.localizedMessage.orEmpty()
                    )
                )
            }
        }
    }

    private fun publishProgress(loaded: Int, total: Int, isBulk: Boolean) {
        if (isBulk) updateState { it.copy(orderLoadingProgress = loaded to total) }
    }

    private suspend fun sendOrders(
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
                    gmailRepository.sendMessage(
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
                failSend(errorNote, statusText = "")
            } else {
                updateState {
                    it.copy(
                        loading = false,
                        sendingOrders = false,
                        orderSendError = null,
                        orderSentStamp = it.orderSentStamp + 1,
                        statusText = ""
                    )
                }
            }
        } catch (error: Exception) {
            error.rethrowIfCancellation()
            AppLog.w(TAG, "Order sending failed", error)
            failSend(
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

    private fun failAuthorization(message: String) {
        if (uiState.value.sendingOrders) {
            failSend(message)
        } else {
            updateState { it.copy(loading = false, statusText = message) }
        }
    }

    private fun failSend(message: String, statusText: String = message) {
        updateState {
            it.copy(
                loading = false,
                sendingOrders = false,
                orderSendError = message,
                statusText = statusText
            )
        }
    }

    private fun supplierSummary(suppliers: Map<String, String>): String =
        suppliers.entries.joinToString(", ") { "${it.key} (${it.value})" }

    private fun htmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun Exception.rethrowIfCancellation() {
        if (this is CancellationException) throw this
    }

    private fun updateState(transform: (OrderWorkflowUiState) -> OrderWorkflowUiState) {
        _uiState.update(transform)
    }

    private sealed interface PendingAuthorization {
        data class Overview(val senders: List<SenderEntity>) : PendingAuthorization
        data class Parse(val items: List<OrderItem>) : PendingAuthorization
        data class Send(
            val items: List<IndexedValue<ExcelRow>>,
            val quantities: Map<Int, Int>
        ) : PendingAuthorization
    }

    private data class OrderItem(
        val overview: SenderOverview,
        val parser: String,
        val sender: SenderEntity?
    )

    private companion object {
        const val TAG = "OrderWorkflowViewModel"
        const val FIRST_RESULT_DISPLAY_MILLIS = 900L
    }
}
