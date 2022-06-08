package kelm.sample.advancedSample

import kelm.Element
import kelm.ExternalException
import kelm.UpdateContext
import kelm.buildModelCmds
import kelm.sample.advancedSample.SignUpElement.Cmd
import kelm.sample.advancedSample.SignUpElement.Model
import kelm.sample.advancedSample.SignUpElement.Msg
import kelm.sample.advancedSample.form.FormElement
import kelm.sample.advancedSample.registerPet.RegisterPetElement
import kelm.sample.advancedSample.form.FormElement.Cmd as FormCmd
import kelm.sample.advancedSample.form.FormElement.Model as FormModel
import kelm.sample.advancedSample.form.FormElement.Msg as FormMsg
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Cmd as RegPetCmd
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Model as RegPetModel
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Msg as RegPetMsg

object SignUpElement : Element<Model, Msg, Cmd, Nothing>() {

    sealed class Model {
        data class FormStep(val formModel: FormModel) : Model()
        data class RegisterPetStep(val regPetModel: RegPetModel) : Model()

        data class AppMainScreen(val userId: String) : Model()
        data class PetScreen(val userId: String, val petId: String) : Model()
    }

    sealed class Msg {
        data class Form(val formMsg: FormMsg) : Msg()
        data class RegisterPet(val registerPetMsg: RegPetMsg) : Msg()
    }

    sealed class Cmd {
        data class Form(val formCmd: FormCmd) : Cmd()
        data class RegisterPet(val registerPetCmd: RegPetCmd) : Cmd()
    }

    fun initModel(idempotencyKey: String): Model =
        FormElement.initModel(idempotencyKey)
            .let(Model::FormStep)

    override fun update(model: Model, msg: Msg) = buildModelCmds {
        when {
            model is Model.FormStep && msg is Msg.Form ->
                updateChild(FormElement, model.formModel, msg.formMsg)
                    .toParent(Model::FormStep, Cmd::Form) { childCmd ->
                        if (childCmd is FormCmd.Finish) handleNavigationFromForm(childCmd)
                        else null
                    }

            model is Model.RegisterPetStep && msg is Msg.RegisterPet ->
                updateChild(RegisterPetElement, model.regPetModel, msg.registerPetMsg)
                    .toParent(Model::RegisterPetStep, Cmd::RegisterPet) { childCmd ->
                        if (childCmd is RegPetCmd.Finish)
                            handleNavigationFromRegisterPet(model, childCmd)
                        else null
                    }
            else -> null
        }
    }

    private fun UpdateContext<Cmd>.handleNavigationFromForm(
        childCmd: FormCmd.Finish,
    ): Model {
        val (userId, petName) = childCmd
        return if (petName != null) goToRegisterPetScreen(userId = userId, petName = petName)
        else Model.AppMainScreen(userId = userId)
    }

    private fun handleNavigationFromRegisterPet(
        model: Model.RegisterPetStep,
        childCmd: RegPetCmd.Finish,
    ): Model =
        when (val petId = childCmd.petId) {
            null -> Model.AppMainScreen(userId = model.regPetModel.userId)
            else -> Model.PetScreen(userId = model.regPetModel.userId, petId = petId)
        }

    override fun exceptionToMsg(exception: ExternalException): Msg? = null

    private fun UpdateContext<Cmd>.goToRegisterPetScreen(
        userId: String,
        petName: String,
    ): Model {
        val initModel = RegisterPetElement.initModel(userId = userId, petName = petName)
        return updateChild(RegisterPetElement, initModel, null)
            .toParent(Model::RegisterPetStep, Cmd::RegisterPet)!!
    }
}
