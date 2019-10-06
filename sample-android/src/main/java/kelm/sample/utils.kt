package kelm.sample

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast

fun Activity.toast(msg: String, short: Boolean = true) =
    Toast.makeText(this, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        .show()

class SimpleTextWatcher(private val update: (String) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        s?.toString()?.let { update(it) }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
}

private val emailRegex = Regex("""[^@]+@[^\.]+\..+""")
fun isEmailValid(email: String): Boolean = emailRegex.matches(email)