package br.com.unisal.gustavo.acelerometro

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.unisal.gustavo.acelerometro.ui.theme.AcelerometroTheme
import kotlin.math.pow
import kotlin.math.sqrt

//public class MainActivity extends ComponentActivity implements SensorEventListener
class MainActivity : ComponentActivity(), SensorEventListener {
    var sensorManager: SensorManager? = null
    var sensor: Sensor? = null

    val gravidade = FloatArray(3)
    val aceleracaoLinear = FloatArray(3)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE) equivalente em java
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        /*
         * if (sensorManager != null) {
         *     sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
         * }
         */

        setContent {
            AcelerometroTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onSensorChanged(evento: SensorEvent?) {
        if(evento?.sensor?.type != Sensor.TYPE_ACCELEROMETER)
            return

        val x = evento.values[0]
        val y = evento.values[1]
        val z = evento.values[2]

        val alpha : Float = 0.8f

        gravidade[0] = alpha * gravidade[0] + (1 - alpha) * x
        gravidade[1] = alpha * gravidade[1] + (1 - alpha) * y
        gravidade[2] = alpha * gravidade[2] + (1 - alpha) * z

        aceleracaoLinear[0] = x - gravidade[0]
        aceleracaoLinear[1] = y - gravidade[1]
        aceleracaoLinear[2] = z - gravidade[2]

        val acelGravidade = sqrt(gravidade[0].pow(2) + gravidade[1].pow(2) +
                gravidade[2].pow(2))
        val acelLinear = sqrt(aceleracaoLinear[0].pow(2) +
                aceleracaoLinear[1].pow(2) + aceleracaoLinear[2].pow(2))
    }
}