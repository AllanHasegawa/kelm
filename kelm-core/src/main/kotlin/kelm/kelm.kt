package kelm

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kelm.internal.CmdOp
import kelm.internal.UpdatePrime
import kelm.internal.build
import java.util.UUID

typealias UpdateF<ModelT, MsgT, CmdT, SubT> =
    UpdateContext<ModelT, MsgT, CmdT, SubT>.(ModelT, MsgT) -> ModelT?

typealias LoggerF<ModelT, MsgT, CmdT, SubT> =
        (Log<ModelT, MsgT, CmdT, SubT>) -> Disposable?

abstract class Cmd(open val id: String = randomUuid()) {
    companion object {
        fun randomUuid(): String = UUID.randomUUID().toString()
    }
}

abstract class Sub(open val id: String) {
    companion object
}

sealed class MsgOrCmd<out MsgT, out CmdT : Cmd> {
    data class Msg<out MsgT>(val value: MsgT) : MsgOrCmd<MsgT, Nothing>()
    data class Cmd<out CmdT : kelm.Cmd>(val value: CmdT) : MsgOrCmd<Nothing, CmdT>()
}

class UpdateContext<ModelT, MsgT, CmdT : Cmd, SubT : Sub> internal constructor() {
    companion object {
        internal val msgOrCmdContext = MsgOrCmdContext<Any, Cmd>()
    }

    private val cmdOps = mutableListOf<CmdOp<CmdT>>()
    private val msgsFromOtherContext = mutableListOf<MsgT>()
    private val subsFromOtherContext = mutableListOf<SubT>()

    class MsgOrCmdContext<MsgT, CmdT : Cmd> {
        fun MsgT.ret() = MsgOrCmd.Msg(this)
        fun CmdT.ret() = MsgOrCmd.Cmd(this)
    }

    internal fun execute(
        f: UpdateF<ModelT, MsgT, CmdT, SubT>,
        model: ModelT,
        msg: MsgT
    ): UpdatePrime<ModelT, MsgT, CmdT, SubT> {
        clear()
        val modelPrime = f(this, model, msg)
        return UpdatePrime(
            model = model,
            msg = msg,
            modelPrime = modelPrime,
            cmdOps = cmdOps.toList(),
            msgsFromOtherContext = msgsFromOtherContext.toList(),
            subsFromOtherContext = subsFromOtherContext.toList()
        )
    }

    operator fun CmdT.unaryPlus() {
        startCmd(this)
    }

    operator fun String.unaryMinus() {
        cancelCmd(this)
    }

    operator fun List<CmdT>.unaryPlus() {
        startCmds(this)
    }

    fun cancelCmd(cmdId: String) {
        cmdOps.add(CmdOp.Cancel(cmdId))
    }

    fun startCmd(cmd: CmdT) {
        cmdOps.add(CmdOp.Start(cmd))
    }

    fun startCmds(cmds: List<CmdT>) {
        cmds.map { CmdOp.Start(it) }
            .let(cmdOps::addAll)
    }

    fun startCmds(vararg cmds: CmdT) {
        startCmds(cmds.toList())
    }

    fun <OtherModelT, OtherMsgT, OtherCmdT : Cmd, OtherSubT : Sub> switchContext(
        otherElement: Kelm.Element<OtherModelT, OtherMsgT, OtherCmdT, OtherSubT>,
        otherModel: OtherModelT,
        otherMsg: OtherMsgT,
        otherCmdToMsgOrCmd: MsgOrCmdContext<MsgT, CmdT>.(OtherCmdT) -> MsgOrCmd<MsgT, CmdT>,
        otherSubToSub: (OtherSubT) -> SubT
    ): OtherModelT? {
        val otherUpdateContext = UpdateContext<OtherModelT, OtherMsgT, OtherCmdT, OtherSubT>()
        val otherUpdatePrime = with(otherElement) {
            with(otherUpdateContext) {
                execute({ model, msg -> update(model, msg) }, otherModel, otherMsg)
            }
        }
        @Suppress("UNCHECKED_CAST")
        val cmds = otherUpdateContext.cmdOps.mapNotNull { otherCmdOp ->
            when (otherCmdOp) {
                is CmdOp.Start -> {
                    val msgOrCmd = otherCmdToMsgOrCmd(
                        msgOrCmdContext as MsgOrCmdContext<MsgT, CmdT>,
                        otherCmdOp.cmd
                    )
                    when (msgOrCmd) {
                        is MsgOrCmd.Msg -> {
                            msgsFromOtherContext.add(msgOrCmd.value)
                            return@mapNotNull null
                        }
                        is MsgOrCmd.Cmd -> CmdOp.Start(msgOrCmd.value)
                    }
                }
                else -> otherCmdOp
            } as CmdOp<CmdT>
        }
        cmdOps.addAll(cmds)

        val otherSubContext = SubContext<OtherSubT>()
        val otherSubs = with(otherElement) {
            with(otherSubContext) {
                execute({ model -> subscriptions(model) }, otherModel)
            }
        }.map(otherSubToSub)
        subsFromOtherContext.addAll(otherSubs)

        return otherUpdatePrime.modelPrime ?: otherUpdatePrime.model
    }

    private fun clear() {
        cmdOps.clear()
        subsFromOtherContext.clear()
        msgsFromOtherContext.clear()
    }
}

class SubContext<SubT : Sub> internal constructor() {

    private val subs = mutableListOf<SubT>()

    internal fun <ModelT> execute(
        f: SubContext<SubT>.(ModelT) -> Unit,
        model: ModelT
    ): List<SubT> {
        subs.clear()
        f(this, model)

        return subs.toList()
    }

    fun retain(sub: SubT) {
        subs += sub
    }

