package kelm.internal

import kelm.CmdException
import kelm.ExternalException
import kelm.Log
import kelm.Sub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch

internal fun <ModelT, MsgT, CmdT, SubT : Sub> CoroutineScope.cmdProcessorActor(
    cmdExecutor: suspend (cmd: CmdT) -> MsgT?,
    internalMsgChannel: SendChannel<MsgT>,
    externalExceptionProcessor: SendChannel<ExternalException>,
    logProcessor: SendChannel<Log<ModelT, MsgT, CmdT, SubT>>,
): SendChannel<CmdT> = actor {
    fun Log<ModelT, MsgT, CmdT, SubT>.send() {
        launch {
            logProcessor.send(this@send)
        }
    }

    fun runCmdAsync(cmd: CmdT) {
        Log.CmdLaunched<ModelT, MsgT, CmdT, SubT>(cmd).send()

        launch {
            val msg = try {
                cmdExecutor(cmd)
            } catch (t: Throwable) {
                Log.CmdError<ModelT, MsgT, CmdT, SubT>(cmd, t).send()
                externalExceptionProcessor.send(CmdException(cmd as Any, cause = t))
                null
            }
            if (msg != null) {
                Log.CmdEmission<ModelT, MsgT, CmdT, SubT>(cmd, msg).send()
                internalMsgChannel.send(msg)
            }
        }
    }

    for (cmd in channel) {
        runCmdAsync(cmd)
    }
}
