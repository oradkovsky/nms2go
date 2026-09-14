package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import com.ror.nms2go.ExcelRow
import com.ror.nms2go.ParsedExcel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ReviewUiState {
    data object Empty : ReviewUiState
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

class ReviewViewModel : ViewModel() {

    private val _parsed = MutableStateFlow<ParsedExcel?>(null)
    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val _sending = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _showConfirm = MutableStateFlow(false)

    private val _quantityChangeEvent = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1, replay = 0)
    val quantityChangeEvent: SharedFlow<Pair<Int, Int>> = _quantityChangeEvent.asSharedFlow()

    private val _orderRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val orderRequested: SharedFlow<Unit> = _orderRequested.asSharedFlow()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Empty)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        // Derive uiState from constituent flows – single source for screen rendering
        // Use combine-like manual update via updateDerivedState() called on each setter
    }

    fun updateData(
        parsed: ParsedExcel?,
        quantities: Map<Int, Int>,
        isSending: Boolean,
        error: String?
    ) {
        _parsed.value = parsed
        _quantities.value = quantities
        _sending.value = isSending
        _error.value = error
        updateDerivedState()
    }

    private fun updateDerivedState() {
        val error = _error.value
        if (!error.isNullOrBlank()) {
            _uiState.value = ReviewUiState.Error(message = error)
            return
        }
        val parsed = _parsed.value
        if (parsed == null) {
            _uiState.value = ReviewUiState.Empty
            return
        }
        val quantities = _quantities.value
        val chosen = parsed.rows.withIndex()
            .filter { (index, _) -> (quantities[index] ?: 0) > 0 }
            .sortedWith(
                compareBy<IndexedValue<ExcelRow>> { it.value.price ?: Double.MAX_VALUE }
                    .thenBy { it.value.counteragent.lowercase() }
            )
        if (chosen.isEmpty()) {
            _uiState.value = ReviewUiState.Empty
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
        val coerced = quantity.coerceIn(0, MAX_QUANTITY)
        _quantityChangeEvent.tryEmit(index to coerced)
        // Optimistically update local quantities for immediate UI resort – will be overwritten by parent's updateData on next frame
        val newMap = _quantities.value.toMutableMap()
        newMap[index] = coerced
        _quantities.value = newMap
        updateDerivedState()
    }

    fun onOrderRequested() {
        _showConfirm.value = true
        updateDerivedState()
    }

    fun onDismissConfirm() {
        _showConfirm.value = false
        updateDerivedState()
    }

    fun onConfirmOrder() {
        _showConfirm.value = false
        updateDerivedState()
        _orderRequested.tryEmit(Unit)
    }
}
