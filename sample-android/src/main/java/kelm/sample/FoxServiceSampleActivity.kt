package kelm.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.Kelm
import kelm.UpdateContext
import kelm.sample.FoxServiceContract.Cmd
import kelm.sample.FoxServiceContract.Model
import kelm.sample.FoxServiceContract.Msg
import kotlinx.android.synthetic.main.activity_fox_service_sample.*
import kotlinx.android.synthetic.main.layout_fox_service_conn_error.*
import kotlinx.android.synthetic.main.layout_fox_service_content.*
import java.util.concurrent.TimeUnit

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

    override fun onStart() {
        super.onStart()

        modelDisposable = Kelm
            .build<Model, Msg, Cmd, Nothing>(
                msgObserver = msgSubj,
                initModel = FoxServiceContract.initModel(),
                initCmd = FoxServiceContract.initCmd(),
                update = { model, msg -> update(model, msg) },
                cmdToMaybe = { cmd: Cmd -> cmdToMaybe(cmd) }
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

    private fun UpdateContext<Cmd>.update(model: Model, msg: Msg) =
        when (model) {
            is Model.Loading ->
                when (msg) {
                    is Msg.GotFoxPicUrl -> Model.ContentLoaded(foxPicUrl = msg.url)

                    is Msg.ConnError -> Model.ConnError

                    else -> model
                }

            is Model.ConnError ->
                when (msg) {
                    is Msg.Fetch -> Model.Loading.also {
                        +Cmd.Fetch(1, TimeUnit.SECONDS)
                    }
                    is Msg.GotFoxPicUrl -> Model.ContentLoaded(foxPicUrl = msg.url)
                    else -> model
                }

            is Model.ContentLoaded ->
                when (msg) {
                    is Msg.Fetch -> Model.Loading.also {
                        +Cmd.Fetch(1, TimeUnit.SECONDS)
                    }
                    else -> model
                }
        }

    private fun cmdToMaybe(cmd: Cmd) =
        when (cmd) {
            is Cmd.Fetch -> fetchCmd(cmd.delay, cmd.unit)
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

    private fun fetchCmd(delay: Long, unit: TimeUnit) =
        Single.timer(delay, unit)
            .flatMap {
                service.fetchRandomFoxPicUrl()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .map(Msg::GotFoxPicUrl)
            .cast(Msg::class.java)
            .onErrorReturn { Msg.ConnError }
            .toMaybe()
}
