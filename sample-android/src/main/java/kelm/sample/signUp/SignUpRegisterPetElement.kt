package kelm.sample.signUp

import kelm.ExternalError
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.signUp.SignUpRegisterPetElement.Cmd
import kelm.sample.signUp.SignUpRegisterPetElement.Model
import kelm.sample.signUp.SignUpRegisterPetElement.Msg

object SignUpRegisterPetElement : Kelm.Element<Model, Msg, Cmd, Nothing>() {
    data class Model(
        val userId: String,
        val petName: String,
        val petId: String?,
        val showErrorMessage: Boolean
    ) {
        val showContinueButton = petId != null
    }


    sealed class Msg {
        object Continue : Msg()

        data class PetRegisterSuccessResponse(val petId: String) : Msg()
        data class PetRegisterError(val error: Throwable) : Msg()

        object Retry : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        data class RegisterPet(
            val userId: String,
            val petName: String
        ) : Cmd()

        data class FinishSuccess(val petId: String) : Cmd()
    }

    fun initModel(userId: String, petName: String) =
        Model(
            userId = userId,
            petName = petName,
            petId = null,
            showErrorMessage = false
        )

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        with(model) {
            when (msg) {
                is Msg.PetRegisterSuccessResponse ->
                    model.copy(petId = msg.petId, showErrorMessage = false)
                is Msg.PetRegisterError -> copy(showErrorMessage = true)
                is Msg.Retry -> {
                    +Cmd.RegisterPet(
                        userId = model.userId,
                        petName = model.petName
                    )
                    model.copy(showErrorMessage = false)
                }
                is Msg.Continue -> {
                    if (model.petId != null) {
                        +Cmd.FinishSuccess(model.petId)
                    }
                    null
                }
            }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}
