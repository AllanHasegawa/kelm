package kelm.sample.signUp

import kelm.ExternalError
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.signUp.SignUpElement.Cmd
import kelm.sample.signUp.SignUpElement.Model
import kelm.sample.signUp.SignUpElement.Msg

object SignUpElement : Kelm.Element<Model, Msg, Cmd, Nothing>() {

    sealed class Model {
        data class FormVisible(
            val model: SignUpFormElement.Model
        ) : Model()

        data class RegisterDevice(
            val model: SignUpRegisterDeviceElement.Model
        ) : Model()

        data class RegisterPet(
            val model: SignUpRegisterPetElement.Model
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
    }

    fun initModel(idempotencyKey: String): Model =
        SignUpFormElement.initModel(idempotencyKey)
            .let(Model::FormVisible)

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? {
    }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
