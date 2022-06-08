package kelm.sample.simpleSample

import kelm.Sandbox
import kelm.sample.simpleSample.CounterElement.Model
import kelm.sample.simpleSample.CounterElement.Msg

object CounterElement : Sandbox<Model, Msg>() {
    data class Model(val count: Int) {
        val resetBtEnabled = count > 0
        val minusBtEnabled = resetBtEnabled
    }

    sealed class Msg {
        object MinusClick : Msg()
        object PlusClick : Msg()
        object ResetClick : Msg()
    }

    fun initModel() = Model(count = 0)

    override fun updateSimple(model: Model, msg: Msg): Model =
        when (msg) {
            is Msg.MinusClick -> model.copy(count = model.count - 1)
            is Msg.PlusClick -> model.copy(count = model.count + 1)
            is Msg.ResetClick -> model.copy(count = 0)
        }
}
