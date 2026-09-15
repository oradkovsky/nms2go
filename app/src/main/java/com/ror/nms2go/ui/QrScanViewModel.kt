package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.QrCodec
import com.ror.nms2go.data.QrSender
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val senderDao: SenderDao,
    private val senderRepository: SenderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrScanUiState>(QrScanUiState.Idle)
    val uiState: StateFlow<QrScanUiState> = _uiState.asStateFlow()

    /**
     * Handles raw QR string. Decodes via QrCodec and persists via SenderDao.
     * Updates single uiState (Idle/Error/Success) – caller observes uiState for rendering and navigation.
     */
    fun handleRawScanned(rawValue: String) {
        val items = QrCodec.decode(rawValue)
        if (items.isNullOrEmpty()) {
            _uiState.value = QrScanUiState.Error("Invalid or empty QR code")
            return
        }
        viewModelScope.launch {
            val existing = senderDao.getAll()
            val handledIds = mutableSetOf<Long>()
            val seenKeys = mutableSetOf<String>()
            for (item in items) {
                val company = item.company.trim()
                val email = item.email.trim()
                val receiver = item.receiver.trim()
                val parser = item.parser.trim()
                if (email.isBlank()) continue
                val key = "${email.lowercase()}|${receiver.lowercase()}"
                if (!seenKeys.add(key)) continue
                val match = existing.firstOrNull { sender ->
                    sender.id !in handledIds && (
                        sender.email.equals(email, ignoreCase = true) ||
                            (receiver.isNotBlank() && sender.receiverEmail.equals(receiver, ignoreCase = true))
                        )
                }
                if (match != null) {
                    handledIds += match.id
                    senderDao.update(
                        match.copy(
                            companyName = company,
                            email = email,
                            receiverEmail = receiver,
                            parser = parser
                        )
                    )
                } else {
                    senderRepository.insert(
                        SenderEntity(
                            companyName = company,
                            email = email,
                            receiverEmail = receiver,
                            parser = parser
                        )
                    )
                }
            }
            _uiState.value = QrScanUiState.Success(items)
        }
    }

    fun consumeSuccess() {
        if (_uiState.value is QrScanUiState.Success) {
            _uiState.value = QrScanUiState.Idle
        }
    }
}
