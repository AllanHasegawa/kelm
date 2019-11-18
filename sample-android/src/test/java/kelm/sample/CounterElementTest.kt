package kelm.sample

import io.kotlintest.shouldBe
import kelm.sample.CounterElement.Model
import kelm.sample.CounterElement.Msg
import org.spekframework.spek2.Spek

object CounterElementTest : Spek({
    group("given no user inputs") {
        model {
            test("counter should be zero") {
                count shouldBe 0
            }

            test("the reset and minus buttons should be disabled") {
                resetBtEnabled shouldBe false
                minusBtEnabled shouldBe false
            }
        }
    }

    group("given user clicked plus 3 times") {
        model(Msg.PlusClick, Msg.PlusClick, Msg.PlusClick) {
            test("counter should be 3") {
                count shouldBe 3
            }

            test("the reset and minus buttons should be enabled") {
                resetBtEnabled shouldBe true
                minusBtEnabled shouldBe true
            }
        }
    }

    group("given user clicked plus 3 times and minus 2 times") {
        model(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.MinusClick, Msg.MinusClick
        ) {
            test("counter should be 1") {
                count shouldBe 1
            }
        }
    }

    group("given user clicked plus 3 times and reset one time") {
        model(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.ResetClick
        ) {
            test("counter should be 0") {
                count shouldBe 0
            }

            test("the reset and minus buttons should be disabled") {
                resetBtEnabled shouldBe false
                minusBtEnabled shouldBe false
            }
        }
    }
})

private fun model(vararg msgs: Msg, testF: Model.() -> Unit) =
    CounterElement.test(CounterElement.initModel())
        .apply {
            if (msgs.isNotEmpty()) {
                step(*msgs)
            }
        }
        .let {
            it.history.last { it.modelPrime != null }.modelPrime!!
        }
        .let(testF::invoke)
