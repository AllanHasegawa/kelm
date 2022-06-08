package kelm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

public typealias UpdateF<ModelT, MsgT, CmdT> = UpdateContext<CmdT>.(ModelT, MsgT) -> ModelT?
public typealias LoggerF<ModelT, MsgT, CmdT, SubT> = (Log<ModelT, MsgT, CmdT, SubT>) -> Unit
public typealias SubF<ModelT, SubT> = SubContext<SubT>.(ModelT) -> Unit
public typealias SubToFlowF<ModelT, MsgT, SubT> = (sub: SubT, modelFlow: SharedFlow<ModelT>, msgFlow: SharedFlow<MsgT>) -> Flow<MsgT>
public typealias CmdExecutorF<CmdT, MsgT> = suspend (CmdT) -> MsgT?
public typealias ExceptionToMsgF<MsgT> = (ExternalException) -> MsgT?
