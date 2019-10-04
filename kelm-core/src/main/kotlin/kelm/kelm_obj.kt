package kelm

import io.reactivex.disposables.Disposable

object Kelm2 {
    abstract class Simple<ModelT, MsgT> {
        abstract fun initModel(): ModelT
        abstract fun update(model: ModelT, msg: MsgT): ModelT?
        abstract fun errorToMsg(error: ExternalError): MsgT?

        fun updateWatcher(step: Step<ModelT, MsgT, Nothing, Nothing>): Disposable? = null
    }

    abstract class WithCmds<ModelT, MsgT, CmdT : Cmd> {
        abstract fun initModel(): ModelT
        abstract fun UpdateContext<ModelT, MsgT, CmdT>.update(model: ModelT, msg: MsgT): ModelT?
        abstract fun errorToMsg(error: ExternalError): MsgT?

        fun initCmds(): List<CmdT>? = null
        fun updateWatcher(step: Step<ModelT, MsgT, CmdT, Nothing>): Disposable? = null
    }

    abstract class Program<ModelT, MsgT, CmdT : Cmd, SubT : Sub> {
        abstract fun initModel(): ModelT
        abstract fun UpdateContext<ModelT, MsgT, CmdT>.update(model: ModelT, msg: MsgT): ModelT?
        abstract fun errorToMsg(error: ExternalError): MsgT?
        abstract fun SubContext<SubT>.subscriptions(model: ModelT)

        fun initCmds(): List<CmdT>? = null
        fun updateWatcher(step: Step<ModelT, MsgT, CmdT, SubT>): Disposable? = null
    }
}

//fun <ModelT, MsgT> doKelm(
//    obj: Kelm2.Simple<ModelT, MsgT>,
//    msgInput: Observable<MsgT>
//): Observable<ModelT> =
//    Kelm.build<ModelT, MsgT, Nothing, Nothing>(
//        initModel = obj.initModel(),
//        msgInput = msgInput,
//        errorToMsg = obj::errorToMsg,
//        updateWatcher = obj::updateWatcher,
//        update = { model, msg ->
//            with(obj) { update(model, msg) }
//        }
//    )
