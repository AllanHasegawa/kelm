package kelm.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Log
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
    private var elementDisposable: Disposable? = null

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

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        elementDisposable =
            ClockElement
                .start(
                    initModel = ClockElement.initModel(),
                    msgInput = msgSubj,
                    cmdToMaybe = { Maybe.empty() },
                    subToObs = { sub, _, _ ->
                        when (sub) {
                            is Sub.ClockTickSub -> clockTickGeneratorObs()
                        }
                    },
                    logger = ::logger
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

        elementDisposable?.dispose()
        elementDisposable = null
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

    private fun logger(log: Log<Model, Msg, Nothing, Sub>): Disposable? {
        when (log) {
            is Log.SubscriptionStarted -> toast("[${log.sub.id}] started")
            is Log.SubscriptionCancelled -> toast("[${log.sub.id}] cancelled")
            is Log.SubscriptionEmission -> println("[${log.sub.id}] emitted: ${log.msg}")
            is Log.Update -> println("---\nModel: ${log.modelPrime}\nSubs: ${log.subs}\n---")
        }

        return null
    }
}
