package kelm.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Kelm
import kelm.sample.CounterContract.Model
import kelm.sample.CounterContract.Msg
import kotlinx.android.synthetic.main.activity_counter_sample.*

class CounterSampleActivity : AppCompatActivity() {
    private val msgSubj = PublishSubject.create<Msg>()
    private var stateDisposable: Disposable? = null

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

        stateDisposable = Kelm
            .build<Model, Msg, Nothing, Nothing>(
                msgObserver = msgSubj,
                initModel = CounterContract.initModel(),
                update = { model, msg ->
                    when (msg) {
                        is Msg.MinusClick -> model.copy(count = model.count - 1)
                        is Msg.PlusClick -> model.copy(count = model.count + 1)
                        is Msg.ResetClick -> model.copy(count = 0)
                    }
                }
            )
            .subscribe { state ->
                counterTv.text = state.count.toString()
                counterMinusBt.isEnabled = state.minusBtEnabled
                counterResetBt.isEnabled = state.resetBtEnabled
            }
    }

    override fun onStop() {
        stateDisposable?.dispose()
        stateDisposable = null
        super.onStop()
    }
}
