package kelm.internal

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kelm.Cmd
import kelm.CmdError
import kelm.CmdFactoryNotImplementedException
import kelm.ExternalError
import kelm.Log
import kelm.Optional
import kelm.Some
import kelm.Sub
import kelm.SubContext
import kelm.SubFactoryNotImplementedException
import kelm.SubscriptionError
import kelm.UpdateContext
import kelm.UpdateF
import kelm.toNullable
import kelm.toOptional
import java.util.concurrent.atomic.AtomicInteger

internal sealed class CmdOp<CmdT> {
    data class Run<CmdT>(val cmd: CmdT) : CmdOp<CmdT>()
    data class Cancel<CmdT>(val cmdId: String) : CmdOp<CmdT>()
}

internal data class UpdatePrime<ModelT, MsgT, CmdT, SubT>(
    val model: ModelT,
    val msg: MsgT?,
    val modelPrime: ModelT?,
    val cmdOps: List<CmdOp<CmdT>>,
    val msgsFromOtherContext: List<MsgT>,
    val subsFromOtherContext: List<SubT>
)

private data class SubsState<ModelT, MsgT, CmdT, SubT>(
    val updatePrime: UpdatePrime<ModelT, MsgT, CmdT, SubT>? = null,
    val subs: List<SubT> = emptyList(),
    val subsPrime: List<SubT> = emptyList()
)

private sealed class SubsDiffOp<SubT> {
    data class Create<SubT>(val sub: SubT) : SubsDiffOp<SubT>()
    data class Dispose<SubT>(val sub: SubT) : SubsDiffOp<SubT>()
}

