package kelm.sample.commandSample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kelm.Log
import kelm.sample.commandSample.FoxServiceElement.Cmd
import kelm.sample.commandSample.FoxServiceElement.Model
import kelm.sample.commandSample.FoxServiceElement.Msg
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

class FoxServiceSampleActivity : AppCompatActivity() {
    private val service = FakeFoxPicsService()
    private val msgChannel = Channel<Msg>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val state = mutableStateOf<Model>(FoxServiceElement.initModel())
        setContent {
            FoxScreen(
                model = state.value,
                onMsg = { msgChannel.trySend(it) }
            )
        }

        lifecycleScope.launchWhenCreated {
            FoxServiceElement
                .buildModelFlow(
                    initModel = FoxServiceElement.initModel(),
                    msgInput = msgChannel,
                    cmdExecutor = ::cmdExecutor,
                    subToFlow = { _, _, _ -> flowOf() },
                    logger = ::logger,
                )
                .collectLatest { model -> state.value = model }
        }
    }

    /**
     * Maps a command to a message.
     */
    private suspend fun cmdExecutor(cmd: Cmd): Msg =
        when (cmd) {
            is Cmd.Fetch -> fetchCmd(cmd.delay)
        }

    /**
     * This is an example of a side-effect and asynchronous task where we do a network request.
     */
    private suspend fun fetchCmd(delayDuration: Duration): Msg {
        delay(delayDuration)

        return try {
            val imgUrl = service.fetchRandomFoxPicUrl()
            Msg.GotFoxPicUrl(url = imgUrl)
        } catch (t: Throwable) {
            Msg.ConnError
        }
    }

    private fun logger(log: Log<Model, Msg, Cmd, Nothing>) {
        println(log)
    }
}

@Composable
private fun FoxScreen(model: Model, onMsg: (Msg) -> Unit) {
    MaterialTheme {
        when (model) {
            is Model.Loading -> FoxLoadingScreen()
            is Model.ConnError -> FoxErrorScreen(onMsg)
            is Model.ContentLoaded -> FoxMainScreen(model, onMsg)
        }
    }
}

@Composable
private fun FoxMainScreen(
    value: Model.ContentLoaded,
    onMsg: (Msg) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.height(256.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = value.foxPicUrl.toString(),
                contentDescription = "Foxy :)",
                contentScale = ContentScale.FillHeight,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onMsg(Msg.Fetch) }) {
            Text(text = "Fetch new image")
        }
    }
}

@Composable
private fun FoxErrorScreen(onMsg: (Msg) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(128.dp))

        Text(
            text = "!",
            fontSize = 128.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Red,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Connection Error",
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(4.dp))

        Button(onClick = { onMsg(Msg.Fetch) }) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun FoxLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            Modifier
                .width(64.dp)
                .height(64.dp),
        )
    }
}
