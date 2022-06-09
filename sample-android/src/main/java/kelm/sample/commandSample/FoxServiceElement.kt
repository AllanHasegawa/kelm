package kelm.sample.commandSample

import kelm.Element
import kelm.ExternalException
import kelm.buildModelCmds
import kelm.sample.commandSample.FoxServiceElement.Cmd
import kelm.sample.commandSample.FoxServiceElement.Model
import kelm.sample.commandSample.FoxServiceElement.Msg
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object FoxServiceElement : Element<Model, Msg, Cmd, Nothing>() {
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

    sealed class Cmd {
        data class Fetch(val delay: Duration) : Cmd()
    }

    fun initModel() = Model.Loading

    override fun initCmds(initModel: Model): List<Cmd> = listOf(
        Cmd.Fetch(500.milliseconds)
    )

    override fun update(model: Model, msg: Msg) = buildModelCmds {
        when (model) {
            is Model.Loading ->
                when (msg) {
                    is Msg.GotFoxPicUrl -> Model.ContentLoaded(foxPicUrl = msg.url)
                    is Msg.ConnError -> Model.ConnError
                    else -> null
                }

            is Model.ConnError ->
                when (msg) {
                    is Msg.Fetch -> Model.Loading.also {
                        +Cmd.Fetch(1.seconds)
                    }
                    is Msg.GotFoxPicUrl -> Model.ContentLoaded(foxPicUrl = msg.url)
                    else -> null
                }

            is Model.ContentLoaded ->
                when (msg) {
                    is Msg.Fetch -> Model.Loading.also {
                        +Cmd.Fetch(1.seconds)
                    }
                    else -> null
                }
        }
    }

    override fun exceptionToMsg(exception: ExternalException): Msg? = null
}
