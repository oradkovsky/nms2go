package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.QrSender
import com.ror.nms2go.di.DefaultDispatcher
import com.ror.nms2go.domain.ImportSendersFromQrResult
import com.ror.nms2go.domain.ImportSendersFromQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QrScanUiState {
    data object Idle : QrScanUiState
    data class Error(val message: String) : QrScanUiState
    data class Success(val items: List<QrSender>) : QrScanUiState
}

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val importSendersFromQrUseCase: ImportSendersFromQrUseCase,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrScanUiState>(QrScanUiState.Idle)
    val uiState: StateFlow<QrScanUiState> = _uiState.asStateFlow()

    /**
     * Handles raw QR string. Delegates decode + persist to [ImportSendersFromQrUseCase]
     * and maps the result to single uiState (Idle/Error/Success).
     *
     * Runs off the main thread: the scanner callback arrives on the main executor,
     * so JSON parsing and DB work are dispatched to [defaultDispatcher].
     */
    fun handleRawScanned(rawValue: String) {
        viewModelScope.launch(defaultDispatcher) {
            when (val result = importSendersFromQrUseCase(rawValue)) {
                is ImportSendersFromQrResult.Success ->
                    _uiState.value = QrScanUiState.Success(result.items)
                ImportSendersFromQrResult.InvalidQr ->
                    _uiState.value = QrScanUiState.Error("Invalid or empty QR code")
            }
        }
    }

    fun consumeSuccess() {
        if (_uiState.value is QrScanUiState.Success) {
            _uiState.value = QrScanUiState.Idle
        }
    }
}
