package kelm.sample.signUp

import kelm.Kelm

object SignUpRegisteringDeviceElement : Kelm.Element<>() {
    data class Model(
        val userId: String,
        val petName: String?,
        val showErrorMessage: Boolean,
        val retryButtonLoading: Boolean
    )

    sealed class Msg {
        object DeviceRegisterSuccessResponse : Msg()
        data class DeviceRegisterError(val error: Throwable) : Msg()
    }

    sealed class Cmd {
        data class RegisterDevice(val userId: String) : Cmd()
        object FinishSuccess : Cmd()
    }
}
