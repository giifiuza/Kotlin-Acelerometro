package br.com.unisal.giovanna.acelerometro

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.unisal.giovanna.acelerometro.ui.theme.AcelerometroTheme
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt


private const val FILTRO_ALPHA = 0.8f

private const val LIMITE_MOVIMENTO_LEVE = 0.5f
private const val LIMITE_MOVIMENTO_INTENSO = 3f

// cores em tons de roxo 
private val RoxoEixoX = Color(0xFF7C4DFF)
private val RoxoEixoY = Color(0xFF9575CD)
private val RoxoEixoZ = Color(0xFFB39DDB)
private val RoxoGravidade = Color(0xFF512DA8)
private val RoxoRepouso = Color(0xFFB39DDB)
private val RoxoLeve = Color(0xFF7E57C2)
private val RoxoIntenso = Color(0xFF4527A0)

// guarda a ultima leitura do sensor pra mostrar na tela
private data class LeituraAcelerometro(
    val eixoX: Float = 0f,
    val eixoY: Float = 0f,
    val eixoZ: Float = 0f,
    val gravidade: Float = 0f,
    val aceleracaoLinear: Float = 0f,
    val sensorDisponivel: Boolean = true
)

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    // vetor de gravidade
    private val gravidade = FloatArray(3)

    // toda vez que muda, a tela atualiza sozinha
    private var leitura by mutableStateOf(LeituraAcelerometro())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // se o celular nao tiver o sensor, erro
        if (sensor == null) {
            leitura = leitura.copy(sensorDisponivel = false)
        }

        setContent {
            AcelerometroTheme(dynamicColor = false) {
                TelaAcelerometro(leitura = leitura)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Criada apenas para passar como parametro
    }

    override fun onSensorChanged(evento: SensorEvent?) {
        if (evento?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = evento.values[0]
        val y = evento.values[1]
        val z = evento.values[2]

        gravidade[0] = FILTRO_ALPHA * gravidade[0] + (1 - FILTRO_ALPHA) * x
        gravidade[1] = FILTRO_ALPHA * gravidade[1] + (1 - FILTRO_ALPHA) * y
        gravidade[2] = FILTRO_ALPHA * gravidade[2] + (1 - FILTRO_ALPHA) * z

        val linearX = x - gravidade[0]
        val linearY = y - gravidade[1]
        val linearZ = z - gravidade[2]

        leitura = LeituraAcelerometro(
            eixoX = x,
            eixoY = y,
            eixoZ = z,
            gravidade = magnitude(gravidade[0], gravidade[1], gravidade[2]),
            aceleracaoLinear = magnitude(linearX, linearY, linearZ),
            sensorDisponivel = true
        )
    }

    // calcula o tamanho do vetor (x, y, z)
    private fun magnitude(x: Float, y: Float, z: Float): Float =
        sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

//tela em Compose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelaAcelerometro(leitura: LeituraAcelerometro) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(text = "Acelerômetro", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Leitura em tempo real",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingInterno ->

        if (!leitura.sensorDisponivel) {
            MensagemSensorIndisponivel(modifier = Modifier.padding(paddingInterno))
            return@Scaffold
        }

        // todos os valores num cartão só, um embaixo do outro
        ElevatedCard(
            modifier = Modifier
                .padding(paddingInterno)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinhaDeValor(
                    titulo = "Aceleração linear",
                    valor = leitura.aceleracaoLinear,
                    cor = corDoMovimento(leitura.aceleracaoLinear)
                )
                LinhaDeValor(titulo = "Gravidade", valor = leitura.gravidade, cor = RoxoGravidade)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LinhaDeValor(titulo = "Eixo X", valor = leitura.eixoX, cor = RoxoEixoX)
                LinhaDeValor(titulo = "Eixo Y", valor = leitura.eixoY, cor = RoxoEixoY)
                LinhaDeValor(titulo = "Eixo Z", valor = leitura.eixoZ, cor = RoxoEixoZ)
            }
        }
    }
}

//Plus: Apenas para a questão de UI
private fun corDoMovimento(magnitude: Float): Color = when {
    magnitude < LIMITE_MOVIMENTO_LEVE -> RoxoRepouso
    magnitude < LIMITE_MOVIMENTO_INTENSO -> RoxoLeve
    else -> RoxoIntenso
}

// título de um lado, valor do outro
@Composable
private fun LinhaDeValor(
    titulo: String,
    valor: Float,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format(Locale.getDefault(), "%.2f", valor),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "m/s²",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

// aparece quando o aparelho nao tem acelerômetro
@Composable
private fun MensagemSensorIndisponivel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Este dispositivo não possui sensor de acelerômetro.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
