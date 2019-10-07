import io.kotlintest.shouldBe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kelm.Kelm
import org.spekframework.spek2.Spek
import java.util.concurrent.TimeUnit

private data class ModelSubBug(val msgs: List<MsgSubBug>)

private sealed class MsgSubBug {
    object A : MsgSubBug()
    object B : MsgSubBug()
    object C : MsgSubBug()
    object D : MsgSubBug()
}

private object CmdSubBug : kelm.Cmd()

private sealed class SubSubBug(id: String) : kelm.Sub(id) {
    object A : SubSubBug("sub-a")
    object B : SubSubBug("sub-b")
    object C : SubSubBug("sub-c")
}

class SubFirstBugTest : Spek({
    repeat(1000) {
        group("Given a sub sends msgs right at its start, then all msgs should be handled") {
            // We are using the default schedulers during tests for purpose,
            // the idea is to force msgs being sent from multiples thread pools
            val subAObs = Observable.interval(0L, 1000L, TimeUnit.MILLISECONDS)
                .map { MsgSubBug.A as MsgSubBug }
            val subBObs = Observable.interval(0L, 1000L, TimeUnit.MILLISECONDS)
                .map { MsgSubBug.B as MsgSubBug }
            val subCObs = Observable.just(MsgSubBug.C as MsgSubBug)

            val msgInput = PublishSubject.create<MsgSubBug>()

            val to = Kelm.build<ModelSubBug, MsgSubBug, CmdSubBug, SubSubBug>(
                initModel = ModelSubBug(emptyList()),
                msgInput = msgInput,
                subscriptions = {
                    runSub(SubSubBug.A)
                    runSub(SubSubBug.B)
                    runSub(SubSubBug.C)
                },
                subToObservable = { sub, _, _ ->
                    when (sub) {
                        is SubSubBug.A -> subAObs
                        is SubSubBug.B -> subBObs
                        is SubSubBug.C -> subCObs
                    }
                },
                update = { model, msg -> model.copy(msgs = model.msgs + msg) }
            ).test()

            Single.timer(10L, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(Schedulers.trampoline())
                .doOnSuccess {
                    msgInput.onNext(MsgSubBug.D)
                }
                .subscribe()

            to.awaitCount(5, {}, 15L)
            val actualMsgs = to.values().last().msgs
            actualMsgs.count { it is MsgSubBug.A } shouldBe 1
            actualMsgs.count { it is MsgSubBug.B } shouldBe 1
            actualMsgs.count { it is MsgSubBug.C } shouldBe 1
            actualMsgs.count { it is MsgSubBug.D } shouldBe 1
        }
    }
})
