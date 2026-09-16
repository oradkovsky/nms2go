package com.ror.nms2go.ui

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
class OrdersHistoryViewModel @Inject constructor(
    orderHistoryRepository: OrderHistoryRepository
) : ViewModel() {

    val orders: StateFlow<List<SentOrderWithItems>> = orderHistoryRepository.observeAllOrders()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}
