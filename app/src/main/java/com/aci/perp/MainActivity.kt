package com.aci.perp

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aci.perp.databinding.MainLayoutBinding
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity(), SensorEventListener2 {

    private object ScreenSizeHelper {
        fun getScreenDimensions(windowManager: WindowManager): Pair<Int?, Int?> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Get the window metrics

                val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics

                // Get the insets and bounds of the window
                val insets: WindowInsets = windowMetrics.windowInsets
                val insetLeft: Int =
                    insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).left
                val insetRight: Int =
                    insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).right
                val insetTop: Int =
                    insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top
                val insetBottom: Int =
                    insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom

                // Calculate the width and height of the screen
                val width = windowMetrics.bounds.width() - insetLeft - insetRight
                val height = windowMetrics.bounds.height() - insetTop - insetBottom

                // Return the width and height
                return Pair(width, height)
            } else {
                val displayMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
        }
    }

    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: MainLayoutBinding

    private lateinit var sensorManager: SensorManager
    private lateinit var windowManager: WindowManager

    val mRotationMatrix = FloatArray(9)
    val mInclinationMatrix = FloatArray(9)
    val mOrientationAngles = FloatArray(3)

    private var staticHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val dims = ScreenSizeHelper.getScreenDimensions(windowManager)

        val p = binding.level.layoutParams
        staticHeight = dims.second ?: 0
        p.height = staticHeight

        mRotationMatrix[ 0] = 1f
        mRotationMatrix[ 3] = 1f
        mRotationMatrix[ 6] = 1f

        binding.level.pivotY = 0f
        dims.second!!.also { p.width = (it * 3f).toInt() }
        (p.width).also { binding.level.pivotX = (it / 2).toFloat() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.watchValues().collect { value ->
                    updateUI(value.accelerometer, value.magneticField, value.gravity)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()

        //todo: make these configurable
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.also { gravity ->
            sensorManager.registerListener(
                this,
                gravity,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
    }

    override fun onFlushCompleted(p0: Sensor?) {
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mainActivityViewModel.updateAcceleration(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mainActivityViewModel.updateMagneticField(event.values)
            }
            Sensor.TYPE_GRAVITY -> {
                mainActivityViewModel.updateGravity(event.values)
            }
        }
    }

    fun formatDataLoggingOutputString(data: Float) = String.format("%.1f", data.also { Math.toDegrees(it.toDouble()) })
    fun formatAngles(data: Float): Float = data.also { Math.toDegrees(it.toDouble()) } * 100.0f

    @SuppressLint("SetTextI18n")
    fun updateUI(accelerometerReading: FloatArray, magnetometerReading: FloatArray, gravityReading: FloatArray) {
        SensorManager.getRotationMatrix(
            mRotationMatrix,
            mInclinationMatrix,
            accelerometerReading,
            magnetometerReading
        )

        SensorManager.remapCoordinateSystem(
            mRotationMatrix,
            SensorManager.AXIS_MINUS_Z,
            SensorManager.AXIS_X,
            mRotationMatrix
        )

        //I cannot for the life of me figure out how to read angles exclusively when the phone rotates side to side and not
        //back and forth
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles)

        var delta = (Math.toDegrees(mOrientationAngles[1].toDouble())) / 180
        //delta *= -1
        val calculatedOffset = (staticHeight * delta).toInt().toFloat()

        val startingPosition = binding.divider.y + staticHeight / 2
        binding.level.y = startingPosition - calculatedOffset
        binding.level.rotation = Math.toDegrees((2*Math.PI) * (gravityReading[0] / 9.81f).toDouble()).toFloat()/4

        Log.d("rotation", "${binding.level.rotation}")
        binding.tvAngularReadout.text = "${String.format("%.1f", mOrientationAngles[1].also { Math.toDegrees(it.toDouble()) } * 100.0)}°"

        //Test logging
        binding.tvSensor1.text = "delta: $delta"
        binding.tvSensor2.text = "offset: $calculatedOffset"
        binding.tvSensor3.text = "inclination: ${SensorManager.getInclination(mInclinationMatrix)}"
        binding.tvSensor4.text = "mRotationMatrix0 (rads): ${formatDataLoggingOutputString(mRotationMatrix[0])}"
        binding.tvSensor5.text = "mRotationMatrix1 (rads): ${formatDataLoggingOutputString(mRotationMatrix[1])}"
        binding.tvSensor6.text = "mRotationMatrix2 (rads): ${formatDataLoggingOutputString(mRotationMatrix[2])}"
        binding.tvSensor7.text = "mRotationMatrix3 (rads): ${formatDataLoggingOutputString(mRotationMatrix[3])}"
        binding.tvSensor8.text = "mRotationMatrix4 (rads): ${formatDataLoggingOutputString(mRotationMatrix[4])}"
        binding.tvSensor9.text = "mRotationMatrix5 (rads): ${formatDataLoggingOutputString(mRotationMatrix[5])}"
        binding.tvSensor10.text = "mRotationMatrix6 (rads): ${formatDataLoggingOutputString(mRotationMatrix[6])}"
        binding.tvSensor11.text = "mRotationMatrix7 (rads): ${formatDataLoggingOutputString(mRotationMatrix[7])}"
        binding.tvSensor12.text = "mRotationMatrix9 (rads): ${formatDataLoggingOutputString(mRotationMatrix[8])}"
        binding.tvSensor13.text = "mOrientationAngles0 (rads): ${formatDataLoggingOutputString(mOrientationAngles[0])}"
        binding.tvSensor14.text = "mOrientationAngles1 (rads): ${formatDataLoggingOutputString(mOrientationAngles[1])}"
        binding.tvSensor15.text = "mOrientationAngles2 (rads): ${formatDataLoggingOutputString(mOrientationAngles[2])}"
        binding.tvSensor16.text = "mRotationMatrix0 (°): ${formatAngles(mRotationMatrix[0])}"
        binding.tvSensor17.text = "mRotationMatrix1 (°): ${formatAngles(mRotationMatrix[1])}"
        binding.tvSensor18.text = "mRotationMatrix2 (°): ${formatAngles(mRotationMatrix[2])}"
        binding.tvSensor19.text = "mRotationMatrix3 (°): ${formatAngles(mRotationMatrix[3])}"
        binding.tvSensor20.text = "mRotationMatrix4 (°): ${formatAngles(mRotationMatrix[4])}"
        binding.tvSensor21.text = "mRotationMatrix5 (°): ${formatAngles(mRotationMatrix[5])}"
        binding.tvSensor22.text = "mRotationMatrix6 (°): ${formatAngles(mRotationMatrix[6])}"
        binding.tvSensor23.text = "mRotationMatrix7 (°): ${formatAngles(mRotationMatrix[7])}"
        binding.tvSensor24.text = "mRotationMatrix9 (°): ${formatAngles(mRotationMatrix[8])}"
        binding.tvSensor25.text = "mOrientationAngles0 (°): ${formatAngles(mOrientationAngles[0])}"
        binding.tvSensor26.text = "mOrientationAngles1 (°): ${formatAngles(mOrientationAngles[1])}"
        binding.tvSensor27.text = "mOrientationAngles2 (°): ${formatAngles(mOrientationAngles[2])}"
        binding.tvSensor28.text = "gravityReading0 (°): ${gravityReading[0]}"
        binding.tvSensor29.text = "gravityReading1 (°): ${gravityReading[1]}"
        binding.tvSensor30.text = "gravityReading2 (°): ${gravityReading[2]}"
    }
}