internal fun <ModelT, MsgT, CmdT : Cmd, SubT : Sub> build(
    initModel: ModelT,
    msgInput: Observable<MsgT>,
    initCmds: List<CmdT>? = null,
    subscriptions: SubContext<SubT>.(model: ModelT) -> Unit = { _ -> },
    cmdToMaybe: (cmd: CmdT) -> Maybe<MsgT> = { Maybe.error(CmdFactoryNotImplementedException()) },
    subToObservable: (
        sub: SubT,
        msgObs: Observable<MsgT>,
        modelObs: Observable<ModelT>
    ) -> Observable<MsgT> = { _, _, _ -> Observable.error(SubFactoryNotImplementedException()) },
    errorToMsg: (error: ExternalError) -> MsgT? = { null },
    logger: (Log<ModelT, MsgT, CmdT, SubT>) -> Disposable?,
    update: UpdateF<ModelT, MsgT, CmdT, SubT>
): Observable<ModelT> = Observable.defer {
    val errorSubj = PublishSubject.create<MsgT>()
    val modelSubj =
        BehaviorSubject
            .createDefault<Optional<ModelT>>(initModel.toOptional())
            .toSerialized()
    val msgSubj = ReplaySubject.create<MsgT>().toSerialized()

    val cmdDisposables = mutableMapOf<String, Disposable>()
    val subDisposables = mutableMapOf<String, Disposable>()

    val loggerDisposables = mutableListOf<Disposable>()
    fun Log<ModelT, MsgT, CmdT, SubT>.log() = synchronized(loggerDisposables) {
        val disposable = logger(this)
        if (disposable != null) loggerDisposables.add(disposable)
        if (loggerDisposables.size > 30) loggerDisposables.removeAll { it.isDisposed }
    }

    val initCmdOps = initCmds?.map { cmd -> CmdOp.Run(cmd) }
        ?: emptyList()

    val updateContext = UpdateContext<ModelT, MsgT, CmdT, SubT>()
    val subContext = SubContext<SubT>()

    val initUpdatePrime =
        UpdatePrime(
            model = initModel,
            msg = null as MsgT?,
            modelPrime = initModel,
            cmdOps = initCmdOps,
            subsFromOtherContext = emptyList<SubT>(),
            msgsFromOtherContext = emptyList()
        )

    val logIdx = AtomicInteger(0)
    fun getLogIdx() = logIdx.getAndIncrement()

    msgInput
        .serialize()
        .mergeWith(msgSubj)
        .mergeWith(errorSubj)
        .scan(initUpdatePrime) { acc, msg ->
            val currentModel = acc.modelPrime ?: acc.model
            updateContext.execute(update, currentModel, msg)
        }
        .doOnNext { updatePrime -> modelSubj.onNext(updatePrime.modelPrime.toOptional()) }
        .doOnNext { updatePrime ->
            fun cancelCmd(cmdId: String, logIdNotFound: Boolean) {
                val disposable = cmdDisposables[cmdId]
                if (disposable == null) {
                    if (logIdNotFound) {
                        Log.CmdIdNotFoundToCancel<ModelT, MsgT, CmdT, SubT>(getLogIdx(), cmdId).log()
                    }
                } else {
                    Log.CmdCancelled<ModelT, MsgT, CmdT, SubT>(getLogIdx(), cmdId).log()
                    disposable.dispose()
                }
            }

            fun processCmdOp(cmdOp: CmdOp<CmdT>) {
                when (cmdOp) {
                    is CmdOp.Cancel -> cancelCmd(cmdOp.cmdId, logIdNotFound = true)
                    is CmdOp.Run -> {
                        cancelCmd(cmdOp.cmd.id, logIdNotFound = false)
                        Log.CmdStarted<ModelT, MsgT, CmdT, SubT>(getLogIdx(), cmdOp.cmd).log()
                        cmdDisposables[cmdOp.cmd.id] =
                            cmdToMaybe(cmdOp.cmd)
                                .doOnSuccess {
                                    Log.CmdEmission<ModelT, MsgT, CmdT, SubT>(getLogIdx(), cmdOp.cmd, it)
                                        .log()
                                }
                                .doOnSuccess(msgSubj::onNext)
                                .doOnError { error: Throwable ->
                                    Log.CmdError<ModelT, MsgT, CmdT, SubT>(
                                        getLogIdx(),
                                        cmdOp.cmd,
                                        error
                                    ).log()

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
                updatePrime.cmdOps.forEach(::processCmdOp)
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
        .doOnNext { updatePrime -> updatePrime.msgsFromOtherContext.forEach(msgSubj::onNext) }
        .scan(SubsState<ModelT, MsgT, CmdT, SubT>()) { subsState, updatePrime ->
            val model = updatePrime.modelPrime ?: return@scan subsState

            val subsPrime = subContext.execute(
                f = subscriptions,
                model = model
            )

            SubsState(
                updatePrime,
                subs = subsState.subsPrime,
                subsPrime = subsPrime
            )
        }
        .skip(1)
        .doOnNext { subsState ->
            val (updatePrime, subs, subsPrime) = subsState

            buildLogUpdate(getLogIdx(), updatePrime!!, subsPrime).log()

            val subsDiffs = computeSubsDiff(old = subs, new = subsPrime)
            val msgObs = msgInput.mergeWith(msgSubj).hide()
            val modelObs = modelSubj.hide()
                .filter { it is Some<*> }
                .map { it.toNullable()!! }

            subsDiffs.forEach { diff ->
                when (diff) {
                    is SubsDiffOp.Create ->
                        subToObservable(diff.sub, msgObs, modelObs)
                            .also {
                                Log.SubscriptionStarted<ModelT, MsgT, CmdT, SubT>(getLogIdx(), diff.sub)
                                    .log()
                            }
                            .doOnNext { msg ->
                                Log.SubscriptionEmission<ModelT, MsgT, CmdT, SubT>(
                                    getLogIdx(), diff.sub, msg
                                ).log()
                            }
                            .doOnNext(msgSubj::onNext)
                            .doOnError { error ->
                                Log.SubscriptionError<ModelT, MsgT, CmdT, SubT>(
                                    getLogIdx(), diff.sub, error
                                ).log()
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
                            Log.SubscriptionCancelled<ModelT, MsgT, CmdT, SubT>(getLogIdx(), diff.sub)
                                .log()
                            subDisposables[diff.sub.id]?.dispose()
                            subDisposables.remove(diff.sub.id)
                        }
                    }
                }
            }
        }
        .map { it.updatePrime?.modelPrime.toOptional() }
        .filter { it is Some<ModelT> }
        .map { it.toNullable()!! }
        .doOnDispose {
            cmdDisposables.values.forEach(Disposable::dispose)
            subDisposables.values.forEach(Disposable::dispose)
            loggerDisposables.forEach(Disposable::dispose)
        }
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

private fun <ModelT, MsgT, CmdT : Cmd, SubT : Sub> buildLogUpdate(
    index: Int,
    updatePrime: UpdatePrime<ModelT, MsgT, CmdT, SubT>,
    subs: List<SubT>
): Log.Update<ModelT, MsgT, CmdT, SubT> =
    Log.Update(
        index = index,
        model = updatePrime.model,
        msg = updatePrime.msg,
        modelPrime = updatePrime.modelPrime,
        cmdsStarted = updatePrime.cmdOps
            .filterIsInstance<CmdOp.Run<CmdT>>()
            .map { it.cmd },
        cmdIdsCancelled = updatePrime.cmdOps
            .filterIsInstance<CmdOp.Cancel<CmdT>>()
            .map { it.cmdId },
        subs = subs
    )
