package kelm

import io.reactivex.Observable
import io.reactivex.disposables.Disposable

object Kelm2 {
    abstract class Simple<ModelT, MsgT> {
        abstract fun initModel(): ModelT
        abstract fun update(model: ModelT, msg: MsgT): ModelT?
        abstract fun errorToMsg(error: ExternalError): MsgT?

        fun updateWatcher(model: ModelT, msg: MsgT, modelPrime: ModelT?): Disposable? =
            null
    }

    object MyCmd : Cmd()
    abstract class Program<ModelT, MsgT, CmdT : Cmd> {
        abstract fun initModel(): ModelT
        abstract fun UpdateContext<MsgT, CmdT>.update(model: ModelT, msg: MsgT): ModelT?
        abstract fun errorToMsg(error: ExternalError): MsgT?

        fun updateWatcher(model: ModelT, msg: MsgT, modelPrime: ModelT?): Disposable? =
            null

        internal infix fun ModelT.withCmd(cmd: CmdT): UpdatePrime<ModelT, CmdT> =
            UpdatePrime<ModelT, CmdT>(this, null, CmdOp.Run(cmd))
    }
}

fun <ModelT, MsgT> doKelm(
    obj: Kelm2.Simple<ModelT, MsgT>,
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
