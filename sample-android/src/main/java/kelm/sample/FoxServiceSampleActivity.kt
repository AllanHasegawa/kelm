package kelm.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Log
import kelm.sample.FoxServiceElement.Cmd
import kelm.sample.FoxServiceElement.Model
import kelm.sample.FoxServiceElement.Msg
import kotlinx.android.synthetic.main.activity_fox_service_sample.*
import kotlinx.android.synthetic.main.layout_fox_service_conn_error.*
import kotlinx.android.synthetic.main.layout_fox_service_content.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class FoxServiceSampleActivity : AppCompatActivity() {
    private val service = FakeFoxPicsService()

    private val msgSubj = PublishSubject.create<Msg>()
    private var modelDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fox_service_sample)

        fun Button.sendMsgOnClick(msg: Msg) = setOnClickListener { msgSubj.onNext(msg) }
        foxServiceFetchBt.sendMsgOnClick(Msg.Fetch)
        foxServiceRetryBt.sendMsgOnClick(Msg.Fetch)
    }

    @ExperimentalTime
    override fun onStart() {
        super.onStart()

        modelDisposable = FoxServiceElement
            .start(
                msgInput = msgSubj,
                cmdToMaybe = ::cmdToMaybe,
                subToObs = { _, _, _ -> Observable.empty() },
                logger = ::logger
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { model ->
                when (model) {
                    is Model.Loading -> handleLoadingModel()
                    is Model.ContentLoaded -> handleContentLoadedModel(model)
                    is Model.ConnError -> handleConnErrorModel()
                }
            }
    }

    @ExperimentalTime
    private fun cmdToMaybe(cmd: Cmd): Maybe<Msg> =
        when (cmd) {
            is Cmd.Fetch -> fetchCmd(cmd.delay)
        }

    private fun handleConnErrorModel() {
        hideAllLayouts()
        foxServiceConnErrorLayout.visibility = View.VISIBLE
    }

    private fun handleContentLoadedModel(model: Model.ContentLoaded) {
        hideAllLayouts()
        foxServiceContentLayout.visibility = View.VISIBLE

        toast("Loading fox pic: ${model.foxPicUrl}")
        Picasso.get()
            .load(model.foxPicUrl.toString())
            .centerCrop()
            .fit()
            .into(foxServicePicIv)
    }

    private fun handleLoadingModel() {
        hideAllLayouts()
        foxServiceLoadingLayout.visibility = View.VISIBLE
    }

    private fun hideAllLayouts() {
        listOf(foxServiceConnErrorLayout, foxServiceContentLayout, foxServiceLoadingLayout)
            .forEach { it.visibility = View.GONE }
    }

    @ExperimentalTime
    private fun fetchCmd(delay: Duration) =
        Single.timer(delay.toLongMilliseconds(), TimeUnit.MILLISECONDS)
            .flatMap {
                service.fetchRandomFoxPicUrl()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .map(Msg::GotFoxPicUrl)
            .cast(Msg::class.java)
            .onErrorReturn { Msg.ConnError }
            .toMaybe()

    private fun logger(log: Log<Model, Msg, Cmd, Nothing>): Disposable? {
        println(log)
        return null
    }
}
