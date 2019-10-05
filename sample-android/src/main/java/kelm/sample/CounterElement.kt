package kelm.sample

import kelm.Kelm
import kelm.sample.CounterElement.Model
import kelm.sample.CounterElement.Msg

object CounterElement : Kelm.Sandbox<Model, Msg>() {
    sealed class Msg {
        object MinusClick : Msg()
        object PlusClick : Msg()
        object ResetClick : Msg()
    }

    data class Model(val count: Int) {
        val resetBtEnabled = count > 0
        val minusBtEnabled = resetBtEnabled
    }

    override fun initModel() = Model(count = 0)

    override fun updateSimple(model: Model, msg: Msg): Model? =
        when (msg) {
            is Msg.MinusClick -> model.copy(count = model.count - 1)
            is Msg.PlusClick -> model.copy(count = model.count + 1)
            is Msg.ResetClick -> model.copy(count = 0)
        }
}