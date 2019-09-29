package kelm

import io.reactivex.Observable
import io.reactivex.disposables.Disposable

abstract class KelmSimple<ModelT, MsgT> {
    abstract fun initModel(): ModelT
    abstract fun update(model: ModelT, msg: MsgT): ModelT?
    abstract fun errorToMsg(error: ExternalError): MsgT?

    fun updateWatcher(model: ModelT, msg: MsgT, modelPrime: ModelT?): Disposable? =
        null
}

fun <ModelT, MsgT> doKelm(
    obj: KelmSimple<ModelT, MsgT>,
    msgInput: Observable<MsgT>
): Observable<ModelT> =
    Kelm.build<ModelT, MsgT, Nothing, Nothing>(
        initModel = obj.initModel(),
        msgInput = msgInput,
        errorToMsg = obj::errorToMsg,
        updateWatcher = obj::updateWatcher,
        update = { model, msg ->
            with(obj) { update(model, msg) }
        }
    )

fun <ModelT, MsgT> steps(
    obj: KelmSimple<ModelT, MsgT>,
    vararg msgs: MsgT
) = Kelm.test<ModelT, MsgT, Nothing>(
    update = { model, msg ->
        with(obj) { update(model, msg) }
    }, initModel = obj.initModel(),
    msgs = msgs.toList()
)