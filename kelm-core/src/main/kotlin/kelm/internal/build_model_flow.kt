package kelm.internal

import kelm.CmdExecutorF
import kelm.CmdFactoryNotImplementedException
import kelm.DispatcherProvider
import kelm.Element
import kelm.Log
import kelm.LoggerF
import kelm.Sub
import kelm.SubFactoryNotImplementedException
import kelm.SubToFlowF
import kelm.UnhandledException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal fun <ModelT, MsgT, CmdT, SubT : Sub> buildModelFlow(
    element: Element<ModelT, MsgT, CmdT, SubT>,
    initModel: ModelT,
    msgInput: Channel<MsgT>,
    cmdExecutor: CmdExecutorF<CmdT, MsgT> = { throw CmdFactoryNotImplementedException() },
    subToFlow: SubToFlowF<ModelT, MsgT, SubT> = { _, _, _ -> flow { throw SubFactoryNotImplementedException() } },
    logger: LoggerF<ModelT, MsgT, CmdT, SubT>,
    dispatcherProvider: DispatcherProvider,
): Flow<ModelT> = flow {
    coroutineScope {
        val internalMsgChannel = Channel<MsgT>()
        val mainMsgReceiveChannel = mergeChannels(msgInput, internalMsgChannel)
        val externalExceptionProcessor =
            externalExceptionProcessorActor(element::exceptionToMsg, internalMsgChannel)
        val logProcessorActor = logProcessorActor(logger, dispatcherProvider.loggerDispatcher)
        val cmdProcessorActor =
            cmdProcessorActor(
                cmdExecutor = cmdExecutor,
                internalMsgChannel = internalMsgChannel,
                externalExceptionProcessor = externalExceptionProcessor,
                logProcessor = logProcessorActor
            )

        val internalSubModelBroadcast = MutableSharedFlow<ModelT>()
        val internalSubMsgBroadcast = MutableSharedFlow<MsgT>()
        val subProcessorActor =
            subProcessorActor(
                subToFlow = subToFlow,
                internalMsgChannel = internalMsgChannel,
                externalExceptionProcessor = externalExceptionProcessor,
                subModelBroadcast = internalSubModelBroadcast.asSharedFlow(),
                subMsgBroadcast = internalSubMsgBroadcast.asSharedFlow(),
                logProcessor = logProcessorActor
            )

        val initUpdatePrime =
            UpdatePrime(
                model = initModel,
                msg = null as MsgT?,
                modelPrime = null,
                cmds = element.initCmds(initModel) ?: emptyList(),
            )

        var model = initModel
        emit(model)
        internalSubModelBroadcast.emit(model)

        initUpdatePrime.cmds.forEach { cmdProcessorActor.send(it) }
        element.subscriptions(model)
            ?.also { subProcessorActor.send(it) }
            ?.also { logProcessorActor.send(buildLogUpdate(initUpdatePrime, it)) }

        for (msg in mainMsgReceiveChannel) {
            internalSubMsgBroadcast.emit(msg)
            val updatePrime = try {
                val modelCmdsPrime = element.update(model, msg)
                UpdatePrime.fromModelCmds(model, msg, modelCmdsPrime)
            } catch (t: Throwable) {
                val exception = UnhandledException(cause = t)
                externalExceptionProcessor.send(exception)
                continue
            }

            val modelPrime = updatePrime.modelPrime
            if (modelPrime != null) {
                emit(modelPrime)
                model = modelPrime
                internalSubModelBroadcast.emit(modelPrime)
            }

            updatePrime.cmds.forEach { cmdProcessorActor.send(it) }
            element.subscriptions(model)
                ?.also { subProcessorActor.send(it) }
                ?.also { logProcessorActor.send(buildLogUpdate(updatePrime, it)) }
        }
    }
}.flowOn(dispatcherProvider.coreDispatcher)

private fun <ModelT, MsgT, CmdT, SubT : Sub> buildLogUpdate(
    updatePrime: UpdatePrime<ModelT, MsgT, CmdT>,
    subs: List<SubT>,
): Log.Update<ModelT, MsgT, CmdT, SubT> =
    Log.Update(
        model = updatePrime.model,
        msg = updatePrime.msg,
        modelPrime = updatePrime.modelPrime,
        cmds = updatePrime.cmds,
        subs = subs,
    )
