package com.zakharov.gpstracker

import androidx.lifecycle.*
import com.zakharov.gpstracker.db.MainDb
import com.zakharov.gpstracker.db.TrackItem
import com.zakharov.gpstracker.location.LocationModel
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class MainViewModel(db: MainDb) : ViewModel() {
    private val dao = db.getDao()
    val locationUpdates = MutableLiveData<LocationModel>()
    val currentTrack = MutableLiveData<TrackItem>()
    val timeData = MutableLiveData<String>()
    val tracks = dao.getAllTracks().asLiveData()

    fun insertTrack(trackItem: TrackItem) = viewModelScope.launch {
        dao.insertTrack(trackItem)
    }

    fun deleteTrack(trackItem: TrackItem) = viewModelScope.launch {
        dao.deleteTrack(trackItem)
    }


    class ViewModelFactory(private val db: MainDb) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(db) as T
            }
            throw java.lang.IllegalArgumentException("Unknown ViewModel class")
        }

    }
}