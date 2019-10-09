package kelm.sample.signUp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kelm.sample.R
import kelm.sample.SimpleTextWatcher
import kelm.sample.signUp.SignUpElement.Model
import kelm.sample.signUp.SignUpElement.Msg
import kotlinx.android.synthetic.main.activity_sign_up_form_sample.*
import kotlinx.android.synthetic.main.layout_sign_up_form.*
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_device.*
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_pet.*

class SignUpFormSampleActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this).get(SignUpFormViewModel::class.java)
    }



    private var inflatedViewId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_form_sample)
    }

    override fun onStart() {
        super.onStart()

        viewModel.observeModel().observe(this, Observer { model ->
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
}
