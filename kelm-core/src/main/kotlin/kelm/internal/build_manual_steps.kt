package kelm.internal

import kelm.Element
import kelm.Sub
import kelm.UpdateStep

internal fun <ModelT, MsgT, CmdT, SubT : Sub> buildManualSteps(
    element: Element<ModelT, MsgT, CmdT, SubT>,
    initModel: ModelT,
    vararg msgs: MsgT?,
): List<UpdateStep<ModelT, MsgT, CmdT, SubT>> {
    require(msgs.isNotEmpty()) {
        "[msgs] must not be empty."
    }
    require(!msgs.drop(1).contains(null)) {
        "'null' Msg after the first position is not allowed. See the [Element::buildManualSteps] docs for more info."
    }

    val initStep = buildManualStep(element, initModel, msgs.first())
    return msgs.drop(1)
        .scan(initStep) { acc, msg ->
            val model = acc.modelPrime ?: acc.model
            buildManualStep(element, model, msg)
        }
}

internal fun <ModelT, MsgT, CmdT, SubT : Sub> buildManualStep(
    element: Element<ModelT, MsgT, CmdT, SubT>,
    model: ModelT,
    msg: MsgT?,
): UpdateStep<ModelT, MsgT, CmdT, SubT> {
    val isInit = msg == null
    val subs = element::subscriptions

    return if (isInit) {
        val initCmds = element.initCmds(model) ?: emptyList()
        UpdateStep(
            model = model,
            msg = null,
            modelPrime = null,
            cmds = initCmds,
            subs = subs(model) ?: emptyList(),
        )
    } else {
        val modelCmdsPrime = element.update(model, msg!!)
        val updatePrime = UpdatePrime.fromModelCmds(model, msg, modelCmdsPrime)

        UpdateStep(
            model = updatePrime.model,
            msg = msg,
            modelPrime = updatePrime.modelPrime,
            cmds = updatePrime.cmds,
            subs = subs(updatePrime.modelPrime ?: updatePrime.model) ?: emptyList(),
        )
    }
}
