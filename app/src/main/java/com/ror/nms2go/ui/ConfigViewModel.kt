package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ConfigViewModel @Inject constructor(
    senderRepository: SenderRepository
) : ViewModel() {

    val senders: StateFlow<List<SenderEntity>> = senderRepository.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}
