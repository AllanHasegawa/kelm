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
}

private object CmdSubBug : kelm.Cmd()

private object SubSubBug : kelm.Sub("sub-id")

class SubFirstBugTest : Spek({
    repeat(10) {
        group("Given a sub sends msgs right at its start, then all msgs should be handled") {
            val subObs = Observable.interval(0L, 1L, TimeUnit.SECONDS)
                .map { MsgSubBug.B as MsgSubBug }

            val msgInput = PublishSubject.create<MsgSubBug>()

            val to = Kelm.build<ModelSubBug, MsgSubBug, CmdSubBug, SubSubBug>(
                initModel = ModelSubBug(emptyList()),
                msgInput = msgInput,
                subscriptions = { runSub(SubSubBug) },
                subToObservable = { _, _, _ -> subObs },
                update = { model, msg -> model.copy(msgs = model.msgs + msg) }
            ).test()

            Single.timer(1L, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(Schedulers.trampoline())
                .doOnSuccess {
                    msgInput.onNext(MsgSubBug.A)
                }
                .subscribe()

            to.awaitCount(2)
            to.assertValueAt(0, ModelSubBug(emptyList()))
            to.assertValueAt(1, ModelSubBug(listOf(MsgSubBug.B)))

            to.awaitCount(4)
            to.assertValueAt(3, ModelSubBug(listOf(MsgSubBug.B, MsgSubBug.A, MsgSubBug.B)))
        }
    }
})