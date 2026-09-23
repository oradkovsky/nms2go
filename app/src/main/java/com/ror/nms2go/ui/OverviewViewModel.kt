package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.data.SenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Presentation-only data used by [OverviewScreen]. Domain and persistence models stay behind
 * this boundary so the screen only has to render its current state.
 */
data class OverviewUiModel(
    val hasConfiguredSenders: Boolean = false,
    val isLoading: Boolean = false,
    val statusText: String = "",
    val items: List<OverviewUiItem> = emptyList()
) {
    val showEmptyState: Boolean
        get() = !isLoading && !hasConfiguredSenders

    val showStatus: Boolean
        get() = hasConfiguredSenders
}

data class OverviewUiItem(
    val senderQuery: String,
    val companyName: String?,
    val subject: String?,
    val date: String?,
    val status: OverviewUiItemStatus
) {
    val isParseable: Boolean
        get() = status == OverviewUiItemStatus.FOUND
}

enum class OverviewUiItemStatus {
    FOUND,
    NO_MESSAGES,
    NO_ATTACHMENTS
}

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val senderRepository: SenderRepository
) : ViewModel() {

    private val _uiModel = MutableStateFlow(OverviewUiModel())
    val uiModel: StateFlow<OverviewUiModel> = _uiModel.asStateFlow()

    // A Channel keeps the initial auto-load request until the navigation collector is active.
    private val _loadRequests = Channel<Unit>(Channel.CONFLATED)
    val loadRequests = _loadRequests.receiveAsFlow()

    private val _parseItemRequests = MutableSharedFlow<SenderOverview>(extraBufferCapacity = 1)
    val parseItemRequests: SharedFlow<SenderOverview> = _parseItemRequests.asSharedFlow()

    private var senders: List<SenderEntity> = emptyList()
    private var overviewResults: List<SenderOverview> = emptyList()
    private var loading: Boolean = false
    private var statusText: String = ""
    private var autoLoadRequestedForEmptyResults = false

    init {
        viewModelScope.launch {
            senderRepository.observeAll().collect {
                senders = it
                rebuild()
            }
        }
    }

    fun updateData(
        loading: Boolean,
        statusText: String,
        overviewResults: List<SenderOverview>
    ) {
        this.loading = loading
        this.statusText = statusText
        this.overviewResults = overviewResults
        rebuild()
    }

    fun onItemClicked(senderQuery: String) {
        overviewResults.firstOrNull {
            it.senderQuery == senderQuery && it.status == SenderOverview.Status.FOUND
        }?.let(_parseItemRequests::tryEmit)
    }

    private fun rebuild() {
        val currentSenders = senders
        val currentResults = overviewResults
        _uiModel.value = OverviewUiModel(
            hasConfiguredSenders = currentSenders.isNotEmpty(),
            isLoading = loading,
            statusText = statusText,
            items = currentResults.map { overview ->
                OverviewUiItem(
                    senderQuery = overview.senderQuery,
                    companyName = currentSenders.firstOrNull { it.email == overview.senderQuery }?.companyName,
                    subject = overview.subject?.let { displaySubject(it, overview.date) },
                    date = overview.date,
                    status = overview.status.toUiItemStatus()
                )
            }
        )
        requestInitialLoadIfNeeded(currentSenders.isNotEmpty(), loading, currentResults)
    }

    private fun requestInitialLoadIfNeeded(
        hasConfiguredSenders: Boolean,
        loading: Boolean,
        results: List<SenderOverview>
    ) {
        if (!hasConfiguredSenders || results.isNotEmpty()) {
            autoLoadRequestedForEmptyResults = false
            return
        }
        if (!loading && !autoLoadRequestedForEmptyResults) {
            autoLoadRequestedForEmptyResults = true
            _loadRequests.trySend(Unit)
        }
    }

    private fun SenderOverview.Status.toUiItemStatus() = when (this) {
        SenderOverview.Status.FOUND -> OverviewUiItemStatus.FOUND
        SenderOverview.Status.NO_MESSAGES -> OverviewUiItemStatus.NO_MESSAGES
        SenderOverview.Status.NO_ATTACHMENTS -> OverviewUiItemStatus.NO_ATTACHMENTS
    }
}

/** Removes the date suffix when it duplicates the separately rendered date. */
private fun displaySubject(subject: String, date: String?): String {
    if (date.isNullOrBlank()) return subject
    val exactSuffix = " ($date)"
    if (subject.endsWith(exactSuffix)) return subject.removeSuffix(exactSuffix)

    val dateOnly = date.substringBefore(" ")
    if (dateOnly != date && subject.endsWith(" ($dateOnly)")) {
        return subject.removeSuffix(" ($dateOnly)")
    }
    return subject.replace(Regex("""\s*\(\s*\d{2}[.\-]\d{2}[.\-]\d{4}[^)]*\)\s*$"""), "")
}
