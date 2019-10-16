package kelm.sample.signUp

import kelm.ExternalError
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.isEmailValid
import kelm.sample.signUp.SignUpFormElement.Cmd
import kelm.sample.signUp.SignUpFormElement.Model
import kelm.sample.signUp.SignUpFormElement.Msg

object SignUpFormElement : Kelm.Element<Model, Msg, Cmd, Nothing>() {
    enum class EmailError {
        Required,
        Validation
    }

    enum class PasswordError {
        Required,
        TooSimple
    }

    data class Model(
        val email: String,
        val emailError: EmailError?,
        val password: String,
        val passwordError: PasswordError?,
        val showPetNameInput: Boolean,
        val petName: String?,
        val petNameRequiredError: Boolean,
        val inputEnabled: Boolean,
        val buttonLoading: Boolean,
        val requestIdempotencyKey: String,
        val showConnErrorCTA: Boolean
    )

    sealed class Msg {
        data class EmailChanged(val email: String) : Msg()
        data class PasswordChanged(val password: String) : Msg()
        data class PetNameChanged(val petName: String) : Msg()
        object RegisterPetClicked : Msg()

        object Continue : Msg()

        data class UserSignUpSuccessResponse(val userId: String) : Msg()
        data class UserSignUpError(val error: Throwable) : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        data class SubmitForm(
            val idempotencyKey: String,
            val email: String,
            val password: String
        ) : Cmd()

        data class FinishSuccess(val userId: String, val petName: String?) : Cmd()
    }

    fun initModel(idempotencyKey: String): Model =
        Model(
            email = "",
            emailError = null,
            password = "",
            passwordError = null,
            showPetNameInput = false,
            petName = null,
            petNameRequiredError = false,
            inputEnabled = true,
            buttonLoading = false,
            requestIdempotencyKey = idempotencyKey,
            showConnErrorCTA = false
        )

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        with(model) {
            when (msg) {
                is Msg.EmailChanged -> copy(email = msg.email, emailError = null)
                is Msg.PasswordChanged -> copy(password = msg.password, passwordError = null)
                is Msg.PetNameChanged -> copy(petName = msg.petName)
                is Msg.RegisterPetClicked -> copy(showPetNameInput = !showPetNameInput)
                is Msg.Continue -> {
                    var emailError: EmailError? = null
                    var passwordError: PasswordError? = null
                    var petNameRequiredError = false
                    if (!isEmailValid(email)) {
                        emailError = EmailError.Validation
                    }
                    if (email.isBlank()) {
                        emailError = EmailError.Required
                    }
                    if (password.length < 6) {
                        passwordError = PasswordError.TooSimple
                    }
                    if (password.isEmpty()) {
                        passwordError = PasswordError.Required
                    }
                    if (showPetNameInput && petName.isNullOrBlank()) {
                        petNameRequiredError = true
                    }
                    if (emailError != null || passwordError != null || petNameRequiredError) {
                        return copy(
                            emailError = emailError,
                            passwordError = passwordError,
                            petNameRequiredError = petNameRequiredError
                        )
                    }

                    copy(buttonLoading = true, inputEnabled = false, showConnErrorCTA = false)
                        .also {
                            +Cmd.SubmitForm(
                                idempotencyKey = requestIdempotencyKey,
                                email = email,
                                password = password
                            )
                        }
                }
                is Msg.UserSignUpSuccessResponse -> {
                    +Cmd.FinishSuccess(userId = msg.userId, petName = model.petName)
                    null
                }
                is Msg.UserSignUpError ->
                    copy(buttonLoading = false, showConnErrorCTA = true, inputEnabled = false)
            }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
