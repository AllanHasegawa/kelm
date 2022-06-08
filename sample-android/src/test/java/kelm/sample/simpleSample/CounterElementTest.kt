package kelm.sample.simpleSample

import io.kotlintest.shouldBe
import kelm.sample.simpleSample.CounterElement
import kelm.sample.simpleSample.CounterElement.Msg
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("For CounterElement")
class CounterElementTest {
    @Nested
    @DisplayName("given no user inputs")
    inner class NoInputs {
        private val model = model(null)

        @Test
        fun `count should be zero`() {
            model.count shouldBe 0
        }

        @Test
        fun `the reset and minus buttons should be disabled`() {
            model.resetBtEnabled shouldBe false
            model.minusBtEnabled shouldBe false
        }
    }

    @Nested
    @DisplayName("given user clicked plus 3 times")
    inner class ThreePluses {
        private val model = model(Msg.PlusClick, Msg.PlusClick, Msg.PlusClick)

        @Test
        fun `count should be 3`() {
            model.count shouldBe 3
        }

        @Test
        fun `the reset and minus buttons should be enabled`() {
            model.resetBtEnabled shouldBe true
            model.minusBtEnabled shouldBe true
        }
    }

    @Nested
    @DisplayName("given user clicked plus 3 times, minus 2 times")
    inner class ThreePluses2Minus {
        private val model = model(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.MinusClick, Msg.MinusClick,
        )

        @Test
        fun `count should be 1`() {
            model.count shouldBe 1
        }

        @Test
        fun `the reset and minus buttons should be enabled`() {
            model.resetBtEnabled shouldBe true
            model.minusBtEnabled shouldBe true
        }
    }

    @Nested
    @DisplayName("given user clicked plus 3 times, then reset")
    inner class ThreePlusesThenReset {
        private val model = model(
            Msg.PlusClick, Msg.PlusClick, Msg.PlusClick,
            Msg.ResetClick
        )

        @Test
        fun `count should be 0`() {
            model.count shouldBe 0
        }

        @Test
        fun `the reset and minus buttons should be disabled`() {
            model.resetBtEnabled shouldBe false
            model.minusBtEnabled shouldBe false
        }
    }
}

private fun model(vararg msgs: Msg?) =
    CounterElement.runUpdateManually(CounterElement.initModel(), *msgs)
        .last().modelNonNull
