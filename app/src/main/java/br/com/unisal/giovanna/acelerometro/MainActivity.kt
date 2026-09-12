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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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


// Constantes de configuração

private const val FILTRO_ALPHA = 0.8f

private const val LIMITE_MOVIMENTO_LEVE = 0.5f
private const val LIMITE_MOVIMENTO_INTENSO = 3f

// Paleta em tons de roxo
private val RoxoEixoX = Color(0xFF7C4DFF)     
private val RoxoEixoY = Color(0xFF9575CD)     
private val RoxoEixoZ = Color(0xFFB39DDB)     
private val RoxoGravidade = Color(0xFF512DA8) 
private val RoxoRepouso = Color(0xFFB39DDB)   
private val RoxoLeve = Color(0xFF7E57C2)      
private val RoxoIntenso = Color(0xFF4527A0)   

//Guarda a última leitura do acelerômetro para ser exibida na tela.
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

    // Vetor de gravidade 
    private val gravidade = FloatArray(3)

    // Estado observado pela UI: sempre que ele muda, a tela é redesenhada
    private var leitura by mutableStateOf(LeituraAcelerometro())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Se o aparelho não tiver o sensor, mostra erro
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
        // Não precisamos reagir a mudanças de precisão do sensor.
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

    // Calcula o módulo (tamanho) de um vetor 3D: raiz(x² + y² + z²).
    private fun magnitude(x: Float, y: Float, z: Float): Float =
        sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

// Interface gráfica (Jetpack Compose)

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

        Column(
            modifier = Modifier
                .padding(paddingInterno)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ---- Maior destaque: os dois valores calculados a partir dos eixos ----
            // Um único cartão largo, com as duas linhas empilhadas e apenas
            // uma linha divisória entre elas (em vez de dois cartões lado a lado).
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LinhaDeMagnitude(
                        titulo = "Aceleração linear",
                        valor = leitura.aceleracaoLinear,
                        cor = corDoMovimento(leitura.aceleracaoLinear)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                    LinhaDeMagnitude(
                        titulo = "Gravidade",
                        valor = leitura.gravidade,
                        cor = RoxoGravidade
                    )
                }
            }

            // valor bruto de cada eixo 
            Text(
                text = "Eixos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoDeEixo(modifier = Modifier.weight(1f), nome = "X", valor = leitura.eixoX, cor = RoxoEixoX)
                CartaoDeEixo(modifier = Modifier.weight(1f), nome = "Y", valor = leitura.eixoY, cor = RoxoEixoY)
                CartaoDeEixo(modifier = Modifier.weight(1f), nome = "Z", valor = leitura.eixoZ, cor = RoxoEixoZ)
            }

            Text(
                text = "Valores em m/s²",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun corDoMovimento(magnitude: Float): Color = when {
    magnitude < LIMITE_MOVIMENTO_LEVE -> RoxoRepouso
    magnitude < LIMITE_MOVIMENTO_INTENSO -> RoxoLeve
    else -> RoxoIntenso
}

/**
 * Uma linha larga e horizontal usada dentro do cartão de destaque:
 * título de um lado e o valor numérico do outro.
 */
@Composable
private fun LinhaDeMagnitude(
    titulo: String,
    valor: Float,
    cor: Color,
    modifier: Modifier = Modifier
) {
    // A cor anima suavemente entre leituras: um feedback visual simples de
    // que o valor foi atualizado, sem precisar de barras ou gráficos.
    val corAnimada by animateColorAsState(targetValue = cor, label = "cor_$titulo")

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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = corAnimada
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "m/s²",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

//Cartão simples usado para cada eixo (X, Y, Z): nome do eixo, valor numérico

@Composable
private fun CartaoDeEixo(
    nome: String,
    valor: Float,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bolinha colorida ajuda a identificar cada eixo
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = cor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Eixo $nome",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format(Locale.getDefault(), "%.2f", valor),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "m/s²",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//Mensagem exibida quando o aparelho não possui sensor de acelerômetro
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
