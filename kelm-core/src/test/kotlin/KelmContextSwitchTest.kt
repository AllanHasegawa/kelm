import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.shouldBe
import kelm.Kelm
import kelm.UpdateContext
import org.spekframework.spek2.Spek

/**
 * Given we have multiples contexts (A and B), each one with its own Model, Msg and Cmds,
 * then `UpdateContext.switchContext` should help separating them.
 *
 * In this test `ModelA` will increment until it reaches 50, at which point it`ll reset
 * and send a `Cmd` to switch to context B.
 */

private data class ModelA(val count: Int)

private data class ModelB(val count: Int)

private data class Model(
    val a: ModelA?,
    val b: ModelB?
)

private data class MsgA(val inc: Int)
private data class MsgB(val dec: Int)

private sealed class Msg {
    data class ForA(val msg: MsgA) : Msg()
    data class ForB(val msg: MsgB) : Msg()
}

private sealed class CmdA : kelm.Cmd() {
    object GoToB : CmdA()
}

private sealed class Cmd : kelm.Cmd() {
    object GoToB : Cmd()
}

private fun UpdateContext<CmdA>.updateA(model: ModelA, msg: MsgA): ModelA? =
    model.copy(count = model.count + msg.inc)
        .let {
            when (it.count) {
                in 0..50 -> it
                else -> {
                    runCmd(CmdA.GoToB)
                    ModelA(0)
                }
            }
        }

private fun updateB(model: ModelB, msg: MsgB): ModelB? =
    model.copy(count = model.count - msg.dec)

private fun UpdateContext<Cmd>.update(model: Model, msg: Msg): Model? =
    when (model.a) {
        is ModelA ->
            when (msg) {
                is Msg.ForA ->
                    model.copy(
                        a = switchContext<ModelA, MsgA, CmdA>(
                            model = model.a,
                            msg = msg.msg,
                            otherCmdToCmd = { cmdA ->
                                when (cmdA) {
                                    is CmdA.GoToB -> Cmd.GoToB
                                }
                            }
                        ) { model, msg ->
                            updateA(model, msg)
                        }
                    )
                else -> null
            }
        else ->
            when (msg) {
                is Msg.ForB ->
                    model.copy(
                        b = switchContext<ModelB, MsgB, Nothing>(
                            model.b!!,
                            msg.msg,
                            { null }
                        ) { model, msg -> updateB(model, msg) }
                    )
                else -> null
            }
    }

private fun steps(vararg msgs: Msg) =
    Kelm.test<Model, Msg, Cmd>(
        update = { model, msg -> update(model, msg) },
        initModel = Model(a = ModelA(count = 0), b = null),
        msgs = msgs.toList()
    )

object KelmContextSwitchTest : Spek({
    group("Given a model with two contexts") {
        test("whenever a msg comes for A, update the corresponding model") {
            val (model, cmds) = steps(
                Msg.ForA(MsgA(10))
            ).last().let { it.modelPrime!! to it.cmdsStarted }

            model.a shouldBe ModelA(10)
            cmds.shouldBeEmpty()
        }

        test("whenever A tries to switch to B, then the CMD should be sent") {
            val (model, cmds) = steps(
                Msg.ForA(MsgA(51))
            ).last().let { it.modelPrime!! to it.cmdsStarted }

            model.a shouldBe ModelA(0)
            cmds shouldContain Cmd.GoToB
        }
    }
})
