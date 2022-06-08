package kelm.sample.advancedSample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import kelm.sample.advancedSample.SignUpElement.Model
import kelm.sample.advancedSample.SignUpElement.Msg
import kelm.sample.advancedSample.form.FormScreen
import kelm.sample.advancedSample.main.AppMainScreen
import kelm.sample.advancedSample.main.PetScreen
import kelm.sample.advancedSample.registerPet.RegisterPetScreen

class SignUpFormSampleActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this)[SignUpViewModel::class.java]
    }

    private val state = mutableStateOf<Model?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SignUpMainScreen(model = state.value, onMsg = viewModel::onMsg)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.model.observe(this) { model -> state.value = model }
    }
}

@Composable
fun SignUpMainScreen(model: Model?, onMsg: (Msg) -> Unit) {
    when (model) {
        is Model.FormStep -> FormScreen(model = model.formModel, onMsg = { onMsg(Msg.Form(it)) })
        is Model.RegisterPetStep -> RegisterPetScreen(
            model = model.regPetModel,
            onMsg = { onMsg(Msg.RegisterPet(it)) })
        is Model.PetScreen -> PetScreen(model = model)
        is Model.AppMainScreen -> AppMainScreen(model = model)
        null -> Unit
    }
}
