package kelm.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.sample.ClockElement.Model
import kelm.sample.ClockElement.Msg
import kelm.sample.ClockElement.Sub
import kotlinx.android.synthetic.main.activity_clock_sample.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ClockSampleActivity : AppCompatActivity() {
    private val msgSubj = PublishSubject.create<Msg>()
    private var stateDisposable: Disposable? = null

    private val dateTimeFormatter by lazy {
        SimpleDateFormat("HH:mm:ss z", Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_sample)

        clockActionBt.setOnClickListener {
            msgSubj.onNext(Msg.ToggleRunPauseClock)
        }
    }

    override fun onStart() {
        super.onStart()

        stateDisposable =
            ClockElement
                .start(
                    msgInput = msgSubj,
                    cmdToMaybe = { Maybe.empty() },
                    subToObs = { sub, _, _ ->
                        when (sub) {
                            is Sub.ClockTickSub -> clockTickGeneratorObs()
                        }
                    }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { model ->
                    when (model) {
                        is Model.Running -> {
                            clockTv.text = model.instant?.let(::formatDateTime) ?: "---"
                            clockActionBt.text = "Pause"
                        }
                        is Model.Paused -> {
                            clockTv.text = "---"
                            clockActionBt.text = "Start"
                        }
                    }
                }
    }

    override fun onStop() {
        super.onStop()

        stateDisposable?.dispose()
        stateDisposable = null
    }

    private fun formatDateTime(instant: Long): String =
        instant
            .let(::Date)
            .let(dateTimeFormatter::format)

    private fun clockTickGeneratorObs() =
        Observable.interval(0, 500, TimeUnit.MILLISECONDS)
            .map { System.currentTimeMillis() }
            .map(Msg::Tick)
            .cast(Msg::class.java)
            .doOnSubscribe { toast("Clock tick subscribed") }
            .doOnDispose { toast("Clock tick disposed") }
}
