package kelm

public sealed class Log<ModelT, MsgT, CmdT, SubT : Sub> {
    public data class Update<ModelT, MsgT, CmdT, SubT : Sub>(
        val model: ModelT,
        val msg: MsgT?,
        val modelPrime: ModelT?,
        val cmds: List<CmdT>,
        val subs: List<SubT>,
    ) : Log<ModelT, MsgT, CmdT, SubT>() {
        public val isInitialization: Boolean = msg == null
    }

    public data class CmdLaunched<ModelT, MsgT, CmdT, SubT : Sub>(
        val cmd: CmdT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class CmdEmission<ModelT, MsgT, CmdT, SubT : Sub>(
        val cmd: CmdT,
        val msg: MsgT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class CmdError<ModelT, MsgT, CmdT, SubT : Sub>(
        val cmd: CmdT,
        val error: Throwable
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class SubscriptionStarted<ModelT, MsgT, CmdT, SubT : Sub>(
        val sub: SubT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class SubscriptionStopped<ModelT, MsgT, CmdT, SubT : Sub>(
        val sub: SubT
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class SubscriptionError<ModelT, MsgT, CmdT, SubT : Sub>(
        val sub: SubT,
        val error: Throwable
    ) : Log<ModelT, MsgT, CmdT, SubT>()

    public data class SubscriptionEmission<ModelT, MsgT, CmdT, SubT : Sub>(
        val sub: SubT,
        val msg: MsgT
    ) : Log<ModelT, MsgT, CmdT, SubT>()
}
