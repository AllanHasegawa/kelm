package kelm

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import java.util.UUID

typealias UpdateF<ModelT, MsgT, CmdT> =
        UpdateContext<ModelT, MsgT, CmdT>.(ModelT, MsgT) -> ModelT?

abstract class Cmd(open val id: String = randomUuid()) {
    companion object {
        fun randomUuid(): String = UUID.randomUUID().toString()
    }
}

abstract class Sub(open val id: String) {
    companion object
}

class UpdateContext<ModelT, MsgT, CmdT : Cmd> internal constructor() {
    private val cmdOps = mutableListOf<CmdOp<CmdT>>()

    internal fun execute(
        f: UpdateF<ModelT, MsgT, CmdT>,
        model: ModelT,
        msg: MsgT
    ): UpdatePrime<ModelT, CmdT> {
        clear()
        val modelPrime = f(this, model, msg)
        return UpdatePrime(model, modelPrime, getCmdOps())
    }

    operator fun CmdT.unaryPlus() {
        runCmd(this)
    }

    operator fun String.unaryMinus() {
        cancelCmd(this)
    }

    fun cancelCmd(cmdId: String) {
        cmdOps.add(CmdOp.Cancel(cmdId))
    }

    fun runCmd(cmd: CmdT) {
        cmdOps.add(CmdOp.Run(cmd))
    }

    fun <OtherModelT, OtherMsgT, OtherCmdT : Cmd> switchContext(
        model: OtherModelT,
        msg: OtherMsgT,
        otherCmdToCmd: (OtherCmdT) -> CmdT? = { null },
        update: UpdateF<OtherModelT, OtherMsgT, OtherCmdT>
    ): OtherModelT? {
        val context = UpdateContext<OtherMsgT, OtherCmdT>()
        val modelPrime = update(context, model, msg)
        val cmds = context.cmdOps.mapNotNull { otherCmdOp ->
            when (otherCmdOp) {
                is CmdOp.Run -> {
                    val newCmd = otherCmdToCmd(otherCmdOp.cmd)
                        ?: return@mapNotNull null
                    CmdOp.Run(newCmd)
                }
                else -> otherCmdOp
            } as CmdOp<CmdT>
        }
        cmdOps.addAll(cmds)
        return modelPrime
    }

    private fun getCmdOps() = CmdOp.MultiOps(cmdOps.toList())

