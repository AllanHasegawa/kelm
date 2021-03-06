package kelm.sample

import kelm.ExternalException
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import kelm.sample.ClockElement.Model
import kelm.sample.ClockElement.Msg
import kelm.sample.ClockElement.Sub

object ClockElement : Kelm.Element<Model, Msg, Nothing, Sub>() {
    sealed class Model {
        object Paused : Model()
        data class Running(val instant: Long?) : Model()

        fun isRunning() = this is Running
    }

    sealed class Msg {
        object ToggleRunPauseClock : Msg()
        data class Tick(val instant: Long) : Msg()
    }

    sealed class Sub(id: String) : kelm.Sub(id) {
        object ClockTickSub : Sub("ClockTick")
    }

    fun initModel() = Model.Paused

    override fun UpdateContext<Model, Msg, Nothing, Sub>.update(model: Model, msg: Msg): Model? =
        when (model) {
            is Model.Running ->
                when (msg) {
                    is Msg.ToggleRunPauseClock -> Model.Paused
                    is Msg.Tick -> model.copy(instant = msg.instant)
                }
            is Model.Paused ->
                when (msg) {
                    is Msg.ToggleRunPauseClock -> Model.Running(instant = null)
                    is Msg.Tick -> null // No-Op
                }
        }

    override fun SubContext<Sub>.subscriptions(model: Model) {
        when (model.isRunning()) {
            true -> +Sub.ClockTickSub
            false -> Unit // No-Op
        }
    }

    override fun errorToMsg(error: ExternalException): Msg? = null
}
