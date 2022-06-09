package kelm

import kelm.internal.buildManualSteps
import kelm.internal.buildModelFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow

public abstract class Element<ModelT, MsgT, CmdT, SubT : Sub> {
    public open fun initCmds(initModel: ModelT): List<CmdT>? = null

    public abstract fun update(
        model: ModelT,
        msg: MsgT,
    ): ModelCmds<ModelT, CmdT>

    public open fun subscriptions(model: ModelT): List<SubT>? = null

    public open fun exceptionToMsg(exception: ExternalException): MsgT? {
        throw exception
    }

    public fun buildModelFlow(
        initModel: ModelT,
        msgInput: SendChannel<MsgT>,
        cmdExecutor: CmdExecutorF<CmdT, MsgT>,
        subToFlow: SubToFlowF<ModelT, MsgT, SubT>,
        logger: LoggerF<ModelT, MsgT, CmdT, SubT> = {},
        dispatcherProvider: DispatcherProvider = DispatcherProvider(),
    ): Flow<ModelT> =
        buildModelFlow(
            element = this,
            initModel = initModel,
            msgInput = msgInput as Channel,
            cmdExecutor = cmdExecutor,
            subToFlow = subToFlow,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        )

    /**
     * Runs the [Element::update] function synchronously for the given [model] and [msgs].
     * For each [MsgT], an [UpdateStep] will be generated with the Model, Msg, Cmds and Subs created.
     *
     * @param[model] The initial Model.
     * @param[msgs] A sequence of Msgs to be processed with the [Element::update] function.
     * If the first element of [msgs] is `null`, then it'll count as an initialisation step and
     * the [Element::initCmds] will be used.
     */
    public fun runUpdateManually(
        model: ModelT,
        vararg msgs: MsgT?,
    ): List<UpdateStep<ModelT, MsgT, CmdT, SubT>> =
        buildManualSteps(this, model, *msgs)
}
