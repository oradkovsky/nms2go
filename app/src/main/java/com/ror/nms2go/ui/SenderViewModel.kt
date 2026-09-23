package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(
    senderDao: SenderDao
) : ViewModel() {

    val senders: StateFlow<List<SenderEntity>> = senderDao.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList()
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}