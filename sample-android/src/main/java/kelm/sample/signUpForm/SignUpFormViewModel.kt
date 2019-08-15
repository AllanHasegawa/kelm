package kelm.sample.signUpForm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Kelm
import kelm.UpdateContext
import kelm.sample.isEmailValid
import kelm.sample.signUpForm.SignUpFormContract.Cmd
import kelm.sample.signUpForm.SignUpFormContract.EmailError
import kelm.sample.signUpForm.SignUpFormContract.Model
import kelm.sample.signUpForm.SignUpFormContract.Msg
import kelm.sample.signUpForm.SignUpFormContract.PasswordError
import java.util.*

class SignUpFormViewModel : ViewModel() {
    val msgSubj = PublishSubject.create<Msg>()

    private val modelLiveData = MutableLiveData<Model>()
    private val streamDisposable: Disposable

    private val service = FakeSignUpService()

    init {
        streamDisposable = Kelm
            .build<Model, Msg, Cmd, Nothing>(
                msgInput = msgSubj,
                initModel = initModel(),
                cmdToMaybe = ::cmdToMaybe,
                update = { model, msg -> update(model, msg) }
            )
            .subscribe(modelLiveData::postValue)
    }

    fun observeModel(): LiveData<Model> = modelLiveData

    override fun onCleared() {
        super.onCleared()
        streamDisposable.dispose()
    }

    private fun UpdateContext<Cmd>.update(model: Model, msg: Msg) =
        when (model) {
            is Model.FormVisible -> handleFormVisibleUpdate(model, msg)
            is Model.RegisteringDevice -> handleRegisteringDeviceUpdate(model, msg)
            is Model.RegisteringPet -> handleRegisterPetUpdate(model, msg)
            is Model.AppMainScreen,
            is Model.PetScreen -> model
        }

    private fun UpdateContext<Cmd>.handleFormVisibleUpdate(model: Model.FormVisible, msg: Msg) = with(model) {
        when (msg) {
            is Msg.EmailChanged -> copy(email = msg.email, emailError = null)
            is Msg.PasswordChanged -> copy(password = msg.password, passwordError = null)
            is Msg.PetNameChanged -> copy(petName = msg.petName)
            is Msg.RegisterPetClicked -> copy(showPetNameInput = !showPetNameInput)
            is Msg.Continue -> {
                var emailError: EmailError? = null
                var passwordError: PasswordError? = null
                var petNameRequiredError = false
                if (!isEmailValid(email)) {
                    emailError = EmailError.Validation
                }
                if (email.isBlank()) {
                    emailError = EmailError.Required
                }
                if (password.length < 6) {
                    passwordError = PasswordError.TooSimple
                }
                if (password.isEmpty()) {
                    passwordError = PasswordError.Required
                }
                if (showPetNameInput && petName.isNullOrBlank()) {
                    petNameRequiredError = true
                }
                if (emailError != null || passwordError != null || petNameRequiredError) {
                    return copy(
                        emailError = emailError, passwordError = passwordError,
                        petNameRequiredError = petNameRequiredError
                    )
                }

                copy(buttonLoading = true, inputEnabled = false, showConnErrorCTA = false)
                    .also {
                        +Cmd.SubmitForm(
                            idempotencyKey = requestIdempotencyKey,
                            email = email,
                            password = password
                        )
                    }
            }
            is Msg.UserSignUpSuccessResponse ->
                Model
                    .RegisteringDevice(
                        userId = msg.userId,
                        petName = petName,
                        showErrorMessage = false,
                        retryButtonLoading = true
                    )
                    .also { +Cmd.RegisterDevice(userId = msg.userId) }

            is Msg.UserSignUpError ->
                copy(buttonLoading = false, showConnErrorCTA = true, inputEnabled = false)

            else -> this
        }
    }

    private fun UpdateContext<Cmd>.handleRegisteringDeviceUpdate(
        model: Model.RegisteringDevice,
        msg: Msg
    ) = with(model) {
        fun Model.RegisteringDevice.goToNextScreen() =
            if (petName != null) {
                Model.RegisteringPet(
                    userId = userId,
                    petName = petName,
                    showErrorMessage = false,
                    showContinueButton = false
                ).also { +Cmd.RegisterPet(userId = userId, petName = petName) }
            } else {
                Model.AppMainScreen(userId = userId)
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
            else -> this
        }
    }

    private fun handleRegisterPetUpdate(
        model: Model.RegisteringPet,
        msg: Msg
    ) = with(model) {
        when (msg) {
            is Msg.PetRegisterSuccessResponse ->
                Model.PetScreen(userId = userId, petId = msg.petId)
            is Msg.PetRegisterError ->
                copy(showErrorMessage = true, showContinueButton = true)
            is Msg.Continue -> Model.AppMainScreen(userId = userId)
            else -> this
        }
    }

    private fun initModel(): Model =
        Model.FormVisible(
            email = "",
            emailError = null,
            password = "",
            passwordError = null,
            showPetNameInput = false,
            inputEnabled = true,
            petName = null,
            petNameRequiredError = false,
            buttonLoading = false,
            requestIdempotencyKey = UUID.randomUUID().toString(),
            showConnErrorCTA = false
        )

    private fun cmdToMaybe(cmd: Cmd): Maybe<Msg> =
        when (cmd) {
            is Cmd.SubmitForm -> service.submitForm(cmd.email, cmd.password, cmd.idempotencyKey)
                .map(Msg::UserSignUpSuccessResponse)
                .cast(Msg::class.java)
                .onErrorReturn(Msg::UserSignUpError)

            is Cmd.RegisterDevice -> service.registerDevice(cmd.userId)
                .toSingleDefault(Msg.DeviceRegisterSuccessResponse)
                .cast(Msg::class.java)
                .onErrorReturn(Msg::DeviceRegisterError)

            is Cmd.RegisterPet -> service.registerPet(cmd.userId, cmd.petName)
                .map(Msg::PetRegisterSuccessResponse)
                .cast(Msg::class.java)
                .onErrorReturn(Msg::PetRegisterError)
        }.toMaybe()
}
