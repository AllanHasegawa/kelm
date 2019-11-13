import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.shouldBe
import kelm.ExternalException
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import org.spekframework.spek2.Spek

/**
 * Given we have multiples contexts (A and B), each one with its own Model, Msg and Cmds,
 * then `UpdateContext.switchContext` should help separating them.
 *
 * In this test `ModelA` will increment until it reaches 50, at which point it`ll reset
 * and send a `Cmd` to switch to context B.
 */

object AElement : Kelm.Element<AElement.Model, AElement.Msg, AElement.Cmd, Nothing>() {
    data class Model(val count: Int)

    sealed class Msg {
        object Inc : Msg()
        object GoToB : Msg()
    }

    sealed class Cmd : kelm.Cmd() {
        object GoToB : Cmd()
    }

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        when (msg) {
            is Msg.Inc -> model.copy(count = model.count + 1)
            is Msg.GoToB -> model.copy(count = 0).also { +Cmd.GoToB }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalException): Msg? = null
}

object BElement : Kelm.Sandbox<BElement.Model, BElement.Msg>() {
    data class Model(val count: Int)

    data class Msg(val dec: Int)

    override fun updateSimple(model: Model, msg: Msg): Model? =
        model.copy(count = model.count - msg.dec)
}

object ParentElement :
    Kelm.Element<ParentElement.Model, ParentElement.Msg, ParentElement.Cmd, Nothing>() {
    sealed class Model {
        data class A(val value: AElement.Model) : Model()
        data class B(val value: BElement.Model) : Model()
    }

    sealed class Msg {
        data class ForA(val msg: AElement.Msg) : Msg()
        data class ForB(val msg: BElement.Msg) : Msg()

        object GoToB : Msg()
    }

    object Cmd : kelm.Cmd()

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(
        model: Model,
        msg: Msg
    ): Model? =
        when (model) {
            is Model.A ->
                when (msg) {
                    is Msg.ForA ->
                        switchContext(
                            otherElement = AElement,
                            otherModel = model.value,
                            otherMsg = msg.msg,
                            otherCmdToMsgOrCmd = { otherCmd ->
                                when (otherCmd) {
                                    is AElement.Cmd.GoToB -> Msg.GoToB.ret()
                                }
                            },
                            otherSubToSub = { it }
                        )?.let { model.copy(value = it) }
                    is Msg.GoToB -> Model.B(BElement.Model(0))
                    else -> null
                }
            is Model.B ->
                when (msg) {
                    is Msg.ForB ->
                        switchContext(
                            otherElement = BElement,
                            otherModel = model.value,
                            otherMsg = msg.msg,
                            otherCmdToMsgOrCmd = { error("BElement has no CMD") },
                            otherSubToSub = { it }
                        )?.let { model.copy(value = it) }
                    else -> null
                }
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit

    override fun errorToMsg(error: ExternalException): Msg? = null
}

object KelmContextSwitchTest : Spek({
    group("Given a model with two contexts") {
        val model = AElement.Model(count = 0).let(ParentElement.Model::A)

        test("whenever a msg comes for A, update the corresponding model") {
            val (newModel, cmdsStarted) = ParentElement
                .test(model)
                .step(
                    ParentElement.Msg.ForA(AElement.Msg.Inc),
                    ParentElement.Msg.ForA(AElement.Msg.Inc),
                    ParentElement.Msg.ForA(AElement.Msg.Inc)
                )
                .let { it.modelPrime!! to it.cmdsStarted }

            (newModel as ParentElement.Model.A).value shouldBe AElement.Model(3)
            cmdsStarted.shouldBeEmpty()
        }

        test("whenever A tries to switch to B, then the CMD should be sent") {
            val te = ParentElement.test(model)

            val (newModel, cmdsStarted) =
                te.step(ParentElement.Msg.ForA(AElement.Msg.GoToB))
                    .let { it.modelPrime!! to it.cmdsStarted }

            (newModel as ParentElement.Model.B).value shouldBe BElement.Model(0)
            cmdsStarted.shouldBeEmpty()

            val newBModel = te.step(ParentElement.Msg.ForB(BElement.Msg(3)))
                .let { it.modelPrime!! }

            (newBModel as ParentElement.Model.B).value shouldBe BElement.Model(-3)
        }
    }
})
