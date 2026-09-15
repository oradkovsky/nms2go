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

@HiltViewModel
class ConfigDetailViewModel @Inject constructor(
    private val senderRepository: SenderRepository,
    private val saveSenderUseCase: SaveSenderUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val senderId: Long = savedStateHandle.get<Long>("senderId") ?: -1L

    val isEditing: Boolean = senderId != -1L

    private val _sender = MutableStateFlow<SenderEntity?>(null)
    val sender: StateFlow<SenderEntity?> = _sender.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                _sender.value = senderRepository.getById(senderId)
            }
        }
    }

    fun save(companyName: String, email: String, receiverEmail: String, parser: String) {
        viewModelScope.launch {
            saveSenderUseCase(
                senderId = senderId,
                companyName = companyName,
                email = email,
                receiverEmail = receiverEmail,
                parser = parser
            )
        }
    }

    fun delete() {
        if (!isEditing) return
        viewModelScope.launch {
            senderRepository.deleteById(senderId)
        }
    }
}
