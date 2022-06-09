package kelm.internal

import kelm.ExternalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch

internal fun <MsgT : Any> CoroutineScope.externalExceptionProcessorActor(
    exceptionToMsg: (ExternalException) -> MsgT?,
    internalMsgChanel: SendChannel<MsgT>,
): SendChannel<ExternalException> {
    return actor {
        launch {
            for (exception in channel) {
                val msg = exceptionToMsg(exception)
                if (msg != null) {
                    internalMsgChanel.send(msg)
                } else {
                    throw exception
                }
            }
        }
    }
}
