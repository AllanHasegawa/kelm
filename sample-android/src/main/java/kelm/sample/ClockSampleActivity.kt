package kelm.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Kelm
import kelm.SubContext
import kelm.sample.ClockContract.Model
import kelm.sample.ClockContract.Msg
import kelm.sample.ClockContract.Sub
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

        stateDisposable = Kelm
            .build<Model, Msg, Nothing, Sub>(
                msgInput = msgSubj,
                initModel = ClockContract.initModel(),
                subscriptions = { model, _, _ -> subscriptions(model) },
                subToObservable = { sub ->
                    when (sub) {
                        is Sub.ClockTickSub -> clockTickGeneratorObs()
                    }
                },
                update = { model, msg -> update(model, msg) }
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

    private fun update(model: Model, msg: Msg) =
        when (model) {
            is Model.Running ->
                when (msg) {
                    is Msg.ToggleRunPauseClock -> Model.Paused
                    is Msg.Tick -> model.copy(instant = msg.instant)
                }
            is Model.Paused ->
                when (msg) {
                    is Msg.ToggleRunPauseClock -> Model.Running(instant = null)
                    is Msg.Tick -> model // No-Op
                }
        }

    private fun SubContext<Sub>.subscriptions(model: Model) {
        when (model.isRunning()) {
            true -> runSub(Sub.ClockTickSub)
            false -> Unit // No-Op
        }
    }

    private fun formatDateTime(instant: Long): String =
        instant
            .let(::Date)
            .let(dateTimeFormatter::format)

    private fun clockTickGeneratorObs() =
        Observable.interval(500, TimeUnit.MILLISECONDS)
            .map { System.currentTimeMillis() }
            .map(Msg::Tick)
            .cast(Msg::class.java)
            .doOnSubscribe { toast("Clock tick subscribed") }
            .doOnDispose { toast("Clock tick disposed") }
}
