import UpdateRetNullNoEffectEle.Model
import UpdateRetNullNoEffectEle.Msg
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import kelm.Kelm
import org.spekframework.spek2.Spek

private object UpdateRetNullNoEffectEle : Kelm.Sandbox<Model, Msg>() {
    sealed class Model {
        object NotReady : Model()
        object Ready : Model()
    }

    sealed class Msg {
        object Loaded : Msg()
    }

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

object UpdateRetNullNoEffectTest : Spek({
    group("given model not ready and loaded twice") {
        steps(Msg.Loaded, Msg.Loaded).run {
            test("three steps should have happened") {
                println(this)
                count() shouldBeExactly 3
            }
            test("first step's model should be init model") {
                first().modelPrime shouldBe Model.NotReady
            }
            test("second step's model should be ready state") {
                getOrNull(1)!!.modelPrime shouldBe Model.Ready
            }
            test("third step's model should be null") {
                getOrNull(2)!!.modelPrime.shouldBeNull()
            }
        }
    }
})

private fun steps(vararg msgs: Msg) =
    UpdateRetNullNoEffectEle
        .test(Model.NotReady)
        .apply { step(*msgs) }
        .history
