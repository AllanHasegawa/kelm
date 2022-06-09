package kelm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public open class DispatcherProvider(
    public val coreDispatcher: CoroutineDispatcher = Dispatchers.Default,
    public val loggerDispatcher: CoroutineDispatcher = Dispatchers.Main,
)
