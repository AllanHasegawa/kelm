package kelm.internal

import kelm.ExternalException
import kelm.Log
import kelm.Sub
import kelm.SubToFlowF
import kelm.SubscriptionException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal fun <ModelT, MsgT, CmdT, SubT : Sub> CoroutineScope.subProcessorActor(
    subToFlow: SubToFlowF<ModelT, MsgT, SubT>,
    internalMsgChannel: SendChannel<MsgT>,
    externalExceptionProcessor: SendChannel<ExternalException>,
    subModelBroadcast: SharedFlow<ModelT>,
    subMsgBroadcast: SharedFlow<MsgT>,
    logProcessor: SendChannel<Log<ModelT, MsgT, CmdT, SubT>>,
): SendChannel<List<SubT>> = actor {
    fun Log<ModelT, MsgT, CmdT, SubT>.send() {
        logProcessor.trySend(this@send)
    }

    launch {
        var previousSubs = emptyList<Sub>()
        val runningSubs = mutableMapOf<String, SubJob<SubT>>()

        for (subs in channel) {
            val ops = computeSubsDiff(old = previousSubs, new = subs)
            previousSubs = subs

            ops.forEach { subOp ->
                @Suppress("UNCHECKED_CAST")
                val sub = subOp.sub as SubT

                when (subOp) {
                    is SubsDiffOp.Start -> {
                        val flow =
                            subToFlow(sub, subModelBroadcast, subMsgBroadcast)

                        val job = launch {
                            flow
                                .onEach { msg ->
                                    Log.SubscriptionEmission<ModelT, MsgT, CmdT, SubT>(sub, msg)
                                        .send()
                                }
                                .catch { t: Throwable ->
                                    Log.SubscriptionError<ModelT, MsgT, CmdT, SubT>(sub, t).send()
                                    val exception = SubscriptionException(sub, t)
                                    externalExceptionProcessor.send(exception)
                                }
                                .collectLatest { msg ->
                                    internalMsgChannel.send(msg)
                                }
                        }

                        runningSubs[sub.id] = SubJob(sub, job)
                        Log.SubscriptionStarted<ModelT, MsgT, CmdT, SubT>(sub).send()
                    }

                    is SubsDiffOp.Stop -> {
                        val (_, job) = runningSubs[sub.id] ?: return@forEach
                        job.cancel()
                        runningSubs.remove(sub.id)
                        Log.SubscriptionStopped<ModelT, MsgT, CmdT, SubT>(sub).send()
                    }
                }
            }
        }
    }
}

private data class SubJob<SubT : Sub>(val sub: SubT, val job: Job)

private sealed class SubsDiffOp<SubT : Sub>(val sub: SubT) {
    class Start<SubT : Sub>(sub: SubT) : SubsDiffOp<SubT>(sub)
    class Stop<SubT : Sub>(sub: SubT) : SubsDiffOp<SubT>(sub)
}

private fun <SubT : Sub> computeSubsDiff(old: List<SubT>, new: List<SubT>): List<SubsDiffOp<SubT>> {
    val oldIdx = old.associateBy { it.id }
    val newIdx = new.associateBy { it.id }

    val toCreateIds = newIdx.keys - oldIdx.keys
    val toDisposeIds = oldIdx.keys - newIdx.keys

    val toCreate: List<SubsDiffOp<SubT>> =
        toCreateIds
            .map { id -> newIdx.getValue(id) }
            .map { SubsDiffOp.Start(it) }

    val toDispose: List<SubsDiffOp<SubT>> =
        toDisposeIds
            .map { id -> oldIdx.getValue(id) }
            .map { SubsDiffOp.Stop(it) }

    return toDispose + toCreate
}
