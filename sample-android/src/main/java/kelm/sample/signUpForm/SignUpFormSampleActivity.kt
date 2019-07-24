package kelm.sample.signUpForm

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kelm.sample.R
import kelm.sample.SimpleTextWatcher
import kelm.sample.signUpForm.SignUpFormContract.Model
import kelm.sample.signUpForm.SignUpFormContract.Msg
import kotlinx.android.synthetic.main.activity_sign_up_form_sample.*
import kotlinx.android.synthetic.main.layout_sign_up_form.*
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_device.*
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_pet.*

class SignUpFormSampleActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProviders.of(this).get(SignUpFormViewModel::class.java)
    }

    private val msgSubj by lazy { viewModel.msgSubj }

    private val emailWatcher = SimpleTextWatcher { msgSubj.onNext(Msg.EmailChanged(it)) }
    private val passwordWatcher = SimpleTextWatcher { msgSubj.onNext(Msg.PasswordChanged(it)) }
    private val petNameWatcher = SimpleTextWatcher { msgSubj.onNext(Msg.PetNameChanged(it)) }

    private var inflatedViewId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_form_sample)
    }

    override fun onStart() {
        super.onStart()

        msgSubj.subscribe { println(it) }

        viewModel.observeModel().observe(this, Observer { model ->
            println(model)
            inflateViewByModel(model)

            when (model) {
                is Model.FormVisible -> handleFormVisible(model)
                is Model.RegisteringDevice -> handleRegisteringDevice(model)
                is Model.RegisteringPet -> handleRegisteringPet(model)
                is Model.PetScreen,
                is Model.AppMainScreen -> Unit
            }
        })
    }

    private fun inflateViewByModel(model: Model) {
        val viewId = when (model) {
            is Model.FormVisible -> R.layout.layout_sign_up_form
            is Model.RegisteringDevice -> R.layout.layout_sign_up_form_registering_device
            is Model.RegisteringPet -> R.layout.layout_sign_up_form_registering_pet
            is Model.AppMainScreen -> R.layout.layout_sign_up_form_app_main
            is Model.PetScreen -> R.layout.layout_sign_up_form_app_pet
        }

        with(signUpContainer) {
            if (viewId != inflatedViewId) {
                if (childCount > 0) {
                    removeAllViews()
                }
                inflatedViewId = viewId
                val view = layoutInflater.inflate(viewId, this, false)
                addView(view)
            }
        }
    }

    private fun handleFormVisible(model: Model.FormVisible) = with(model) {
        fun EditText.updateEditTextIfNeeded(text: String?, enabled: Boolean) {
            if (this.text.toString() != text) {
                this.setText(text)
                if (text != null) {
                    this.setSelection(text.length)
                }
            }
            this.isEnabled = enabled
            this.isFocusable = enabled
            this.isFocusableInTouchMode = enabled
        }

        formSubmitBt.setOnClickListener { msgSubj.onNext(Msg.Continue) }

        removeFormTextWatchers()

        formEmailEt.updateEditTextIfNeeded(email, inputEnabled)
        formEmailEt.error = when (emailError) {
            SignUpFormContract.EmailError.Required -> "Email is required"
            SignUpFormContract.EmailError.Validation -> "Email is not valid"
            null -> null
        }

        formPasswordEt.updateEditTextIfNeeded(password, inputEnabled)
        formPasswordEt.error = when (passwordError) {
            SignUpFormContract.PasswordError.Required -> "Password is required"
            SignUpFormContract.PasswordError.TooSimple -> "Passwords must be more than 6 letters"
            null -> null
        }

        formPetNameEt.updateEditTextIfNeeded(petName, inputEnabled)
        formPetNameEt.error = when (petNameRequiredError) {
            true -> "Pet name is required"
            false -> null
        }
        addFormTextWatchers()

        formRegisterPetCheckBox.setOnCheckedChangeListener { _, _ -> msgSubj.onNext(Msg.RegisterPetClicked) }
        formRegisterPetCheckBox.isChecked = showPetNameInput
        formRegisterPetCheckBox.isEnabled = inputEnabled
        formPetNameEt.visibility = when (showPetNameInput) {
            true -> View.VISIBLE
            false -> View.INVISIBLE
        }

        when (buttonLoading) {
            true -> formSubmitBt.startAnimation()
            false -> formSubmitBt.revertAnimation()
        }
        formConnErrorTv.visibility = when (showConnErrorCTA) {
            true -> View.VISIBLE
            false -> View.INVISIBLE
        }
    }

    private fun handleRegisteringDevice(model: Model.RegisteringDevice) = with(model) {
        signUpRegDeviceRetryBt.setOnClickListener { msgSubj.onNext(Msg.Retry) }

        when (retryButtonLoading) {
            true -> signUpRegDeviceRetryBt.startAnimation()
            false -> signUpRegDeviceRetryBt.revertAnimation()
        }

        signUpRegDeviceStatusTv.text = when (showErrorMessage) {
            true -> "Error when setting up your account. Try again."
            false -> "Setting up your account"
        }
    }

    private fun handleRegisteringPet(model: Model.RegisteringPet) = with(model) {
        signUpRegPetContinueBt.setOnClickListener { msgSubj.onNext(Msg.Continue) }

        when (showContinueButton) {
            true -> signUpRegPetContinueBt.revertAnimation()
            false -> signUpRegPetContinueBt.startAnimation()
        }

        signUpRegPetStatusTv.text = when (showErrorMessage) {
            true -> "Error while registering your pet, please try again later."
            false -> "Registering $petName <3"
        }
    }

    private fun addFormTextWatchers() {
        formEmailEt.addTextChangedListener(emailWatcher)
        formPasswordEt.addTextChangedListener(passwordWatcher)
        formPetNameEt.addTextChangedListener(petNameWatcher)
    }

    private fun removeFormTextWatchers() {
        formEmailEt.removeTextChangedListener(emailWatcher)
        formPasswordEt.removeTextChangedListener(passwordWatcher)
        formPetNameEt.removeTextChangedListener(petNameWatcher)
    }
}