    private fun clear() {
        cmdOps.clear()
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

    fun runSub(sub: SubT) {
        subs += sub
    }
}

data class Step<ModelT, MsgT, CmdT : Cmd>(
    val model: ModelT,
    val msg: MsgT,
    val modelPrime: ModelT?,
    val cmdsStarted: List<CmdT> = emptyList(),
    val cmdIdsCancelled: List<String> = emptyList()
)

object Kelm {
    @Suppress("UNCHECKED_CAST")
    fun <ModelT, MsgT, CmdT : Cmd, SubT : Sub> build(
        initModel: ModelT,
        msgInput: Observable<MsgT>,
        initCmd: CmdT? = null,
        subscriptions: SubContext<SubT>.(model: ModelT) -> Unit = { _ -> },
        cmdToMaybe: (cmd: CmdT) -> Maybe<MsgT> = { Maybe.error(CmdFactoryNotImplementedException()) },
        subToObservable: (
            sub: SubT,
            msgObs: Observable<MsgT>,
            modelObs: Observable<ModelT>
        ) -> Observable<MsgT> = { _, _, _ -> Observable.error(SubFactoryNotImplementedException()) },
        errorToMsg: (error: ExternalError) -> MsgT? = { null },
        updateWatcher: (model: ModelT, msg: MsgT, modelPrime: ModelT?) -> Disposable? = { _, _, _ -> null },
        update: UpdateF<ModelT, MsgT, CmdT>
    ): Observable<ModelT> =
        Observable
            .defer {
                val errorSubj = PublishSubject.create<MsgT>()
                val modelSubj =
                    BehaviorSubject
                        .createDefault<Optional<ModelT>>(initModel.toOptional())
                        .toSerialized()
                val msgSubj = ReplaySubject.create<MsgT>().toSerialized()

                val cmdDisposables = mutableMapOf<String, Disposable>()
                val subDisposables = mutableMapOf<String, Disposable>()

                val watcherDisposables = mutableListOf<Disposable>()

                val initCmdOp = when (initCmd) {
                    null -> CmdOp.NoOp as CmdOp<CmdT>
                    else -> CmdOp.Run(initCmd)
                }

                val updateContext = UpdateContext<MsgT, CmdT>()
                val subContext = SubContext<SubT>()

                msgInput
                    .serialize()
                    .mergeWith(msgSubj)
                    .mergeWith(errorSubj)
                    .scan(
                        UpdatePrime(initModel, initModel, initCmdOp)
                    ) { acc, msg ->
                        val currentModel = acc.modelPrime ?: acc.model
                        updateContext.execute(update, currentModel, msg)
                            .also { accPrime ->
                                val maybeDisposable =
                                    updateWatcher(accPrime.model, msg, accPrime.modelPrime)
                                if (maybeDisposable != null) {
                                    watcherDisposables.add(maybeDisposable)
                                }
                                watcherDisposables.removeAll { it.isDisposed }
                            }
                    }
                    .doOnNext { (modelPrime, _) -> modelSubj.onNext(modelPrime.toOptional()) }
                    .doOnNext { updatePrime ->
                        fun processCmdOp(cmdOp: CmdOp<CmdT>) {
                            when (cmdOp) {
                                is CmdOp.NoOp -> Unit
                                is CmdOp.MultiOps -> cmdOp.ops.forEach(::processCmdOp)
                                is CmdOp.Cancel -> cmdDisposables[cmdOp.cmdId]?.dispose()
                                is CmdOp.Run -> {
                                    cmdDisposables[cmdOp.cmd.id]?.dispose()
                                    cmdDisposables[cmdOp.cmd.id] =
                                        cmdToMaybe(cmdOp.cmd)
                                            .doOnSuccess(msgSubj::onNext)
                                            .doOnError { error: Throwable ->
                                                CmdError(cmdOp.cmd as Any, error)
                                                    .let { cmdError ->
                                                        when (val msg = errorToMsg(cmdError)) {
                                                            null -> errorSubj.onError(cmdError)
                                                            else -> msgSubj.onNext(msg)
                                                        }
                                                    }
                                            }
                                            .subscribe({}, {})
                                }
                            }
                        }

                        synchronized(cmdDisposables) {
                            processCmdOp(updatePrime.cmdOp)
                        }
                    }
                    .doOnNext { _ ->
                        synchronized(cmdDisposables) {
                            cmdDisposables
                                .toMap()
                                .filter { it.value.isDisposed }
                                .map { it.key }
                                .forEach { cmdDisposables.remove(it) }
                        }
                    }
                    .filter { it.modelPrime != null }
                    .map { it.modelPrime!! }
                    .scan(SubsState<ModelT, SubT>()) { subsState, model ->
                        val subsPrime = subContext.execute(
                            f = subscriptions,
                            model = model
                        )

                        SubsState(model, subs = subsState.subsPrime, subsPrime = subsPrime)
                    }
                    .doOnNext { (_, subs, subsPrime) ->
                        val subsDiffs = computeSubsDiff(old = subs, new = subsPrime)
                        val msgObs = msgInput.mergeWith(msgSubj).hide()
                        val modelObs = modelSubj.hide()
                            .filter { it is Some<*> }
                            .map { it.toNullable()!! }

                        subsDiffs.forEach { diff ->
                            when (diff) {
                                is SubsDiffOp.Create ->
                                    subToObservable(diff.sub, msgObs, modelObs)
                                        .doOnNext(msgSubj::onNext)
                                        .doOnError { error ->
                                            SubscriptionError(diff.sub as Any, error)
                                                .let { subError ->
                                                    when (val msg = errorToMsg(subError)) {
                                                        null -> errorSubj.onError(subError)
                                                        else -> msgSubj.onNext(msg)
                                                    }
                                                }
                                        }
                                        .subscribe({}, {}, {})
                                        .let { disposable ->
                                            synchronized(subDisposables) {
                                                subDisposables[diff.sub.id] = disposable
                                            }
                                        }
                                is SubsDiffOp.Dispose -> {
                                    synchronized(subDisposables) {
                                        subDisposables[diff.sub.id]?.dispose()
                                        subDisposables.remove(diff.sub.id)
                                    }
                                }
                            }
                        }
                    }
                    .skip(1)
                    .map { it.model!! }
                    .doOnDispose {
                        cmdDisposables.values.forEach(Disposable::dispose)
                        subDisposables.values.forEach(Disposable::dispose)
                        watcherDisposables.forEach(Disposable::dispose)
                    }
            }

