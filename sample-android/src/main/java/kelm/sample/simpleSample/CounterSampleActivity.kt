package kelm.sample.simpleSample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kelm.sample.simpleSample.CounterElement.Model
import kelm.sample.simpleSample.CounterElement.Msg
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest

class CounterSampleActivity : AppCompatActivity() {
    private val msgChannel = Channel<Msg>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val state = mutableStateOf(CounterElement.initModel())
        setContent {
            CounterScreen(
                model = state.value,
                onMsg = msgChannel::trySend
            )
        }

        lifecycleScope.launchWhenCreated {
            CounterElement
                .buildModelFlow(
                    initModel = CounterElement.initModel(),
                    msgInput = msgChannel,
                )
                .collectLatest { model -> state.value = model }
        }
    }
}

@Composable
private fun CounterScreen(
    model: Model,
    onMsg: (Msg) -> Unit,
) {
    fun Msg.ret(): () -> Unit = { onMsg(this) }
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = model.count.toString(),
                fontSize = 64.sp,
            )

            Row(modifier = Modifier.padding(all = 8.dp)) {
                Button(
                    onClick = Msg.MinusClick.ret(),
                    enabled = model.minusBtEnabled,
                    modifier = Modifier.padding(all = 8.dp)
                ) {
                    Text(text = "-")
                }
                Button(onClick = Msg.PlusClick.ret(), modifier = Modifier.padding(all = 8.dp)) {
                    Text(text = "+")
                }
            }

            Button(onClick = Msg.ResetClick.ret(), enabled = model.resetBtEnabled) {
                Text(text = "RESET")
            }
        }
    }
}

@Composable
@Preview
private fun CounterScreenPreview() {
    CounterScreen(
        model = CounterElement.initModel(),
        onMsg = {}
    )
}
