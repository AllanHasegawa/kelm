package kelm.sample.signUpForm

object SignUpFormContract {
    enum class EmailError {
        Required,
        Validation
    }

    enum class PasswordError {
        Required,
        TooSimple
    }

    sealed class Model {
        data class FormVisible(
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
        ) : Model()

        data class RegisteringDevice(
            val userId: String,
            val petName: String?,
            val showErrorMessage: Boolean,
            val retryButtonLoading: Boolean
        ) : Model()

        data class RegisteringPet(
            val userId: String,
            val petName: String,
            val showErrorMessage: Boolean,
            val showContinueButton: Boolean
        ) : Model()

        data class AppMainScreen(val userId: String) : Model()
        data class PetScreen(val userId: String, val petId: String) : Model()
    }

    sealed class Msg {
        data class EmailChanged(val email: String) : Msg()
        data class PasswordChanged(val password: String) : Msg()
        data class PetNameChanged(val petName: String) : Msg()

        object RegisterPetClicked : Msg()

        object Continue : Msg()
        object Retry : Msg()

        data class UserSignUpSuccessResponse(val userId: String) : Msg()
        data class UserSignUpError(val error: Throwable) : Msg()

        object DeviceRegisterSuccessResponse : Msg()
        data class DeviceRegisterError(val error: Throwable) : Msg()

        data class PetRegisterSuccessResponse(val petId: String) : Msg()
        data class PetRegisterError(val error: Throwable) : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        data class SubmitForm(
            val idempotencyKey: String,
            val email: String,
            val password: String
        ) : Cmd()

        data class RegisterDevice(val userId: String) : Cmd()

        data class RegisterPet(
            val userId: String,
            val petName: String
        ) : Cmd()
    }
}
