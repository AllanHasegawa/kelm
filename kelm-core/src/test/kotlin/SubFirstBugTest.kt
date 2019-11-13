import io.kotlintest.shouldBe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kelm.ExternalException
import kelm.Kelm
import kelm.SubContext
import kelm.UpdateContext
import org.spekframework.spek2.Spek
import java.util.concurrent.TimeUnit

object SubBugE : Kelm.Element<SubBugE.Model, SubBugE.Msg, SubBugE.Cmd, SubBugE.Sub>() {
    data class Model(val msgs: List<Msg>)

    sealed class Msg {
        object A : Msg()
        object B : Msg()
        object C : Msg()
        object D : Msg()
    }

    object Cmd : kelm.Cmd()

    sealed class Sub(id: String) : kelm.Sub(id) {
        object A : Sub("sub-a")
        object B : Sub("sub-b")
        object C : Sub("sub-c")
    }

    fun initModel() = Model(emptyList())

    override fun UpdateContext<Model, Msg, Cmd, Sub>.update(model: Model, msg: Msg): Model? =
        model.copy(msgs = model.msgs + msg)

    override fun SubContext<Sub>.subscriptions(model: Model) {
        +Sub.A
        +Sub.B
        +Sub.C
    }

    override fun errorToMsg(error: ExternalException): Msg? = null
}

object SubFirstBugTest : Spek({
    repeat(1000) {
        group("Given a sub sends msgs right at its start, then all msgs should be handled") {
            // We are using the default schedulers during tests for purpose,
            // the idea is to force msgs being sent from multiples thread pools
            val subAObs = Observable.interval(0L, 1000L, TimeUnit.MILLISECONDS)
                .map { SubBugE.Msg.A as SubBugE.Msg }
            val subBObs = Observable.interval(0L, 1000L, TimeUnit.MILLISECONDS)
                .map { SubBugE.Msg.B as SubBugE.Msg }
            val subCObs = Observable.just(SubBugE.Msg.C as SubBugE.Msg)

            val msgInput = PublishSubject.create<SubBugE.Msg>()

            val to = SubBugE.start(
                initModel = SubBugE.initModel(),
                msgInput = msgInput,
                subToObs = { sub, _, _ ->
                    when (sub) {
                        is SubBugE.Sub.A -> subAObs
                        is SubBugE.Sub.B -> subBObs
                        is SubBugE.Sub.C -> subCObs
                    }
                },
                cmdToMaybe = { error("No CMD") }
            ).test()

            Single.timer(10L, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(Schedulers.trampoline())
                .doOnSuccess {
                    msgInput.onNext(SubBugE.Msg.D)
                }
                .subscribe()

            to.awaitCount(5, {}, 15L)
            val actualMsgs = to.values().last().msgs
            actualMsgs.count { it is SubBugE.Msg.A } shouldBe 1
            actualMsgs.count { it is SubBugE.Msg.B } shouldBe 1
            actualMsgs.count { it is SubBugE.Msg.C } shouldBe 1
            actualMsgs.count { it is SubBugE.Msg.D } shouldBe 1
        }
    }
})
