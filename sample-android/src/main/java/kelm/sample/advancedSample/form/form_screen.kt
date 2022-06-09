package kelm.sample.advancedSample.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kelm.sample.advancedSample.form.FormElement.Model
import kelm.sample.advancedSample.form.FormElement.Msg

@Composable
fun FormScreen(model: Model, onMsg: (Msg) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "\uD83D\uDC36", fontSize = 58.sp)

        TextFieldWithError(
            value = model.email,
            placeholder = "Email",
            enabled = model.inputEnabled,
            isPassword = false,
            error = when (model.emailError) {
                EmailError.Required -> "Email is required."
                EmailError.Validation -> "Email is invalid."
                null -> null
            },
            onChange = { onMsg(Msg.EmailChanged(it)) }
        )

        TextFieldWithError(
            value = model.password,
            placeholder = "Password",
            enabled = model.inputEnabled,
            isPassword = true,
            error = when (model.passwordError) {
                PasswordError.Required -> "Password is required."
                PasswordError.TooSimple -> "Password is too simple."
                null -> null
            },
            onChange = { onMsg(Msg.PasswordChanged(it)) }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = model.showPetNameInput,
                onCheckedChange = { onMsg(Msg.RegisterPetClicked) }
            )

            Text(text = "Register pet")
        }

        if (model.showPetNameInput) {
            TextFieldWithError(
                placeholder = "Pet name",
                value = model.petName ?: "",
                enabled = model.inputEnabled,
                isPassword = false,
                error = if (model.petNameRequiredError) "Pet name is required." else null,
                onChange = { onMsg(Msg.PetNameChanged(it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (model.showConnErrorCTA) {
            Text(text = "Connection Error. Try Again.")
        }

        if (model.loading) {
            CircularProgressIndicator(Modifier.width(16.dp))
        } else {
            Button(
                enabled = model.inputEnabled,
                onClick = { onMsg(Msg.ContinueClicked) }
            ) {
                Text(text = "Register")
            }
        }
    }
}

@Composable
fun TextFieldWithError(
    placeholder: String,
    value: String,
    enabled: Boolean,
    error: String?,
    isPassword: Boolean,
    onChange: (String) -> Unit,
) {
    val visualTransformation = if (!isPassword) VisualTransformation.None
    else VisualTransformation {
        TransformedText(
            AnnotatedString(it.map { '*' }.joinToString("")),
            OffsetMapping.Identity
        )
    }
    TextField(
        value = value,
        placeholder = { Text(placeholder) },
        enabled = enabled,
        isError = error != null,
        visualTransformation = visualTransformation,
        label = { error?.let { Text(text = it) } },
        onValueChange = { onChange(it) }
    )
}

class FormScreenPreviewProvider : PreviewParameterProvider<Model> {
    override val values: Sequence<Model> = sequenceOf(
        Model(
            email = "",
            emailError = null,
            password = "",
            passwordError = null,
            showPetNameInput = false,
            petName = null,
            petNameRequiredError = false,
            loading = false,
            requestIdempotencyKey = "id-key",
            showConnErrorCTA = false,
        ),
        Model(
            email = "hello@gmail.com",
            emailError = null,
            password = "password123",
            passwordError = null,
            showPetNameInput = true,
            petName = null,
            petNameRequiredError = false,
            loading = true,
            requestIdempotencyKey = "id-key",
            showConnErrorCTA = false,
        ),
        Model(
            email = "hello@gmail.com",
            emailError = null,
            password = "password123",
            passwordError = null,
            showPetNameInput = true,
            petName = "Fluffy",
            petNameRequiredError = false,
            loading = false,
            requestIdempotencyKey = "id-key",
            showConnErrorCTA = false,
        ),
        Model(
            email = "hello@gmail.com",
            emailError = EmailError.Validation,
            password = "password123",
            passwordError = PasswordError.Required,
            showPetNameInput = true,
            petName = "Fluffy",
            petNameRequiredError = true,
            loading = false,
            requestIdempotencyKey = "id-key",
            showConnErrorCTA = true,
        ),
    )
}

@Preview
@Composable
private fun FormScreenPreview(
    @PreviewParameter(FormScreenPreviewProvider::class) model: Model
) {
    FormScreen(
        model = model,
        onMsg = {}
    )
}
