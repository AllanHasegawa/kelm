package kelm.sample

import kelm.ExternalError
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.FoxServiceElement.Cmd
import kelm.sample.FoxServiceElement.Model
import kelm.sample.FoxServiceElement.Msg
import java.net.URL
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

object FoxServiceElement : Kelm.Element<Model, Msg, Cmd, Nothing>() {
    sealed class Model {
        object Loading : Model()
        object ConnError : Model()
        data class ContentLoaded(val foxPicUrl: URL) : Model()
    }

    sealed class Msg {
        object Fetch : Msg()
        data class GotFoxPicUrl(val url: URL) : Msg()
        object ConnError : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        @ExperimentalTime
        data class Fetch(val delay: Duration) : Cmd()
    }

    override fun initModel() = Model.Loading

    @ExperimentalTime
    override fun initCmds(): List<Cmd>? = listOf(
        Cmd.Fetch(500.toDuration(DurationUnit.MILLISECONDS))
    )

    @ExperimentalTime
    override fun UpdateContext<Model, Msg, Cmd>.update(model: Model, msg: Msg): Model? =
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
                        +Cmd.Fetch(1.toDuration(DurationUnit.SECONDS))
                    }
                    is Msg.GotFoxPicUrl -> Model.ContentLoaded(foxPicUrl = msg.url)
                    else -> null
                }

            is Model.ContentLoaded ->
                when (msg) {
                    is Msg.Fetch -> Model.Loading.also {
                        +Cmd.Fetch(1.toDuration(DurationUnit.SECONDS))
                    }
                    else -> null
                }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) {}

    override fun errorToMsg(error: ExternalError): Msg? = null
}
