package kelm

public fun <ModelT, CmdT> buildModelCmds(f: UpdateContext<CmdT>.() -> ModelT?): ModelCmds<ModelT, CmdT> {
    val context = UpdateContext<CmdT>()
    val modelPrime = with(context) {
        f()
    }
    return ModelCmds(modelPrime, context.cmds)
}

public fun <ModelT, MsgT, CmdT, SubT : Sub> Element<ModelT, MsgT, CmdT, SubT>.buildModelCmds(f: UpdateContext<CmdT>.() -> ModelT?): ModelCmds<ModelT, CmdT> =
    buildModelCmds<ModelT, CmdT>(f)

public class UpdateContext<CmdT> internal constructor() {
    internal val cmds = mutableListOf<CmdT>()

    public operator fun CmdT.unaryPlus() {
        cmds.add(this)
    }

    public operator fun List<CmdT>.unaryPlus() {
        cmds.addAll(this)
    }

    /**
     * Run the child [Kelm.Element::update] function for the [child] given the [childModel] and [childMsg].
     *
     * @param[childMsg] The Msg to be processed by the [child] `Element`. If null then it'll count as an initialisation
     * step and the child's [Element::initCmds] method will be used.
     * Whenever [childMsg] is null, the returned [ModelCmds::model] is guaranteed to be non null.
     *
     * @return A [ModelCmds] containing the new model and commands ran by the child element. The commands have not
     * being added to the parent's commands yet to be launched. Use [ModelCmds::toParent] for it.
     */
    public fun <ChildModelT, ChildMsgT, ChildCmdT, ChildSubT : Sub> updateChild(
        child: Element<ChildModelT, ChildMsgT, ChildCmdT, ChildSubT>,
        childModel: ChildModelT,
        childMsg: ChildMsgT?,
    ): ModelCmds<ChildModelT, ChildCmdT> {
        val updateStep = child.runUpdateManually(childModel, childMsg).last()
        val childModelPrime = updateStep.modelPrime ?: childModel
        val childCmds = updateStep.cmds
        return ModelCmds(childModelPrime, childCmds)
    }

    /**
     * Maps a child's ModelCmds to the parent ModelCmds, while adding the child's commands to the parent's commands.
     *
     * @param[cmdCaptor] Intercepts a child's command to map to a specific parent's model. This can be used
     * for navigation, for example.
     */
    public fun <ModelT, ChildModelT, ChildCmdT> ModelCmds<ChildModelT, ChildCmdT>.toParent(
        modelMapper: (ChildModelT) -> ModelT,
        cmdMapper: (ChildCmdT) -> CmdT?,
        cmdCaptor: ((ChildCmdT) -> ModelT?)? = null,
    ): ModelT? {
        val childCmds = this.cmds
        val parentCmds = this@UpdateContext.cmds

        parentCmds.addAll(childCmds.mapNotNull(cmdMapper))

        val capturedParentModel =
            if (cmdCaptor != null) childCmds.firstNotNullOfOrNull { cmdCaptor(it) }
            else null

        if (capturedParentModel != null) {
            return capturedParentModel
        }

        return model?.let(modelMapper)
    }
}
