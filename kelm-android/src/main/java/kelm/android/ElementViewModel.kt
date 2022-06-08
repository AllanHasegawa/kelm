package kelm.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kelm.DispatcherProvider
import kelm.Element
import kelm.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

abstract class ElementViewModel<ModelT, MsgT, CmdT, SubT : kelm.Sub>(
    element: Element<ModelT, MsgT, CmdT, SubT>,
    initModel: ModelT,
    dispatcherProvider: DispatcherProvider = DispatcherProvider()
) : ViewModel() {
    val model: LiveData<ModelT> get() = _model

    private val msgChannel: SendChannel<MsgT> = Channel()
    private val _model = MutableLiveData<ModelT>()

    init {
        viewModelScope.launch {
            element.buildModelFlow(
                initModel = initModel,
                msgInput = msgChannel,
                cmdExecutor = ::cmdExecutor,
                subToFlow = ::subToFlow,
                logger = ::logger,
                dispatcherProvider = dispatcherProvider,
            ).collectLatest { _model.value = it }
        }
    }

    abstract suspend fun cmdExecutor(cmd: CmdT): MsgT?
    open fun subToFlow(
        sub: SubT,
        modelFlow: SharedFlow<ModelT>,
        msgFlow: SharedFlow<MsgT>
    ): Flow<MsgT> = flowOf()

    open fun logger(log: Log<ModelT, MsgT, CmdT, SubT>) = Unit

    fun onMsg(msg: MsgT) {
        msgChannel.trySend(msg)
    }
}
