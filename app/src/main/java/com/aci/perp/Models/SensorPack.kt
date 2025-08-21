package com.example.gyrosopicexposer.Models

data class SensorPack(
    val accelerometer: FloatArray,
    val magneticField: FloatArray,
    val gravity: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorPack

        if (!accelerometer.contentEquals(other.accelerometer)) return false
        if (!magneticField.contentEquals(other.magneticField)) return false
        if (!gravity.contentEquals(other.gravity)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accelerometer.contentHashCode()
        result = 31 * result + magneticField.contentHashCode()
        result = 31 * result + gravity.contentHashCode()
        return result
    }
}
