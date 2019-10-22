package kelm

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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

class UpdateContext<ModelT, MsgT, CmdT : Cmd, SubT : Sub> internal constructor() {
    private val cmdOps = mutableListOf<CmdOp<CmdT>>()
    private val msgsFromOtherContext = mutableListOf<MsgT>()
    private val subsFromOtherContext = mutableListOf<SubT>()

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
        otherCmdToCmd: (OtherCmdT) -> CmdT,
        otherSubToSub: (OtherSubT) -> SubT,
        bypassOtherCmdToMsg: (OtherCmdT) -> MsgT? = { null }
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
                    val msgMaybe = bypassOtherCmdToMsg(otherCmdOp.cmd)
                    if (msgMaybe != null) {
                        msgsFromOtherContext.add(msgMaybe)
                        return@mapNotNull null
                    }
                    val newCmd = otherCmdToCmd(otherCmdOp.cmd)
                    CmdOp.Start(newCmd)
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

        final override fun errorToMsg(error: ExternalError): MsgT? = null
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
        abstract fun errorToMsg(error: ExternalError): MsgT?

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
    }
}
