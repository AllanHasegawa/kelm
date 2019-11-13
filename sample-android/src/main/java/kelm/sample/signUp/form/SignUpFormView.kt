package kelm.sample.signUp.form

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import kelm.sample.R
import kelm.sample.SimpleTextWatcher
import kelm.sample.signUp.form.SignUpFormElement.EmailError
import kelm.sample.signUp.form.SignUpFormElement.PasswordError
import kotlinx.android.synthetic.main.layout_sign_up_form.view.*

class SignUpFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var onEmailChanged: (String) -> Unit = {}
    private var onPasswordChanged: (String) -> Unit = {}
    private var onPetNameChanged: (String) -> Unit = {}

    private val emailWatcher = SimpleTextWatcher { onEmailChanged(it) }
    private val passwordWatcher = SimpleTextWatcher { onPasswordChanged(it) }
    private val petNameWatcher = SimpleTextWatcher { onPetNameChanged(it) }

    init {
        View.inflate(context, R.layout.layout_sign_up_form, this)
    }

    fun bind(
        model: SignUpFormElement.Model,
        onEmailChanged: (String) -> Unit,
        onPasswordChanged: (String) -> Unit,
        onPetNameChanged: (String) -> Unit,
        onRegisterPetClicked: () -> Unit,
        onSubmitClick: () -> Unit
    ): Unit = with(model) {
        this@SignUpFormView.onEmailChanged = onEmailChanged
        this@SignUpFormView.onPasswordChanged = onPasswordChanged
        this@SignUpFormView.onPetNameChanged = onPetNameChanged

        formSubmitBt.setOnClickListener { onSubmitClick() }

        removeFormTextWatchers()

        formEmailEt.updateEditTextIfNeeded(email, inputEnabled)
        formEmailEt.error = when (emailError) {
            EmailError.Required -> "Email is required"
            EmailError.Validation -> "Email is not valid"
            null -> null
        }

        formPasswordEt.updateEditTextIfNeeded(password, inputEnabled)
        formPasswordEt.error = when (passwordError) {
            PasswordError.Required -> "Password is required"
            PasswordError.TooSimple -> "Passwords must be more than 6 letters"
            null -> null
        }

        formPetNameEt.updateEditTextIfNeeded(petName, inputEnabled)
        formPetNameEt.error = when (petNameRequiredError) {
            true -> "Pet name is required"
            false -> null
        }
        addFormTextWatchers()

        formRegisterPetCheckBox.setOnCheckedChangeListener { _, _ -> onRegisterPetClicked() }
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

    private fun EditText.updateEditTextIfNeeded(text: String?, enabled: Boolean) {
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
