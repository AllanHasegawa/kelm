package kelm.sample.signUp.registerDevice

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kelm.sample.R
import kotlinx.android.synthetic.main.layout_sign_up_form_registering_device.view.*

class SignUpRegisterDeviceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.layout_sign_up_form_registering_device, this)
    }

    fun bind(
        model: SignUpRegisterDeviceElement.Model,
        onRetryClick: () -> Unit
    ): Unit = with(model) {

        signUpRegDeviceRetryBt.setOnClickListener { onRetryClick() }

        when (retryButtonLoading) {
            true -> signUpRegDeviceRetryBt.startAnimation()
            false -> signUpRegDeviceRetryBt.revertAnimation()
        }

        signUpRegDeviceStatusTv.text = when (showErrorMessage) {
            true -> "Error when setting up your account. Try again."
            false -> "Setting up your account"
        }
    }
}
