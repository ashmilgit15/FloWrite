package com.flowrite.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowrite.history.data.TranscriptionDao
import com.flowrite.history.data.TranscriptionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transcriptionDao: TranscriptionDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transcriptions: StateFlow<List<TranscriptionEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                transcriptionDao.getAll()
            } else {
                transcriptionDao.searchByText(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            transcriptionDao.deleteById(id)
        }
    }

    fun deleteAllTranscriptions() {
        viewModelScope.launch {
            transcriptionDao.deleteAll()
        }
    }
}
