package kelm.sample.signUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Log
import kelm.sample.signUp.SignUpElement.Cmd
import kelm.sample.signUp.SignUpElement.Model
import kelm.sample.signUp.SignUpElement.Msg
import kelm.sample.signUp.form.SignUpFormElement
import kelm.sample.signUp.registerDevice.SignUpRegisterDeviceElement
import kelm.sample.signUp.registerPet.SignUpRegisterPetElement
import java.util.UUID
import kelm.sample.signUp.form.SignUpFormElement.Cmd as FormCmd
import kelm.sample.signUp.form.SignUpFormElement.Msg as FormMsg
import kelm.sample.signUp.registerDevice.SignUpRegisterDeviceElement.Cmd as RegDevCmd
import kelm.sample.signUp.registerDevice.SignUpRegisterDeviceElement.Msg as RegDevMsg
import kelm.sample.signUp.registerPet.SignUpRegisterPetElement.Cmd as RegPetCmd
import kelm.sample.signUp.registerPet.SignUpRegisterPetElement.Msg as RegPetMsg

class SignUpViewModel : ViewModel() {
    private val msgSubj = PublishSubject.create<Msg>()

    private val modelLiveData = MutableLiveData<Model>()
    private val streamDisposable: Disposable
    private val idempotencyKey = UUID.randomUUID().toString()

    private val service = FakeSignUpService()

    init {
        streamDisposable = SignUpElement
            .start(
                initModel = SignUpElement.initModel(idempotencyKey = idempotencyKey),
                msgInput = msgSubj,
                cmdToMaybe = ::cmdToMaybe,
                subToObs = { _, _, _ -> Observable.empty() },
                logger = ::logger
            )
            .subscribe(modelLiveData::postValue)
    }

    fun observeModel(): LiveData<Model> = modelLiveData

    override fun onCleared() {
        super.onCleared()
        streamDisposable.dispose()
    }

    fun onEmailChanged(email: String) {
        SignUpFormElement.Msg.EmailChanged(email)
            .let(Msg::FromForm)
            .up()
    }

    fun onPasswordChanged(pass: String) {
        SignUpFormElement.Msg.PasswordChanged(pass)
            .let(Msg::FromForm)
            .up()
    }

    fun onPetNameChanged(name: String) {
        SignUpFormElement.Msg.PetNameChanged(name)
            .let(Msg::FromForm)
            .up()
    }

    fun onFormSubmitBtClick() {
        SignUpFormElement.Msg.Continue
            .let(Msg::FromForm)
            .up()
    }

    fun onPetRegisterClick() {
        SignUpFormElement.Msg.RegisterPetClicked
            .let(Msg::FromForm)
            .up()
    }

    fun onRegisterDeviceRetryClick() {
        SignUpRegisterDeviceElement.Msg.Retry
            .let(Msg::FromRegDevice)
            .up()
    }

    fun onRegisterPetContinueClick() {
        SignUpRegisterPetElement.Msg.Continue
            .let(Msg::FromRegPet)
            .up()
    }

    private fun cmdToMaybe(cmd: Cmd): Maybe<Msg> =
        when (cmd) {
            is Cmd.FromForm ->
                when (val it = cmd.it) {
                    is FormCmd.SubmitForm ->
                        service.submitForm(it.email, it.password, it.idempotencyKey)
                            .map(FormMsg::UserSignUpSuccessResponse)
                            .cast(FormMsg::class.java)
                            .onErrorReturn(FormMsg::UserSignUpError)
                            .map(Msg::FromForm)
                    is FormCmd.FinishSuccess -> error("Handled by parent context")
                }

            is Cmd.FromRegDevice ->
                when (val it = cmd.it) {
                    is RegDevCmd.RegisterDevice ->
                        service.registerDevice(it.userId)
                            .toSingleDefault(RegDevMsg.DeviceRegisterSuccessResponse)
                            .cast(RegDevMsg::class.java)
                            .onErrorReturn(RegDevMsg::DeviceRegisterError)
                            .map(Msg::FromRegDevice)
                    is RegDevCmd.FinishSuccess -> error("Handled by parent context")
                }

            is Cmd.FromPetDevice ->
                when (val it = cmd.it) {
                    is RegPetCmd.RegisterPet ->
                        service.registerPet(it.userId, it.petName)
                            .map(RegPetMsg::PetRegisterSuccessResponse)
                            .cast(RegPetMsg::class.java)
                            .onErrorReturn(RegPetMsg::PetRegisterError)
                            .map(Msg::FromRegPet)
                    is RegPetCmd.FinishSuccess,
                    is RegPetCmd.FinishWithError -> error("Handled by parent context")
                }
        }.cast(Msg::class.java)
            .toMaybe()

    private fun Msg.up() = msgSubj.onNext(this)

    private fun logger(log: Log<Model, Msg, Cmd, Nothing>): Disposable? {
        fun pp(s: Any?) = ("[${log.index}] " + s.toString()).let(::println)

        fun div() = (0..64).map { '*' }.joinToString("").let(::pp)

        when (log) {
            is Log.Update -> {
                div()
                pp(log.model)
                pp(log.msg)
                pp(log.modelPrime)
                div()
                pp(log.cmdsStarted)
                div()
            }
            else -> {
                div()
                pp(log)
                div()
            }
        }
        return null
    }
}
