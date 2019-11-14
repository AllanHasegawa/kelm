package kelm.sample

import io.kotlintest.shouldBe
import kelm.sample.CounterElement.Msg
import org.spekframework.spek2.Spek

object CounterElementTest : Spek({
    group("given no user inputs") {
        val msgs = msgs()

        test("counter should be zero") {
            model(msgs).count shouldBe 0
        }

        test("the reset and minus buttons should be disabled") {
            model(msgs).apply {
                resetBtEnabled shouldBe false
                minusBtEnabled shouldBe false
            }
        }
    }

    group("given user clicked plus 3 times") {
        val msgs = msgs(Msg.PlusClick, Msg.PlusClick, Msg.PlusClick)

        test("counter should be 3") {
            model(msgs).count shouldBe 3
        }

        test("the reset and minus buttons should be enabled") {
            model(msgs).apply {
                resetBtEnabled shouldBe true
                minusBtEnabled shouldBe true
            }
        }
    }

    group("given user clicked plus 3 times and minus 2 times") {
        val msgs = msgs(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.MinusClick, Msg.MinusClick
        )

        test("counter should be 1") {
            model(msgs).count shouldBe 1
        }
    }

    group("given user clicked plus 3 times and reset one time") {
        val msgs = msgs(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.ResetClick
        )

        test("counter should be 0") {
            model(msgs).count shouldBe 0
        }

        test("the reset and minus buttons should be disabled") {
            model(msgs).apply {
                resetBtEnabled shouldBe false
                minusBtEnabled shouldBe false
            }
        }
    }
})

private fun msgs(vararg msgs: Msg) = msgs.toList()

private fun model(msgs: List<Msg>) =
    CounterElement.test(CounterElement.initModel())
        .apply {
            if (msgs.isNotEmpty()) {
                step(*msgs.toTypedArray())
            }
        }
        .let {
            it.history.last { it.modelPrime != null }.modelPrime!!
        }
