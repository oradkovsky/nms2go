package com.ror.nms2go.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import com.ror.nms2go.domain.SaveSenderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI model for the config detail screen. The screen never sees Room types —
 * mapping from [SenderEntity] happens here, in [ConfigDetailViewModel].
 */
data class ConfigDetailItem(
    val id: Long = -1L,
    val company: String = "",
    val email: String = "",
    val receiver: String = "",
    val parser: String = "",
    val skipKeywords: String = ""
)

sealed interface ConfigDetailUiState {
    data object Loading : ConfigDetailUiState
    data class Content(val item: ConfigDetailItem, val isEditing: Boolean) : ConfigDetailUiState
    data object NotFound : ConfigDetailUiState
}

@HiltViewModel
class ConfigDetailViewModel @Inject constructor(
    private val senderRepository: SenderRepository,
    private val saveSenderUseCase: SaveSenderUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val senderId: Long = savedStateHandle.get<Long>("senderId") ?: -1L

    private val _uiState: MutableStateFlow<ConfigDetailUiState> = MutableStateFlow(
        if (senderId == -1L) {
            ConfigDetailUiState.Content(ConfigDetailItem(), isEditing = false)
        } else {
            ConfigDetailUiState.Loading
        }
    )
    val uiState: StateFlow<ConfigDetailUiState> = _uiState.asStateFlow()

    init {
        if (senderId != -1L) {
            viewModelScope.launch {
                val entity = senderRepository.getById(senderId)
                _uiState.value = if (entity == null) {
                    ConfigDetailUiState.NotFound
                } else {
                    ConfigDetailUiState.Content(entity.toUiItem(), isEditing = true)
                }
            }
        }
    }

    fun save(
        companyName: String,
        email: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String = ""
    ) {
        viewModelScope.launch {
            saveSenderUseCase(
                senderId = senderId,
                companyName = companyName,
                email = email,
                receiverEmail = receiverEmail,
                parser = parser,
                skipKeywords = skipKeywords
            )
        }
    }

    fun delete() {
        if (senderId == -1L) return
        viewModelScope.launch {
            senderRepository.deleteById(senderId)
        }
    }

    private fun SenderEntity.toUiItem() = ConfigDetailItem(
        id = id,
        company = companyName,
        email = email,
        receiver = receiverEmail,
        parser = parser,
        skipKeywords = skipKeywords
    )
}
