package kelm.sample.signUp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kelm.sample.R
import kelm.sample.signUp.SignUpElement.Model
import kelm.sample.signUp.form.SignUpFormView
import kelm.sample.signUp.main.SignUpAppMainView
import kelm.sample.signUp.main.SignUpPetScreenView
import kelm.sample.signUp.registerDevice.SignUpRegisterDeviceView
import kelm.sample.signUp.registerPet.SignUpRegisterPetView
import kotlinx.android.synthetic.main.activity_sign_up_form_sample.*
import kotlin.reflect.KFunction

class SignUpFormSampleActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    private var lastViewConstructorUsed: KFunction<*>? = null

    private val modelToViewConstructor = mapOf(
        Model.FormVisible::class to ::SignUpFormView,
        Model.RegisterDevice::class to ::SignUpRegisterDeviceView,
        Model.RegisterPet::class to ::SignUpRegisterPetView,
        Model.AppMainScreen::class to ::SignUpAppMainView,
        Model.PetScreen::class to ::SignUpPetScreenView
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_form_sample)
    }

    override fun onStart() {
        super.onStart()

        viewModel.observeModel().observe(this, Observer { model ->
            inflateViewByModel(model)

            when (model) {
                is Model.FormVisible ->
                    getViewUnsafe<SignUpFormView>().bind(
                        model.formModel,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onPetNameChanged = viewModel::onPetNameChanged,
                        onRegisterPetClicked = viewModel::onPetRegisterClick,
                        onSubmitClick = viewModel::onFormSubmitBtClick
                    )
                is Model.RegisterDevice ->
                    getViewUnsafe<SignUpRegisterDeviceView>().bind(
                        model = model.regDeviceModel,
                        onRetryClick = viewModel::onRegisterDeviceRetryClick
                    )
                is Model.RegisterPet ->
                    getViewUnsafe<SignUpRegisterPetView>().bind(
                        model = model.regPetModel,
                        onContinueClick = viewModel::onRegisterPetContinueClick
                    )
                is Model.PetScreen,
                is Model.AppMainScreen -> Unit
            }
        })
    }

    private fun inflateViewByModel(model: Model) {
        val viewConstructor = modelToViewConstructor[model::class] ?: error("Ops")

        if (lastViewConstructorUsed != viewConstructor) {
            with(signUpContainer) {
                if (childCount > 0) {
                    removeAllViews()
                }

                lastViewConstructorUsed = viewConstructor
                val view = viewConstructor.invoke(context, null, 0)
                addView(view)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : View> getViewUnsafe() = signUpContainer[0] as T
}
