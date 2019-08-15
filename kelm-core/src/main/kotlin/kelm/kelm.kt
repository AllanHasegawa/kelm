package kelm

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

abstract class Cmd(open val id: String = randomUuid()) {
    companion object {
        fun randomUuid(): String = UUID.randomUUID().toString()
    }
}

abstract class Sub(open val id: String) {
    companion object
}

class CmdFactoryNotImplementedException :
    RuntimeException("Cmd factory not implemented")

class SubFactoryNotImplementedException :
    RuntimeException("Subscription factory not implemented")

sealed class ExternalError(message: String, cause: Throwable) :
    RuntimeException(message, cause)

data class SubscriptionError(val subscription: Any, override val cause: Throwable) :
    ExternalError("The subscription [$subscription] threw an error", cause)

data class CmdError(val cmd: Any, override val cause: Throwable) :
    ExternalError("The command [$cmd] threw an error", cause)

class UpdateContext<CmdT : Cmd> internal constructor() {
    private val cmdOps = mutableListOf<CmdOp<CmdT>>()

    internal fun <ModelT, MsgT> execute(
        f: UpdateContext<CmdT>.(ModelT, MsgT) -> ModelT,
        model: ModelT,
        msg: MsgT
    ): UpdatePrime<ModelT, CmdT> {
        clear()
        val modelPrime = f(this, model, msg)
        return UpdatePrime(modelPrime, getCmdOps())
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

    private fun getCmdOps() = CmdOp.MultiOps(cmdOps.toList())

    private fun clear() {
        cmdOps.clear()
    }
}

class SubContext<SubT : Sub> internal constructor() {

    private val subs = mutableListOf<SubT>()

    internal fun <ModelT, MsgT> execute(
        f: SubContext<SubT>.(ModelT, Observable<ModelT>, Observable<MsgT>) -> Unit,
        model: ModelT,
        modelObs: Observable<ModelT>,
        msgObs: Observable<MsgT>
    ): List<SubT> {
        subs.clear()
        f(this, model, modelObs, msgObs)

        return subs.toList()
    }

    fun runSub(sub: SubT) {
        subs += sub
    }
}

object Kelm {
    @Suppress("UNCHECKED_CAST")
    fun <ModelT, MsgT, CmdT : Cmd, SubT : Sub> build(
        initModel: ModelT,
        msgInput: Observable<MsgT>,
        initCmd: CmdT? = null,
        subscriptions: SubContext<SubT>.(
            model: ModelT,
            modelObs: Observable<ModelT>,
            msgObs: Observable<MsgT>
        ) -> Unit = { _, _, _ -> },
        cmdToMaybe: (cmd: CmdT) -> Maybe<MsgT> = { Maybe.error(CmdFactoryNotImplementedException()) },
        subToObservable: (sub: SubT) -> Observable<MsgT> = { Observable.error(SubFactoryNotImplementedException()) },
        errorToMsg: (error: ExternalError) -> MsgT? = { null },
        updateWatcher: (model: ModelT, msg: MsgT, modelPrime: ModelT) -> Disposable? = { _, _, _ -> null },
        update: UpdateContext<CmdT>.(model: ModelT, msg: MsgT) -> ModelT
    ): Observable<ModelT> =
        Observable
            .defer {
                val errorSubj = PublishSubject.create<MsgT>()
                val modelSubj = BehaviorSubject.createDefault(initModel)
                val msgSubj = BehaviorSubject.create<MsgT>()

                val cmdDisposables = mutableMapOf<String, Disposable>()
                val subDisposables = mutableMapOf<String, Disposable>()

                val watcherDisposables = mutableListOf<Disposable>()

                val initCmdOp = when (initCmd) {
                    null -> CmdOp.NoOp as CmdOp<CmdT>
                    else -> CmdOp.Run(initCmd)
                }

                val updateContext = UpdateContext<CmdT>()
                val subContext = SubContext<SubT>()

                msgInput
                    .mergeWith(msgSubj)
                    .mergeWith(errorSubj)
                    .scan(
                        UpdatePrime(initModel, initCmdOp)
                    ) { acc, msg ->
                        updateContext.execute(update, acc.modelPrime, msg)
                            .also { accPrime ->
                                val maybeDisposable = updateWatcher(acc.modelPrime, msg, accPrime.modelPrime)
                                if (maybeDisposable != null) {
                                    watcherDisposables.add(maybeDisposable)
                                }
                                watcherDisposables.removeAll { it.isDisposed }
                            }
                    }
                    .doOnNext { (modelPrime, _) -> modelSubj.onNext(modelPrime) }
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

                        processCmdOp(updatePrime.cmdOp)
                    }
                    .doOnNext { _ ->
                        cmdDisposables
                            .toMap()
                            .filter { it.value.isDisposed }
                            .map { it.key }
                            .forEach { cmdDisposables.remove(it) }
                    }
                    .map { it.modelPrime }
                    .scan(SubsState<ModelT, SubT>()) { subsState, model ->
                        val subsPrime = subContext.execute(
                            f = subscriptions,
                            model = model,
                            modelObs = modelSubj.hide(),
                            msgObs = msgSubj.hide()
                        )

                        SubsState(model, subs = subsState.subsPrime, subsPrime = subsPrime)
                    }
                    .doOnNext { (_, subs, subsPrime) ->
                        val subsDiffs = computeSubsDiff(old = subs, new = subsPrime)
                        subsDiffs.forEach { diff ->
                            when (diff) {
                                is SubsDiffOp.Create ->
                                    subToObservable(diff.sub)
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
                                        .let { disposable -> subDisposables[diff.sub.id] = disposable }
                                is SubsDiffOp.Dispose -> {
                                    subDisposables[diff.sub.id]?.dispose()
                                    subDisposables.remove(diff.sub.id)
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
    val modelPrime: ModelT,
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

    val toCreate = toCreateIds
        .map { id -> newIdx.getValue(id) }
        .map { SubsDiffOp.Create(it) as SubsDiffOp<SubT> }

    val toDispose = toDisposeIds
        .map { id -> oldIdx.getValue(id) }
        .map { SubsDiffOp.Dispose(it) as SubsDiffOp<SubT> }

    return toDispose + toCreate
}
