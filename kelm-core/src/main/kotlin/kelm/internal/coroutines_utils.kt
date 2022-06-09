package kelm.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch

internal fun <T> CoroutineScope.mergeChannels(vararg channels: ReceiveChannel<T>): ReceiveChannel<T> {
    return produce {
        channels.forEach { channel ->
            launch {
                for (msg in channel) {
                    send(msg)
                }
            }
        }
    }
}
