package kelm.sample.advancedSample.registerPet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterPetScreen(model: RegisterPetElement.Model, onMsg: (RegisterPetElement.Msg) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "\uD83D\uDC31\u200D\uD83D\uDCBB",
            fontSize = 58.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your account is ready",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (model.showErrorMessage) {
                true -> "Error while registering your pet, please try again later."
                false -> "Registering ${model.petName} <3"
            },
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            enabled = model.showContinueButton,
            onClick = { onMsg(RegisterPetElement.Msg.ContinueClicked) }
        ) {
            Text(text = "Continue")
        }
    }
}

class RegPetModelPreviewProvider : PreviewParameterProvider<RegisterPetElement.Model> {
    override val values: Sequence<RegisterPetElement.Model> = sequenceOf(
        RegisterPetElement.Model("", "Fluffly", null, false),
        RegisterPetElement.Model("", "Rex", "id", false),
        RegisterPetElement.Model("", "Peanut-Butter Biscuit Sandwich", null, true),
        RegisterPetElement.Model("", "Homer", "id", true),
    )
}

@Composable
@Preview
private fun RegisterPetPreview(
    @PreviewParameter(RegPetModelPreviewProvider::class) model: RegisterPetElement.Model,
) {
    RegisterPetScreen(model = model, onMsg = {})
}
