package kelm.sample

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast

fun Activity.toast(msg: String, short: Boolean = true) =
    Toast.makeText(this, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        .show()

private val emailRegex = Regex("""[^@]+@[^\.]+\..+""")
fun isEmailValid(email: String): Boolean = emailRegex.matches(email)
