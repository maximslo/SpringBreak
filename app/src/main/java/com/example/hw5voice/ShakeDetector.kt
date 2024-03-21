package com.example.hw5voice

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ShakeDetector : SensorEventListener {

    private var shakeListener: OnShakeListener? = null

    interface OnShakeListener {
        fun onShake()
    }

    fun setOnShakeListener(listener: OnShakeListener) {
        this.shakeListener = listener
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used in this example
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val accelerationSquareRoot = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)
            if (accelerationSquareRoot >= 2) { // Change this threshold based on your sensitivity preferences
                shakeListener?.onShake()
            }
        }
    }
}
