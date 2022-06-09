package kelm

public data class UpdateStep<ModelT, MsgT, CmdT, SubT : Sub>(
    val model: ModelT,
    val msg: MsgT?,
    val modelPrime: ModelT?,
    val cmds: List<CmdT>,
    val subs: List<SubT>,
) {
    val modelNonNull: ModelT = modelPrime ?: model
}
