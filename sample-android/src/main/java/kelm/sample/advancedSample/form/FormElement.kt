package kelm.sample.advancedSample.form

import kelm.Element
import kelm.ExternalException
import kelm.buildModelCmds
import kelm.sample.isEmailValid
import kelm.sample.advancedSample.form.FormElement.Cmd
import kelm.sample.advancedSample.form.FormElement.Model
import kelm.sample.advancedSample.form.FormElement.Msg

object FormElement : Element<Model, Msg, Cmd, Nothing>() {
    data class Model(
        val email: String,
        val emailError: EmailError?,
        val password: String,
        val passwordError: PasswordError?,
        val showPetNameInput: Boolean,
        val petName: String?,
        val petNameRequiredError: Boolean,
        val loading: Boolean,
        val requestIdempotencyKey: String,
        val showConnErrorCTA: Boolean,
    ) {
        val inputEnabled = !loading
    }

    fun initModel(idempotencyKey: String): Model =
        Model(
            email = "epic.email@example.com",
            emailError = null,
            password = "hunter42",
            passwordError = null,
            showPetNameInput = false,
            petName = null,
            petNameRequiredError = false,
            loading = false,
            requestIdempotencyKey = idempotencyKey,
            showConnErrorCTA = false,
        )

    sealed class Msg {
        data class EmailChanged(val email: String) : Msg()
        data class PasswordChanged(val password: String) : Msg()
        data class PetNameChanged(val petName: String) : Msg()
        object RegisterPetClicked : Msg()

        object ContinueClicked : Msg()

        data class SuccessResponse(val userId: String) : Msg()
        data class UserSignUpError(val error: Throwable) : Msg()
    }

    sealed class Cmd {
        data class SubmitForm(
            val idempotencyKey: String,
            val email: String,
            val password: String
        ) : Cmd()

        data class Finish(val userId: String, val petName: String?) : Cmd()
    }

    override fun update(
        model: Model,
        msg: Msg,
    ) = buildModelCmds {
        with(model) {
            when (msg) {
                is Msg.EmailChanged -> copy(email = msg.email, emailError = null)
                is Msg.PasswordChanged -> copy(password = msg.password, passwordError = null)
                is Msg.PetNameChanged -> copy(petName = msg.petName, petNameRequiredError = false)
                is Msg.RegisterPetClicked -> copy(showPetNameInput = !showPetNameInput)
                is Msg.ContinueClicked -> {
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
                        return@buildModelCmds copy(
                            emailError = emailError,
                            passwordError = passwordError,
                            petNameRequiredError = petNameRequiredError
                        )
                    }

                    copy(loading = true, showConnErrorCTA = false)
                        .also {
                            +Cmd.SubmitForm(
                                idempotencyKey = requestIdempotencyKey,
                                email = email,
                                password = password
                            )
                        }
                }
                is Msg.SuccessResponse -> {
                    +Cmd.Finish(
                        userId = msg.userId,
                        petName = model.petName,
                    )
                    null
                }
                is Msg.UserSignUpError -> copy(loading = false, showConnErrorCTA = true)
            }
        }
    }

    override fun exceptionToMsg(exception: ExternalException): Msg? {
        throw exception
    }
}
