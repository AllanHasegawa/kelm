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
            val formModel: SignUpFormElement.Model
        ) : Model()

        data class RegisterDevice(
            val petName: String?,
            val regDeviceModel: SignUpRegisterDeviceElement.Model
        ) : Model()

        data class RegisterPet(
            val regPetModel: SignUpRegisterPetElement.Model
        ) : Model()

        data class AppMainScreen(val userId: String) : Model()
        data class PetScreen(val userId: String, val petId: String) : Model()
    }

    sealed class Msg {
        data class FromForm(val it: SignUpFormElement.Msg) : Msg()
        data class FormFinishSuccess(val userId: String, val petName: String?) : Msg()

        data class FromRegDevice(val it: SignUpRegisterDeviceElement.Msg) : Msg()
        object RegDeviceFinishSuccess : Msg()

        data class FromRegPet(val it: SignUpRegisterPetElement.Msg) : Msg()
        data class RegPetFinishSuccess(val petId: String) : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        data class FromForm(val it: SignUpFormElement.Cmd) : Cmd()
        data class FromRegDevice(val it: SignUpRegisterDeviceElement.Cmd) : Cmd()
        data class FromPetDevice(val it: SignUpRegisterPetElement.Cmd) : Cmd()
    }

    fun initModel(idempotencyKey: String): Model =
        SignUpFormElement.initModel(idempotencyKey)
            .let(Model::FormVisible)

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        when (model) {
            is Model.FormVisible ->
                when (msg) {
                    is Msg.FromForm ->
                        switchContext(
                            otherElement = SignUpFormElement,
                            otherModel = model.formModel,
                            otherMsg = msg.it,
                            otherCmdToCmd = Cmd::FromForm,
                            otherSubToSub = { it },
                            bypassOtherCmdToMsg = { cmd ->
                                when (cmd) {
                                    is SignUpFormElement.Cmd.FinishSuccess ->
                                        Msg.FormFinishSuccess(cmd.userId, cmd.petName)
                                    else -> null
                                }
                            }
                        )?.let(Model::FormVisible)

                    is Msg.FormFinishSuccess ->
                        SignUpRegisterDeviceElement.initModel(userId = msg.userId)
                            .let {
                                Model.RegisterDevice(
                                    petName = msg.petName,
                                    regDeviceModel =
                                    SignUpRegisterDeviceElement.initModel(userId = msg.userId)
                                )
                            }

                    else -> null
                }
            is Model.RegisterDevice ->
                when (msg) {
                    is Msg.FromRegDevice ->
                        switchContext(
                            otherElement = SignUpRegisterDeviceElement,
                            otherModel = model.regDeviceModel,
                            otherMsg = msg.it,
                            otherCmdToCmd = Cmd::FromRegDevice,
                            otherSubToSub = { it },
                            bypassOtherCmdToMsg = { cmd ->
                                when (cmd) {
                                    is SignUpRegisterDeviceElement.Cmd.FinishSuccess ->
                                        Msg.RegDeviceFinishSuccess
                                    else -> null
                                }
                            }
                        )?.let { model.copy(regDeviceModel = it) }

                    is Msg.RegDeviceFinishSuccess ->
                        when (model.petName) {
                            null -> Model.AppMainScreen(userId = model.regDeviceModel.userId)
                            else -> Model.RegisterPet(
                                SignUpRegisterPetElement.initModel(
                                    userId = model.regDeviceModel.userId,
                                    petName = model.petName
                                )
                            )
                        }
                    else -> null
                }

            is Model.RegisterPet ->
                when (msg) {
                    is Msg.FromRegPet ->
                        switchContext(
                            otherElement = SignUpRegisterPetElement,
                            otherModel = model.regPetModel,
                            otherMsg = msg.it,
                            otherCmdToCmd = Cmd::FromPetDevice,
                            otherSubToSub = { it },
                            bypassOtherCmdToMsg = { cmd ->
                                when (cmd) {
                                    is SignUpRegisterPetElement.Cmd.FinishSuccess ->
                                        Msg.RegPetFinishSuccess(cmd.petId)
                                    else -> null
                                }
                            }
                        )?.let { model.copy(regPetModel = it) }

                    is Msg.RegPetFinishSuccess ->
                        Model.PetScreen(
                            userId = model.regPetModel.userId,
                            petId = msg.petId
                        )
                    else -> null
                }

            is Model.AppMainScreen,
            is Model.PetScreen -> null
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
