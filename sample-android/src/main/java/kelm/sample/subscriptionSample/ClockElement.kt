package kelm.sample.subscriptionSample

import kelm.Element
import kelm.ExternalException
import kelm.buildModelCmds
import kelm.buildSubs
import kelm.sample.subscriptionSample.ClockElement.Model
import kelm.sample.subscriptionSample.ClockElement.Msg
import kelm.sample.subscriptionSample.ClockElement.Sub

object ClockElement : Element<Model, Msg, Nothing, Sub>() {
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

    override fun subscriptions(model: Model) = buildSubs {
        when (model.isRunning()) {
            true -> +Sub.ClockTickSub
            false -> Unit // No-Op
        }
    }

    override fun update(model: Model, msg: Msg) = buildModelCmds {
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
    }

    override fun exceptionToMsg(exception: ExternalException): Msg? = null
}
