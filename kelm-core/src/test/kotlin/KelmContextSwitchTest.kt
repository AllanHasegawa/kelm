import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.shouldBe
import kelm.ExternalError
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

    data class Msg(val inc: Int)

    sealed class Cmd : kelm.Cmd() {
        object GoToB : Cmd()
    }

    override fun UpdateContext<Model, Msg, Cmd, Nothing>.update(model: Model, msg: Msg): Model? =
        model.copy(count = model.count + msg.inc)
            .let {
                when (it.count) {
                    in 0..50 -> it
                    else -> {
                        +Cmd.GoToB
                        Model(0)
                    }
                }
            }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit
    override fun errorToMsg(error: ExternalError): Msg? = null
}

object BElement : Kelm.Sandbox<BElement.Model, BElement.Msg>() {
    data class Model(val count: Int)

    data class Msg(val dec: Int)

    override fun updateSimple(model: Model, msg: Msg): Model? =
        model.copy(count = model.count - msg.dec)
}

object ParentElement :
    Kelm.Element<ParentElement.Model, ParentElement.Msg, ParentElement.Cmd, Nothing>() {
    data class Model(
        val a: AElement.Model?,
        val b: BElement.Model?
    )

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
        when (model.a) {
            is AElement.Model ->
                when (msg) {
                    is Msg.ForA ->
                        switchContext(
                            otherElement = AElement,
                            otherModel = model.a,
                            otherMsg = msg.msg,
                            otherCmdToMsgOrCmd = { otherCmd ->
                                when (otherCmd) {
                                    is AElement.Cmd.GoToB -> Msg.GoToB.ret()
                                }
                            },
                            otherSubToSub = { it }
                        )?.let { model.copy(a = it) }
                    is Msg.GoToB ->
                        Model(
                            a = null,
                            b = BElement.Model(0)
                        )
                    else -> null
                }
            null ->
                when (msg) {
                    is Msg.ForB ->
                        switchContext(
                            otherElement = BElement,
                            otherModel = model.b!!,
                            otherMsg = msg.msg,
                            otherCmdToMsgOrCmd = { error("BElement has no CMD") },
                            otherSubToSub = { it }
                        )?.let { model.copy(b = it) }
                    else -> null
                }
            else -> null
        }

    override fun SubContext<Nothing>.subscriptions(model: Model) = Unit

    override fun errorToMsg(error: ExternalError): Msg? = null

//    override fun updateSimple(model: Model, msg: Msg): Model? =
//        when (msg) {
//            is Msg.ForA ->
//                model.copy(
//                    a = switchContext<ModelA, MsgA, CmdA>(
//                        model = model.a,
//                        msg = msg.msg,
//                        otherCmdToCmd = { cmdA ->
//                            when (cmdA) {
//                                is CmdA.GoToB -> Cmd.GoToB
//                            }
//                        }
//                    ) { model, msg ->
//                        updateA(model, msg)
//                    }
//                )
//            else -> null
//        }
//        else ->
//        when (msg) {
//            is Msg.ForB ->
//                model.copy(
//                    b = switchContext<ModelB, MsgB, Nothing>(
//                        model.b!!,
//                        msg.msg,
//                        { null }
//                    ) { model, msg -> updateB(model, msg) }
//                )
//            else -> null
//        }
//    }
}


//    private fun steps(vararg msgs: msg) =
//    kelm.test<model, msg, cmd>(
//        update = { model, msg -> update(model, msg) },
//        initmodel = model(a = modela(count = 0), b = null),
//        msgs = msgs.tolist()
//tolist    )

object KelmContextSwitchTest : Spek({
    group("Given a model with two contexts") {
        val model = ParentElement.Model(a = AElement.Model(count = 0), b = null)

        test("whenever a msg comes for A, update the corresponding model") {
            val (newModel, cmdsStarted) = ParentElement
                .test(model)
                .step(
                    ParentElement.Msg.ForA(AElement.Msg(10))
                )
                .let { it.modelPrime !! to it.cmdsStarted }

            newModel.a shouldBe AElement.Model(10)
            cmdsStarted.shouldBeEmpty()
        }

        test("whenever A tries to switch to B, then the CMD should be sent") {
            val (newModel, cmdsStarted) = ParentElement
                .test(model)
                .step(
                    ParentElement.Msg.ForA(AElement.Msg(51))
                )
                .let { it.modelPrime !! to it.cmdsStarted }

            newModel.a shouldBe null
            newModel.b shouldBe BElement.Model(0)
            cmdsStarted.shouldBeEmpty()
        }
    }
})
