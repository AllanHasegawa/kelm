package kelm

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

public abstract class Sandbox<ModelT, MsgT> : Element<ModelT, MsgT, Nothing, Nothing>() {
    public abstract fun updateSimple(model: ModelT, msg: MsgT): ModelT?

    final override fun update(
        model: ModelT,
        msg: MsgT,
    ): ModelCmds<ModelT, Nothing> = ModelCmds(updateSimple(model, msg), emptyList())

    public fun buildModelFlow(
        initModel: ModelT,
        msgInput: SendChannel<MsgT>,
        logger: LoggerF<ModelT, MsgT, Nothing, Nothing> = {}
    ): Flow<ModelT> =
        buildModelFlow(
            initModel = initModel,
            msgInput = msgInput,
            cmdExecutor = { null },
            subToFlow = { _, _, _ -> flowOf() },
            logger = logger,
        )

    final override fun exceptionToMsg(exception: ExternalException): MsgT? = null
    final override fun initCmds(initModel: ModelT): List<Nothing>? = null
    final override fun subscriptions(model: ModelT): List<Nothing>? = null
}
