package kelm.sample.signUp.main

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kelm.sample.R

class SignUpAppMainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.layout_sign_up_form_app_main, this)
    }
}
