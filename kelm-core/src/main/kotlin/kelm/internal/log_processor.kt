package kelm.internal

import kelm.Log
import kelm.LoggerF
import kelm.Sub
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch

internal fun <ModelT, MsgT, CmdT, SubT : Sub> CoroutineScope.logProcessorActor(
    logger: LoggerF<ModelT, MsgT, CmdT, SubT>,
    dispatcher: CoroutineDispatcher,
): SendChannel<Log<ModelT, MsgT, CmdT, SubT>> = actor {
    for (log in channel) {
        launch(dispatcher) {
            logger(log)
        }
    }
}
