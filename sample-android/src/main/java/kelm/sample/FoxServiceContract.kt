package kelm.sample

import java.net.URL
import java.util.concurrent.TimeUnit

object FoxServiceContract {
    fun initModel() = Model.Loading as Model
    fun initCmd() = Cmd.Fetch(0, TimeUnit.MILLISECONDS) as Cmd

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
        data class Fetch(
            val delay: Long,
            val unit: TimeUnit
        ) : Cmd()
    }
}
