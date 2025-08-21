package com.aci.perp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.example.gyrosopicexposer.Models.SensorPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MainActivityViewModel : ViewModel() {
    private val _dataAccelerometer = MutableLiveData<FloatArray>()
    val dataAccelerometer: LiveData<FloatArray> = _dataAccelerometer

    private val _dataMagneticField = MutableLiveData<FloatArray>()
    val dataMagneticField: LiveData<FloatArray> = _dataMagneticField

    private val _dataGravity = MutableLiveData<FloatArray>()
    val dataGravity: LiveData<FloatArray> = _dataGravity

    fun updateAcceleration(data: FloatArray) {
        _dataAccelerometer.postValue(data)
    }

    fun updateMagneticField(data: FloatArray) {
        _dataMagneticField.postValue(data)
    }

    fun updateGravity(data: FloatArray) {
        _dataGravity.postValue(data)
    }

    fun watchValues(): Flow<SensorPack> {
        return combine(
            dataAccelerometer.asFlow(),
            dataMagneticField.asFlow(),
            dataGravity.asFlow()
        ) { dataA, dataB, dataC ->
            SensorPack(dataA, dataB, dataC)
        }
    }
}