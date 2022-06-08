package kelm.internal

import kelm.ModelCmds

internal data class UpdatePrime<ModelT, MsgT, CmdT>(
    val model: ModelT,
    val msg: MsgT?,
    val modelPrime: ModelT?,
    val cmds: List<CmdT>,
) {
    internal companion object {
        internal fun <ModelT, MsgT, CmdT> fromModelCmds(
            model: ModelT,
            msg: MsgT,
            modelCmdsPrime: ModelCmds<ModelT, CmdT>,
        ): UpdatePrime<ModelT, MsgT, CmdT> =
            UpdatePrime(
                model = model,
                msg = msg,
                modelPrime = modelCmdsPrime.model,
                cmds = modelCmdsPrime.cmds,
            )
    }
}
