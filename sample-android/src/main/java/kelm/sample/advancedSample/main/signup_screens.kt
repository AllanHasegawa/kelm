package kelm.sample.advancedSample.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kelm.sample.advancedSample.SignUpElement

@Composable
fun AppMainScreen(model: SignUpElement.Model.AppMainScreen) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Welcome to the main app ${model.userId}!")
    }
}

@Composable
fun PetScreen(model: SignUpElement.Model.PetScreen) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "This is the pet screen ${model.userId}! Hi ${model.petId}!")
    }
}
