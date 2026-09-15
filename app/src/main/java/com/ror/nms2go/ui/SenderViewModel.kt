package com.ror.nms2go.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ror.nms2go.data.OrderDao
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SentOrderWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(
    private val senderDao: SenderDao,
    orderDao: OrderDao
) : ViewModel() {

    val senders: StateFlow<List<SenderEntity>> = senderDao.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList()
        )

    val orders: StateFlow<List<SentOrderWithItems>> = orderDao.observeAllOrders()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList()
        )

    fun addSender(companyName: String, email: String, receiverEmail: String, parser: String) {
        val company = companyName.trim()
        val normalizedEmail = email.trim()
        val receiver = receiverEmail.trim()
        val parserValue = parser.trim()
        if (company.isBlank() || normalizedEmail.isBlank()) return
        viewModelScope.launch {
            senderDao.insert(
                SenderEntity(
                    companyName = company,
                    email = normalizedEmail,
                    receiverEmail = receiver,
                    parser = parserValue
                )
            )
        }
    }

    fun updateSender(
        id: Long,
        companyName: String,
        email: String,
        receiverEmail: String,
        parser: String
    ) {
        val existing = senders.value.firstOrNull { it.id == id } ?: return
        val company = companyName.trim()
        val normalizedEmail = email.trim()
        val receiver = receiverEmail.trim()
        val parserValue = parser.trim()
        if (company.isBlank() || normalizedEmail.isBlank()) return
        viewModelScope.launch {
            senderDao.update(
                existing.copy(
                    companyName = company,
                    email = normalizedEmail,
                    receiverEmail = receiver,
                    parser = parserValue
                )
            )
        }
    }

    fun removeSender(id: Long) {
        viewModelScope.launch {
            senderDao.deleteById(id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}