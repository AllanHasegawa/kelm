package kelm.sample.signUp

import kelm.ExternalError
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.signUp.SignUpRegisterDeviceElement.Cmd
import kelm.sample.signUp.SignUpRegisterDeviceElement.Model
import kelm.sample.signUp.SignUpRegisterDeviceElement.Msg

object SignUpRegisterDeviceElement : Kelm.Element<Model, Msg, Cmd, Nothing>() {
    data class Model(
        val userId: String,
        val showErrorMessage: Boolean,
        val retryButtonLoading: Boolean
    )

    sealed class Msg {
        object DeviceRegisterSuccessResponse : Msg()
        data class DeviceRegisterError(val error: Throwable) : Msg()
        object Retry : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        data class RegisterDevice(val userId: String) : Cmd()
        object FinishSuccess : Cmd()
    }

    fun initModel(userId: String): Model =
        Model(
            userId = userId,
            showErrorMessage = false,
            retryButtonLoading = false
        )

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        with(model) {
            fun goToNextScreen(): Model? {
                +Cmd.FinishSuccess
                return null
            }

            when (msg) {
                is Msg.DeviceRegisterSuccessResponse -> goToNextScreen()
                is Msg.DeviceRegisterError ->
                    copy(
                        showErrorMessage = true,
                        retryButtonLoading = false
                    )
                is Msg.Retry ->
                    copy(
                        showErrorMessage = false,
                        retryButtonLoading = true
                    ).also { +Cmd.RegisterDevice(userId = userId) }
            }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
