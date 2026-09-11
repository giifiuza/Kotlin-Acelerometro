package br.com.unisal.giovanna.acelerometro

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.unisal.giovanna.acelerometro.ui.theme.AcelerometroTheme
import br.com.unisal.giovanna.acelerometro.ui.theme.AxisXColor
import br.com.unisal.giovanna.acelerometro.ui.theme.AxisYColor
import br.com.unisal.giovanna.acelerometro.ui.theme.AxisZColor
import br.com.unisal.giovanna.acelerometro.ui.theme.MotionIntenseColor
import br.com.unisal.giovanna.acelerometro.ui.theme.MotionLightColor
import br.com.unisal.giovanna.acelerometro.ui.theme.MotionRestColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val LOW_PASS_FILTER_ALPHA = 0.8f
private const val LIGHT_MOTION_THRESHOLD = 0.5f
private const val INTENSE_MOTION_THRESHOLD = 3f
private const val LINEAR_ACCELERATION_SCALE = 15f
private const val GRAVITY_SCALE = 10f
private const val AXIS_SCALE_MAX = 15f

private data class AccelerometerReading(
    val axisX: Float = 0f,
    val axisY: Float = 0f,
    val axisZ: Float = 0f,
    val gravityMagnitude: Float = 0f,
    val linearAccelerationMagnitude: Float = 0f,
    val isSensorAvailable: Boolean = true
)

/**
 *
 * Author: Giovanna Fiuza
 * RA: 240025585
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    /** Smoothed gravity vector, updated in place by the low-pass filter on every sample. */
    private val gravity = FloatArray(3)

    /** Latest sensor snapshot exposed to Compose; reassigning it triggers recomposition. */
    private var reading by mutableStateOf(AccelerometerReading())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            reading = reading.copy(isSensorAvailable = false)
        }

        setContent {
            AcelerometroTheme {
                AccelerometerScreen(reading = reading)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

   
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        gravity[0] = LOW_PASS_FILTER_ALPHA * gravity[0] + (1 - LOW_PASS_FILTER_ALPHA) * x
        gravity[1] = LOW_PASS_FILTER_ALPHA * gravity[1] + (1 - LOW_PASS_FILTER_ALPHA) * y
        gravity[2] = LOW_PASS_FILTER_ALPHA * gravity[2] + (1 - LOW_PASS_FILTER_ALPHA) * z

        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]

        reading = AccelerometerReading(
            axisX = x,
            axisY = y,
            axisZ = z,
            gravityMagnitude = vectorMagnitude(gravity[0], gravity[1], gravity[2]),
            linearAccelerationMagnitude = vectorMagnitude(linearX, linearY, linearZ),
            isSensorAvailable = true
        )
    }

    private fun vectorMagnitude(x: Float, y: Float, z: Float): Float =
        sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

// ---------------------------------------------------------------------------
// Compose UI
// ---------------------------------------------------------------------------
@Composable
private fun AccelerometerScreen(reading: AccelerometerReading) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.screen_subtitle), style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!reading.isSensorAvailable) {
            SensorUnavailableMessage(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Highest visual hierarchy: the two derived accelerations, side by side.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MagnitudeCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.linear_acceleration_label),
                    value = reading.linearAccelerationMagnitude,
                    maxValue = LINEAR_ACCELERATION_SCALE,
                    accentColor = motionColor(reading.linearAccelerationMagnitude),
                    statusText = motionLabel(reading.linearAccelerationMagnitude)
                )
                MagnitudeCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.gravity_label),
                    value = reading.gravityMagnitude,
                    maxValue = GRAVITY_SCALE,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }

            // Secondary hierarchy: the raw value of each axis.
            Text(
                text = stringResource(R.string.axes_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AxisValueCard(modifier = Modifier.weight(1f), axisName = "X", value = reading.axisX, color = AxisXColor)
                AxisValueCard(modifier = Modifier.weight(1f), axisName = "Y", value = reading.axisY, color = AxisYColor)
                AxisValueCard(modifier = Modifier.weight(1f), axisName = "Z", value = reading.axisZ, color = AxisZColor)
            }

            Text(
                text = stringResource(R.string.unit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun motionColor(magnitude: Float): Color = when {
    magnitude < LIGHT_MOTION_THRESHOLD -> MotionRestColor
    magnitude < INTENSE_MOTION_THRESHOLD -> MotionLightColor
    else -> MotionIntenseColor
}

@Composable
private fun motionLabel(magnitude: Float): String = when {
    magnitude < LIGHT_MOTION_THRESHOLD -> stringResource(R.string.motion_status_rest)
    magnitude < INTENSE_MOTION_THRESHOLD -> stringResource(R.string.motion_status_light)
    else -> stringResource(R.string.motion_status_intense)
}


@Composable
private fun MagnitudeCard(
    label: String,
    value: Float,
    maxValue: Float,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        label = "magnitude_fraction_$label"
    )
    val animatedColor by animateColorAsState(
        targetValue = accentColor,
        label = "magnitude_color_$label"
    )

    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier.padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier.size(92.dp),
                    color = animatedColor,
                    trackColor = animatedColor.copy(alpha = 0.15f),
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f", value),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "m/s²", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (statusText != null) {
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    color = animatedColor.copy(alpha = 0.15f),
                    contentColor = animatedColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun AxisValueCard(
    axisName: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (abs(value) / AXIS_SCALE_MAX).coerceIn(0f, 1f),
        label = "axis_${axisName}_fraction"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Eixo $axisName",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format(Locale.getDefault(), "%.2f", value),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "m/s²",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
    }
}

//caso erro
@Composable
private fun SensorUnavailableMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⚠", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sensor_unavailable_message),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
