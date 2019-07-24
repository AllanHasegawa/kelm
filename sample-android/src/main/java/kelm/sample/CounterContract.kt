package kelm.sample

object CounterContract {
    fun initModel() = Model(count = 0)

    sealed class Msg {
        object MinusClick : Msg()
        object PlusClick : Msg()
        object ResetClick : Msg()
    }

    data class Model(val count: Int) {
        val resetBtEnabled = count > 0
        val minusBtEnabled = resetBtEnabled
    }
}