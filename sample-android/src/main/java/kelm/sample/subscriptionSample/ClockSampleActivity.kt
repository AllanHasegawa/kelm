package kelm.sample.subscriptionSample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kelm.Log
import kelm.sample.subscriptionSample.ClockElement.Model
import kelm.sample.subscriptionSample.ClockElement.Msg
import kelm.sample.subscriptionSample.ClockElement.Sub
import kelm.sample.toast
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SetTextI18n")
class ClockSampleActivity : AppCompatActivity() {
    private val msgChannel: SendChannel<Msg> = Channel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val state = mutableStateOf<Model>(Model.Paused)
        setContent {
            ClockScreen(model = state.value, onMsg = { msgChannel.trySend(it) })
        }

        lifecycleScope.launchWhenCreated {
            ClockElement
                .buildModelFlow(
                    initModel = ClockElement.initModel(),
                    msgInput = msgChannel,
                    cmdExecutor = { null },
                    subToFlow = { sub, _, _ ->
                        // Converts a Sub to a Flow<Msg>
                        when (sub) {
                            is Sub.ClockTickSub -> clockTickProducer()
                        }
                    },
                    logger = ::logger
                )
                .collectLatest { model -> state.value = model }
        }
    }

    private fun clockTickProducer(): Flow<Msg> = flow {
        coroutineScope {
            while (true) {
                delay(16.milliseconds)
                emit(Msg.Tick(System.currentTimeMillis()))
            }
        }
    }

    private fun logger(log: Log<Model, Msg, Nothing, Sub>) {
        when (log) {
            is Log.Update -> println("---\nModel: ${log.modelPrime}\nSubs: ${log.subs}\n---")
            is Log.SubscriptionStarted -> toast("[${log.sub.id}] started")
            is Log.SubscriptionStopped -> toast("[${log.sub.id}] cancelled")
            is Log.SubscriptionEmission -> println("[${log.sub.id}] emitted: ${log.msg}")
            else -> Unit
        }
    }
}

@Composable
private fun ClockScreen(model: Model, onMsg: (Msg) -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss:SSS z", Locale.getDefault()) }
    fun Long.format() = dateFormatter.format(Date(this))

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        val mainText = (model as? Model.Running)?.instant?.format()?.toString() ?: "---"
        Text(text = mainText, fontSize = 32.sp)

        Spacer(modifier = Modifier.height(24.dp))

        val buttonText = when (model) {
            is Model.Paused -> "Start"
            is Model.Running -> "Pause"
        }
        Button(onClick = { onMsg(Msg.ToggleRunPauseClock) }) {
            Text(text = buttonText)
        }
    }
}
