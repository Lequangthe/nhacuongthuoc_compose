package com.quangthe.nhacnho_uongthuoc

import androidx.lifecycle.*
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.launch

class PillViewModel(private val repository: PillRepository) : ViewModel() {

    val allPills: LiveData<List<Pill>> = repository.allPills.asLiveData()
    val trashPills: LiveData<List<Pill>> = repository.trashPills.asLiveData()

    fun insert(pill: Pill) = viewModelScope.launch {
        repository.insertPill(pill)
    }

    fun update(pill: Pill) = viewModelScope.launch {
        repository.updatePill(pill)
    }

    fun softDelete(pill: Pill) = viewModelScope.launch {
        repository.softDeletePill(pill)
    }

    fun restore(pill: Pill) = viewModelScope.launch {
        repository.restorePill(pill)
    }

    fun deletePermanently(pill: Pill) = viewModelScope.launch {
        repository.deletePill(pill)
    }
}

class PillViewModelFactory(private val repository: PillRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PillViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
