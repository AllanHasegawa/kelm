package kelm.sample.advancedSample

import kelm.Log
import kelm.android.ElementViewModel
import kelm.sample.advancedSample.SignUpElement.Cmd
import kelm.sample.advancedSample.SignUpElement.Model
import kelm.sample.advancedSample.SignUpElement.Msg
import java.util.UUID
import kelm.sample.advancedSample.form.FormElement.Cmd as FormCmd
import kelm.sample.advancedSample.form.FormElement.Msg as FormMsg
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Cmd as RegisterPetCmd
import kelm.sample.advancedSample.registerPet.RegisterPetElement.Msg as RegisterPetMsg

class SignUpViewModel : ElementViewModel<Model, Msg, Cmd, Nothing>(
    SignUpElement,
    SignUpElement.initModel(UUID.randomUUID().toString())
) {
    private val service = FakeSignUpService()

    override suspend fun cmdExecutor(cmd: Cmd): Msg? =
        when (cmd) {
            is Cmd.Form ->
                when (val it = cmd.formCmd) {
                    is FormCmd.SubmitForm ->
                        try {
                            service.submitForm(it.email, it.password, it.idempotencyKey)
                                .let(FormMsg::SuccessResponse)
                        } catch (t: Throwable) {
                            FormMsg.UserSignUpError(t)
                        }.let(Msg::Form)
                    is FormCmd.Finish -> null
                }
            is Cmd.RegisterPet ->
                when (val it = cmd.registerPetCmd) {
                    is RegisterPetCmd.RegisterPet ->
                        try {
                            service.registerPet(it.userId, it.petName)
                                .let(RegisterPetMsg::SuccessResponse)
                        } catch (t: Throwable) {
                            RegisterPetMsg.PetRegisterError(t)
                        }.let(Msg::RegisterPet)
                    is RegisterPetCmd.Finish -> null
                }
        }

    override fun logger(log: Log<Model, Msg, Cmd, Nothing>) {
        fun div() = (0..64).map { '*' }.joinToString("").let(::println)

        when (log) {
            is Log.Update -> {
                div()
                println(log.model)
                println(log.msg)
                println(log.modelPrime)
                div()
                println(log.cmds)
                div()
            }
            else -> {
                div()
                println(log)
                div()
            }
        }
    }
}
