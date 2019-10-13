package kelm.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Log
import kelm.sample.CounterElement.Model
import kelm.sample.CounterElement.Msg
import kotlinx.android.synthetic.main.activity_counter_sample.*

class CounterSampleActivity : AppCompatActivity() {
    private val msgSubj = PublishSubject.create<Msg>()
    private var elementDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter_sample)

        fun Button.sendMsgOnClick(msg: Msg) = setOnClickListener { msgSubj.onNext(msg) }
        counterPlusBt.sendMsgOnClick(Msg.PlusClick)
        counterMinusBt.sendMsgOnClick(Msg.MinusClick)
        counterResetBt.sendMsgOnClick(Msg.ResetClick)
    }

    override fun onStart() {
        super.onStart()

        elementDisposable = CounterElement
            .start(
                initModel = CounterElement.initModel(),
                msgInput = msgSubj,
                logger = ::logger
            )
            .subscribe { state ->
                counterTv.text = state.count.toString()
                counterMinusBt.isEnabled = state.minusBtEnabled
                counterResetBt.isEnabled = state.resetBtEnabled
            }
    }

    override fun onStop() {
        elementDisposable?.dispose()
        elementDisposable = null
        super.onStop()
    }

    @SuppressLint("SetTextI18n")
    private fun logger(log: Log<Model, Msg, Nothing, Nothing>): Disposable? {
        when (log) {
            is Log.Update -> {
                val msgStr = when (log.msg) {
                    null -> "init"
                    is Msg.MinusClick -> "-"
                    is Msg.PlusClick -> "+"
                    is Msg.ResetClick -> "@"
                }
                val previousCount = log.model.count
                val newCount = log.modelPrime!!.count
                counterLogTv.text =
                    counterLogTv.text.toString() +
                        "\n" +
                        "$previousCount -> '$msgStr' -> $newCount"
            }
        }

        return null
    }
}