    fun retain(subs: List<SubT>) {
        subs.forEach(::retain)
    }

    fun retain(vararg subs: SubT) {
        retain(subs.toList())
    }

    operator fun SubT.unaryPlus() {
        retain(this)
    }

    operator fun List<SubT>.unaryPlus() {
        retain(this)
    }

    fun SubT.retainSub() {
        retain(this)
    }

    fun List<SubT>.retainSubs() {
        map(::retain)
    }
}

sealed class Log<ModelT, MsgT, CmdT : Cmd, SubT : Sub> {
    abstract val index: Int

    data class Update<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val model: ModelT,
        val msg: MsgT?,
        val modelPrime: ModelT?,
        val cmdsStarted: List<CmdT> = emptyList(),
        val cmdIdsCancelled: List<String> = emptyList(),
        val subs: List<SubT> = emptyList()
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class SubscriptionStarted<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val sub: Sub
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class SubscriptionCancelled<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val sub: Sub
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class SubscriptionError<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val sub: Sub,
        val error: Throwable
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class SubscriptionEmission<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val sub: SubT,
        val msg: MsgT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class CmdStarted<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val cmd: Cmd
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class CmdCancelled<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val cmdId: String
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class CmdEmission<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val cmd: Cmd,
        val msg: MsgT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class CmdError<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val cmd: Cmd,
        val error: Throwable
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    data class CmdIdNotFoundToCancel<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
        override val index: Int,
        val cmdId: String
    ) : Log<ModelT, MsgT, CmdT, SubT>()
}

class TestEnvironment<ModelT, MsgT, CmdT : Cmd, SubT : Sub>(
    val element: Kelm.Element<ModelT, MsgT, CmdT, SubT>,
    val initModel: ModelT
) {
    val history: List<Log.Update<ModelT, MsgT, CmdT, SubT>>
        get() = steps.toList()

    private val steps = mutableListOf(initStep())
    private val msgSubj = PublishSubject.create<MsgT>()

    fun step(vararg msgs: MsgT): Log.Update<ModelT, MsgT, CmdT, SubT> {
        if (msgs.isEmpty()) return steps.last()

        val logCapturer = mutableListOf<Log.Update<ModelT, MsgT, CmdT, SubT>>()

        val ts = element
            .start(
                initModel = steps.last().let { it.modelPrime ?: it.model },
                msgInput = msgSubj,
                cmdToMaybe = { Maybe.empty() },
                subToObs = { _, _, _ -> Observable.empty() },
                logger = {
                    (it as? Log.Update)?.let(logCapturer::add)
                    null
                }
            )
            .test()

        msgs.forEach(msgSubj::onNext)

        ts.dispose()

        val indexOffset = steps.last().index + 1
        val newSteps = logCapturer.drop(1)
            .mapIndexed { idx, step -> step.copy(index = idx + indexOffset) }

        println("adding new steps: $newSteps")
        steps.addAll(newSteps)
        return steps.last()
    }

    private fun initStep(): Log.Update<ModelT, MsgT, CmdT, SubT> {
        val startCmds = element.initCmds(initModel)
            ?: emptyList()

        return Log.Update(
            index = 0,
            model = initModel,
            msg = null,
            modelPrime = initModel,
            cmdsStarted = startCmds,
            cmdIdsCancelled = emptyList()
        )
    }
}

object Kelm {
    abstract class Sandbox<ModelT, MsgT> : Element<ModelT, MsgT, Nothing, Nothing>() {
        abstract fun updateSimple(model: ModelT, msg: MsgT): ModelT?

        final override fun UpdateContext<ModelT, MsgT, Nothing, Nothing>.update(
            model: ModelT,
            msg: MsgT
        ): ModelT? = updateSimple(model, msg)

        fun start(
            initModel: ModelT,
            msgInput: Observable<MsgT>,
            logger: LoggerF<ModelT, MsgT, Nothing, Nothing> = { null }
        ) =
            start(
                initModel = initModel,
                msgInput = msgInput,
                cmdToMaybe = { Maybe.empty() },
                subToObs = { _, _, _ -> Observable.empty() },
                logger = logger
            )

        final override fun errorToMsg(error: ExternalException): MsgT? = null
        final override fun initCmds(initModel: ModelT): List<Nothing>? = null
        final override fun SubContext<Nothing>.subscriptions(model: ModelT) = Unit
    }

    abstract class Element<ModelT, MsgT, CmdT : Cmd, SubT : Sub> {
        open fun initCmds(initModel: ModelT): List<CmdT>? = null

        abstract fun UpdateContext<ModelT, MsgT, CmdT, SubT>.update(
            model: ModelT,
            msg: MsgT
        ): ModelT?

        abstract fun SubContext<SubT>.subscriptions(model: ModelT)
        abstract fun errorToMsg(error: ExternalException): MsgT?

        fun start(
            initModel: ModelT,
            msgInput: Observable<MsgT>,
            cmdToMaybe: (CmdT) -> Maybe<MsgT>,
            subToObs: (SubT, Observable<MsgT>, Observable<ModelT>) -> Observable<MsgT>,
            logger: LoggerF<ModelT, MsgT, CmdT, SubT> = { null }
        ) =
            build(
                initModel = initModel,
                initCmds = initCmds(initModel),
                msgInput = msgInput,
                cmdToMaybe = cmdToMaybe,
                subToObservable = subToObs,
                errorToMsg = ::errorToMsg,
                subscriptions = { model -> this.subscriptions(model) },
                logger = logger,
                update = { model: ModelT, msg: MsgT -> update(model, msg) }
            )

        fun test(initModel: ModelT) = TestEnvironment(this, initModel)
    }
}
