package kelm

public fun <SubT : Sub> buildSubs(f: SubContext<SubT>.() -> Unit): List<SubT> {
    val context = SubContext<SubT>()
    with(context) { f() }
    return context.subs
}

public fun <ModelT, MsgT, CmdT, SubT : Sub> Element<ModelT, MsgT, CmdT, SubT>.buildSubs(f: SubContext<SubT>.() -> Unit): List<SubT> =
    buildSubs<SubT>(f)

public class SubContext<SubT : Sub> internal constructor() {
    internal val subs = mutableListOf<SubT>()

    public operator fun SubT.unaryPlus() {
        subs += this
    }

    public operator fun List<SubT>.unaryPlus() {
        subs += this
    }

    public fun <ChildModelT, ChildMsgT, ChildCmdT, ChildSubT : Sub> childSubs(
        childElement: Element<ChildModelT, ChildMsgT, ChildCmdT, ChildSubT>,
        childModel: ChildModelT,
        childSubMapper: (ChildSubT) -> SubT
    ): Unit {
        childElement.subscriptions(childModel)?.map(childSubMapper)?.unaryPlus()
    }
}
