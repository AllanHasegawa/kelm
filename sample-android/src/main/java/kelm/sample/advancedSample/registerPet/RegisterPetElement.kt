package kelm.sample.advancedSample.registerPet

import kelm.Element
import kelm.buildModelCmds
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Cmd
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Model
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Msg

object RegisterPetElement : Element<Model, Msg, Cmd, Nothing>() {
    data class Model(
        val userId: String,
        val petName: String,
        val petId: String?,
        val showErrorMessage: Boolean
    ) {
        val showContinueButton = petId != null || showErrorMessage
    }


    sealed class Msg {
        object ContinueClicked : Msg()

        data class SuccessResponse(val petId: String) : Msg()
        data class PetRegisterError(val error: Throwable) : Msg()
    }

    sealed class Cmd {
        data class RegisterPet(
            val userId: String,
            val petName: String,
        ) : Cmd()

        data class Finish(val userId: String, val petId: String?) : Cmd()
    }

    fun initModel(
        userId: String,
        petName: String
    ): Model =
        Model(
            userId = userId,
            petName = petName,
            petId = null,
            showErrorMessage = false,
        )

    override fun update(model: Model, msg: Msg) = buildModelCmds {
        with(model) {
            when (msg) {
                is Msg.SuccessResponse ->
                    copy(petId = msg.petId, showErrorMessage = false)
                is Msg.PetRegisterError -> copy(showErrorMessage = true)
                is Msg.ContinueClicked -> {
                    +Cmd.Finish(userId = userId, petId = petId)
                    null
                }
            }
        }
    }

    override fun initCmds(initModel: Model): List<Cmd> = listOf(
        Cmd.RegisterPet(
            userId = initModel.userId,
            petName = initModel.petName,
        )
    )
}
