/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.tiltspot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

class MainActivity : AppCompatActivity(), SensorEventListener {
    // System sensor manager instance.
    private var mSensorManager: SensorManager? = null
    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetometer: Sensor? = null
    // TextViews to display current sensor values.
    private var mTextSensorAzimuth: TextView? = null
    private var mTextSensorPitch: TextView? = null
    private var mTextSensorRoll: TextView? = null
    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)
    private var mSpotTop: ImageView? = null
    private var mSpotBottom: ImageView? = null
    private var mSpotLeft: ImageView? = null
    private var mSpotRight: ImageView? = null
    private var mDisplay: Display? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTextSensorAzimuth = findViewById<View>(R.id.value_azimuth) as TextView
        mTextSensorPitch = findViewById<View>(R.id.value_pitch) as TextView
        mTextSensorRoll = findViewById<View>(R.id.value_roll) as TextView
        mSpotTop = findViewById<View>(R.id.spot_top) as ImageView
        mSpotBottom = findViewById<View>(R.id.spot_bottom) as ImageView
        mSpotLeft = findViewById<View>(R.id.spot_left) as ImageView
        mSpotRight = findViewById<View>(R.id.spot_right) as ImageView
        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager
        mSensorAccelerometer = mSensorManager!!.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )
        mSensorMagnetometer = mSensorManager!!.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD
        )
        val wm =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = wm.defaultDisplay
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    override fun onStart() {
        super.onStart()
        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        if (mSensorMagnetometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorMagnetometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val sensorType = sensorEvent.sensor.type
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> mAccelerometerData =
                sensorEvent.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> mMagnetometerData =
                sensorEvent.values.clone()
        }
        val rotationMatrix = FloatArray(9)
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix,
            null, mAccelerometerData, mMagnetometerData
        )
        var rotationMatrixAdjusted = FloatArray(9)
        when (mDisplay!!.rotation) {
            Surface.ROTATION_0 -> rotationMatrixAdjusted = rotationMatrix.clone()
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                rotationMatrixAdjusted
            )
            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                rotationMatrixAdjusted
            )
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                rotationMatrixAdjusted
            )
        }
        val orientationValues = FloatArray(3)
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrixAdjusted, orientationValues)
        }
        val azimuth = orientationValues[0]
        var pitch = orientationValues[1]
        var roll = orientationValues[2]
        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0f
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0f
        }
        mTextSensorAzimuth!!.text = resources.getString(
            R.string.value_format, azimuth
        )
        mTextSensorPitch!!.text = resources.getString(
            R.string.value_format, pitch
        )
        mTextSensorRoll!!.text = resources.getString(
            R.string.value_format, roll
        )
        mSpotTop!!.alpha = 0f
        mSpotBottom!!.alpha = 0f
        mSpotLeft!!.alpha = 0f
        mSpotRight!!.alpha = 0f
        if (pitch > 0) {
            mSpotBottom!!.alpha = pitch
        } else {
            mSpotTop!!.alpha = Math.abs(pitch)
        }
        if (roll > 0) {
            mSpotLeft!!.alpha = roll
        } else {
            mSpotRight!!.alpha = Math.abs(roll)
        }
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}

    companion object {
        // Very small values for the accelerometer (on all three axes) should
        // be interpreted as 0. This value is the amount of acceptable
        // non-zero drift.
        private const val VALUE_DRIFT = 0.05f
    }
}