package com.ror.nms2go.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ror.nms2go.ExcelRow
import com.ror.nms2go.ParsedExcel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ParsedUiState {
    data object Idle : ParsedUiState
    data class Loading(val progress: Pair<Int, Int>? = null) : ParsedUiState
    data class Content(
        val parsed: ParsedExcel,
        val query: String = "",
        val page: Int = 0,
        val totalPages: Int = 1,
        val filteredSize: Int = 0,
        val pageItems: List<IndexedValue<ExcelRow>> = emptyList(),
        val summaryText: String = "",
        val quantities: Map<Int, Int> = emptyMap(),
        val isLoadingMore: Boolean = false,
        val orderLoadingProgress: Pair<Int, Int>? = null
    ) : ParsedUiState
}

class ParsedViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _parsed = MutableStateFlow<ParsedExcel?>(null)
    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val _isLoading = MutableStateFlow(false)
    private val _orderLoadingProgress = MutableStateFlow<Pair<Int, Int>?>(null)

    private val _query = MutableStateFlow(savedStateHandle.get<String>("query") ?: "")
    private val _page = MutableStateFlow(savedStateHandle.get<Int>("page") ?: 0)

    private val _quantityChangeEvent = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1)
    val quantityChangeEvent: SharedFlow<Pair<Int, Int>> = _quantityChangeEvent.asSharedFlow()

    private val _uiState = MutableStateFlow<ParsedUiState>(ParsedUiState.Idle)
    val uiState: StateFlow<ParsedUiState> = _uiState.asStateFlow()

    fun updateData(
        parsed: ParsedExcel?,
        quantities: Map<Int, Int>,
        isLoading: Boolean,
        orderLoadingProgress: Pair<Int, Int>?
    ) {
        _parsed.value = parsed
        _quantities.value = quantities
        _isLoading.value = isLoading
        _orderLoadingProgress.value = orderLoadingProgress
        // keep query/page from savedStateHandle
        updateDerivedState()
    }

    private fun updateDerivedState() {
        val parsed = _parsed.value
        val isLoading = _isLoading.value
        val progress = _orderLoadingProgress.value
        if (parsed == null) {
            _uiState.value = if (isLoading) ParsedUiState.Loading(progress) else ParsedUiState.Idle
            return
        }
        val query = _query.value
        val page = _page.value
        val quantities = _quantities.value

        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val filteredWithIndex = parsed.rows.withIndex()
            .filter { (_, row) ->
                tokens.isEmpty() || tokens.all { token ->
                    row.article.contains(
                        token,
                        ignoreCase = true
                    )
                }
            }
            .sortedWith(
                compareBy<IndexedValue<ExcelRow>> { it.value.price ?: Double.MAX_VALUE }
                    .thenBy { it.value.counteragent.lowercase() }
            )

        val totalPages = maxOf((filteredWithIndex.size + PAGE_SIZE - 1) / PAGE_SIZE, 1)
        val safePage = page.coerceIn(0, totalPages - 1)
        val pageItems = filteredWithIndex.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)

        val summaryText = if (parsed.dateAsString.isBlank()) {
            "${parsed.supplier} · ${parsed.rows.size}"
        } else {
            "${parsed.supplier} · ${parsed.dateAsString} · ${parsed.rows.size}"
        }

        _uiState.value = ParsedUiState.Content(
            parsed = parsed,
            query = query,
            page = safePage,
            totalPages = totalPages,
            filteredSize = filteredWithIndex.size,
            pageItems = pageItems,
            summaryText = summaryText,
            quantities = quantities,
            isLoadingMore = _isLoading.value,
            orderLoadingProgress = _orderLoadingProgress.value
        )
        // persist page/query
        savedStateHandle["query"] = query
        savedStateHandle["page"] = safePage
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _page.value = 0
        savedStateHandle["query"] = newQuery
        savedStateHandle["page"] = 0
        updateDerivedState()
    }

    fun onClearQuery() {
        onQueryChange("")
    }

    fun onPageChange(newPage: Int) {
        _page.value = newPage
        savedStateHandle["page"] = newPage
        updateDerivedState()
    }

    fun onQuantityChange(index: Int, quantity: Int) {
        val coerced = quantity.coerceIn(0, MAX_QUANTITY)
        _quantityChangeEvent.tryEmit(index to coerced)
        // Optimistic local update for immediate resort (will be overwritten by parent's updateData)
        val newMap = _quantities.value.toMutableMap()
        newMap[index] = coerced
        _quantities.value = newMap
        updateDerivedState()
    }
}
