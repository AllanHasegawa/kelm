package kelm.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kelm.sample.commandSample.FoxServiceSampleActivity
import kelm.sample.advancedSample.SignUpFormSampleActivity
import kelm.sample.simpleSample.CounterSampleActivity
import kelm.sample.subscriptionSample.ClockSampleActivity
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buttons = listOf(
            MainButtonData("Counter (Sandbox sample)", CounterSampleActivity::class),
            MainButtonData("Fox service (CMD sample)", FoxServiceSampleActivity::class),
            MainButtonData("Clock (Sub sample)", ClockSampleActivity::class),
            MainButtonData("SignUp form (Advanced sample)", SignUpFormSampleActivity::class),
        )

        fun openSampleActivity(sampleClass: Class<*>) {
            Intent(this@MainActivity, sampleClass)
                .let(this@MainActivity::startActivity)
        }

        setContent {
            MainScreen(buttons = buttons, onClick = { kClass -> openSampleActivity(kClass.java) })
        }
    }
}

private data class MainButtonData(val text: String, val kClass: KClass<*>)

@Composable
private fun MainButton(data: MainButtonData, onClick: (KClass<*>) -> Unit) {
    Button(
        onClick = { onClick(data.kClass) },
        modifier = Modifier.padding(8.dp)
    ) {
        Text(text = data.text)
    }
}

@Composable
private fun MainScreen(buttons: List<MainButtonData>, onClick: (KClass<*>) -> Unit) {
    MaterialTheme {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            buttons.forEach { data -> MainButton(data, onClick) }
        }
    }
}
