package com.ror.nms2go.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.OrderHistoryRepository
import com.ror.nms2go.data.SentOrderWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    orderHistoryRepository: OrderHistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: Long = savedStateHandle.get<Long>("orderId") ?: -1L

    val order: StateFlow<SentOrderWithItems?> = orderHistoryRepository.observeOrder(orderId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )
}