    fun <ModelT, MsgT, CmdT : Cmd> test(
        update: UpdateF<ModelT, MsgT, CmdT>,
        initModel: ModelT,
        msgs: List<MsgT>
    ): List<Step<ModelT, MsgT, CmdT>> {
        val context = UpdateContext<MsgT, CmdT>()
        return Observable.fromIterable(msgs)
            .scan(emptyList<Step<ModelT, MsgT, CmdT>>()) { acc, msg ->
                val model =
                    when (acc.isEmpty()) {
                        true -> initModel
                        false -> acc.last().run { modelPrime ?: model }
                    }
                val updatePrime = context.execute(update, model, msg)
                val modelPrime = updatePrime.modelPrime
                val cmdOps = (updatePrime.cmdOp as CmdOp.MultiOps<CmdT>).ops
                val cmdsStarted = cmdOps
                    .mapNotNull { it as? CmdOp.Run<CmdT> }
                    .map { it.cmd }
                val cmdsCancelled = cmdOps
                    .mapNotNull { it as? CmdOp.Cancel<CmdT> }
                    .map { it.cmdId }
                val step = Step(
                    model = model,
                    msg = msg,
                    modelPrime = modelPrime,
                    cmdsStarted = cmdsStarted,
                    cmdIdsCancelled = cmdsCancelled
                )
                acc + step
            }
            .blockingLast()
            .toList()
    }
}

internal sealed class CmdOp<CmdT> {
    object NoOp : CmdOp<Nothing>()
    data class Run<CmdT>(val cmd: CmdT) : CmdOp<CmdT>()
    data class Cancel<CmdT>(val cmdId: String) : CmdOp<CmdT>()
    data class MultiOps<CmdT>(val ops: List<CmdOp<CmdT>>) : CmdOp<CmdT>() {
        init {
            require(ops.firstOrNull { it is MultiOps || it is NoOp } == null) {
                "CmdOp::MultiOps must not have any CmdOp::MultiOps or CmdOp::NoOp inside"
            }
        }
    }
}

internal data class UpdatePrime<ModelT, CmdT>(
    val model: ModelT,
    val modelPrime: ModelT?,
    val cmdOp: CmdOp<CmdT>
)

private data class SubsState<ModelT, SubT>(
    val model: ModelT? = null,
    val subs: List<SubT> = emptyList(),
    val subsPrime: List<SubT> = emptyList()
)

private sealed class SubsDiffOp<SubT> {
    data class Create<SubT>(val sub: SubT) : SubsDiffOp<SubT>()
    data class Dispose<SubT>(val sub: SubT) : SubsDiffOp<SubT>()
}

private fun <SubT : Sub> computeSubsDiff(old: List<SubT>, new: List<SubT>): List<SubsDiffOp<SubT>> {
    val oldIdx = old.map { it.id to it }.toMap()
    val newIdx = new.map { it.id to it }.toMap()

    val toCreateIds = newIdx.keys - oldIdx.keys
    val toDisposeIds = oldIdx.keys - newIdx.keys

    val toCreate: List<SubsDiffOp<SubT>> =
        toCreateIds
            .map { id -> newIdx.getValue(id) }
            .map { SubsDiffOp.Create(it) }

    val toDispose: List<SubsDiffOp<SubT>> =
        toDisposeIds
            .map { id -> oldIdx.getValue(id) }
            .map { SubsDiffOp.Dispose(it) }

    return toDispose + toCreate
}
