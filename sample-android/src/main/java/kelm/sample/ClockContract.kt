package kelm.sample

object ClockContract {
    const val SUB_CLOCK_TICK_ID = "clockTick"

    fun initModel() = Model.Paused as Model

    sealed class Msg {
        object ToggleRunPauseClock : Msg()
        data class Tick(val instant: Long) : Msg()
    }

    sealed class Model {
        object Paused : Model()
        data class Running(val instant: Long?) : Model()

        fun isRunning() = this is Running
    }

    sealed class Sub(id: String) : kelm.Sub(id) {
        object ClockTickSub : Sub("ClockTick")
    }
}
