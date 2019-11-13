package kelm.sample.signUp.registerPet

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kelm.sample.R
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_pet.view.*

class SignUpRegisterPetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.layout_sign_up_form_registering_pet, this)
    }

    fun bind(
        model: SignUpRegisterPetElement.Model,
        onContinueClick: () -> Unit
    ): Unit = with(model) {
        signUpRegPetContinueBt.setOnClickListener { onContinueClick() }

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
