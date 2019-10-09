package kelm.sample.signUp

object SignUpElement {

    sealed class Model {

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
        object Retry : Msg()

        data class UserSignUpSuccessResponse(val userId: String) : Msg()
        data class UserSignUpError(val error: Throwable) : Msg()

        object DeviceRegisterSuccessResponse : Msg()
        data class DeviceRegisterError(val error: Throwable) : Msg()

        data class PetRegisterSuccessResponse(val petId: String) : Msg()
        data class PetRegisterError(val error: Throwable) : Msg()
    }

    sealed class Cmd : kelm.Cmd() {

        data class RegisterPet(
            val userId: String,
            val petName: String
        ) : Cmd()
    }
}
