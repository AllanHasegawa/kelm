import UpdateRetNullNoEffectEle.Model
import UpdateRetNullNoEffectEle.Msg
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import kelm.Log
import kelm.DispatcherProvider
import kelm.Sandbox
import kelm.UpdateStep
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal object UpdateRetNullNoEffectEle : Sandbox<Model, Msg>() {
    sealed class Model {
        object NotReady : Model()
        object Ready : Model()
    }

    sealed class Msg {
        object Loaded : Msg()
    }

    fun initModel() = Model.NotReady

    override fun updateSimple(model: Model, msg: Msg): Model? =
        when (model) {
            is Model.NotReady ->
                when (msg) {
                    is Msg.Loaded -> Model.Ready
                }
            is Model.Ready ->
                when (msg) {
                    is Msg.Loaded -> null
                }
        }
}

@DisplayName("Whenever the update function returns null, no model should be emitted")
internal class UpdateRetNullNoEffectTest {

    internal lateinit var updates: List<UpdateStep<Model, Msg, Nothing, Nothing>>

    @DisplayName("Given Model was NotReady and Loaded Twice")
    @Nested
    inner class GivenModelWasNotReadyAndLoadedTwice {
        @BeforeAll
        fun setUp() {
            updates = UpdateRetNullNoEffectEle.runUpdateManually(
                UpdateRetNullNoEffectEle.initModel(),
                null, Msg.Loaded, Msg.Loaded
            )
        }

        @Test
        fun `updates should have count of three`() {
            updates.count() shouldBeExactly 3
        }

        @Test
        fun `first update should return modelPrime as null`() {
            updates.first().model shouldBe Model.NotReady
            updates.first().modelPrime.shouldBeNull()
        }

        @Test
        fun `second update should return modelPrime as Model_Ready`() {
            updates[1].modelPrime shouldBe Model.Ready
        }

        @Test
        fun `third update should return modelPrime as null`() {
            updates[2].modelPrime.shouldBeNull()
        }

        @Test
        fun `one modelPrimes should've been emitted`() {
            updates.count { it.modelPrime != null } shouldBeExactly 1
        }
    }
}
