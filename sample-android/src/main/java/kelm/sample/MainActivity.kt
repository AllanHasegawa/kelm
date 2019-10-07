package kelm.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kelm.sample.signUpForm.SignUpFormSampleActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fun <T> Button.openSampleOnClick(sampleClass: Class<T>) {
            setOnClickListener {
                Intent(this@MainActivity, sampleClass)
                    .let(this@MainActivity::startActivity)
            }
        }

        counterSampleBt.openSampleOnClick(CounterSampleActivity::class.java)
        clockSampleBt.openSampleOnClick(ClockSampleActivity::class.java)
        foxServiceSampleBt.openSampleOnClick(FoxServiceSampleActivity::class.java)
        signUpSampleBt.openSampleOnClick(SignUpFormSampleActivity::class.java)
    }
}
