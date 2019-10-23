package kelm.sample.signUp

import kelm.ExternalError
import kelm.Kelm
import kelm.MsgOrCmd
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.signUp.SignUpElement.Cmd
import kelm.sample.signUp.SignUpElement.Model
import kelm.sample.signUp.SignUpElement.Msg
import kelm.sample.signUp.form.SignUpFormElement
import kelm.sample.signUp.registerDevice.SignUpRegisterDeviceElement
import kelm.sample.signUp.registerPet.SignUpRegisterPetElement

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
        object RegPetFinishError : Msg()
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
                            otherCmdToMsgOrCmd = { otherCmd ->
                                when (otherCmd) {
                                    is SignUpFormElement.Cmd.FinishSuccess ->
                                        Msg.FormFinishSuccess(otherCmd.userId, otherCmd.petName).ret()
                                    else -> Cmd.FromForm(otherCmd).ret()
                                }
                            },
                            otherSubToSub = { it }
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
                            .also {
                                +SignUpRegisterDeviceElement.initCmds(it.regDeviceModel)!!
                                    .map(Cmd::FromRegDevice)
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
                            otherCmdToMsgOrCmd = { otherCmd ->
                                when (otherCmd) {
                                    is SignUpRegisterDeviceElement.Cmd.FinishSuccess ->
                                        Msg.RegDeviceFinishSuccess.ret()
                                    else -> Cmd.FromRegDevice(otherCmd).ret()
                                }
                            },
                            otherSubToSub = { it }
                        )?.let { model.copy(regDeviceModel = it) }

                    is Msg.RegDeviceFinishSuccess ->
                        when (model.petName) {
                            null -> Model.AppMainScreen(userId = model.regDeviceModel.userId)
                            else -> Model.RegisterPet(
                                SignUpRegisterPetElement.initModel(
                                    userId = model.regDeviceModel.userId,
                                    petName = model.petName
                                )
                            ).also {
                                +SignUpRegisterPetElement.initCmds(it.regPetModel)!!
                                    .map(Cmd::FromPetDevice)
                            }
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
                            otherCmdToMsgOrCmd = { otherCmd ->
                                when (otherCmd) {
                                    is SignUpRegisterPetElement.Cmd.FinishSuccess ->
                                        Msg.RegPetFinishSuccess(otherCmd.petId).ret()
                                    is SignUpRegisterPetElement.Cmd.FinishWithError ->
                                        Msg.RegPetFinishError.ret()
                                    else -> Cmd.FromPetDevice(otherCmd).ret()
                                }
                            },
                            otherSubToSub = { it }
                        )?.let { model.copy(regPetModel = it) }

                    is Msg.RegPetFinishSuccess ->
                        Model.PetScreen(
                            userId = model.regPetModel.userId,
                            petId = msg.petId
                        )

                    is Msg.RegPetFinishError ->
                        Model.AppMainScreen(userId = model.regPetModel.userId)

                    else -> null
                }

            is Model.AppMainScreen,
            is Model.PetScreen -> null
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
