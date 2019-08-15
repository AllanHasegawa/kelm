import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import kelm.CmdError
import kelm.Kelm
import kelm.SubContext
import kelm.SubscriptionError
import kelm.UpdateContext
import org.spekframework.spek2.Spek
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object KelmTest : Spek({
    group("Given a simple contract and an initial model with sum of 0") {
        data class Model(val count: Int)
        data class Msg(val add: Int)
        class Cmd(val throwError: Boolean) : kelm.Cmd()
        class Sub : kelm.Sub("the-sub")

        val cmdSubj = PublishSubject.create<Unit>()
        val testScheduler = TestScheduler()

        fun msgs(vararg v: Int) = v.map(::Msg)

        fun UpdateContext<Cmd>.update(model: Model, msg: Msg) =
            model.copy(count = model.count + msg.add)
                .also {
                    if (it.count.absoluteValue == 5) {
                        runCmd(Cmd(throwError = it.count < 0))
                    }
                }

        fun subToObservable(sub: Sub) =
            Observable
                .interval(1, TimeUnit.MINUTES, testScheduler)
                .map { Msg(100) }
                .take(3)
                .concatWith(Observable.error(IllegalStateException()))

        fun cmdToMaybe(cmd: Cmd) =
            when (cmd.throwError) {
                true -> cmdSubj.map { throw IllegalStateException() }
                    .cast(Msg::class.java)
                    .firstOrError()
                    .toMaybe()
                false -> cmdSubj.map { Msg(add = 10) }.firstOrError()
                    .toMaybe()
            }

        fun SubContext<Sub>.subscriptions(
            model: Model,
            modelObs: Observable<Model>,
            msgObs: Observable<Msg>
        ) {
            when (model.count) {
                in 10..Int.MAX_VALUE ->
                    runSub(Sub())
            }
        }

        val model = Model(0)

        fun build(msgs: List<Msg>) = Kelm
            .build<Model, Msg, Cmd, Sub>(
                initModel = model,
                msgInput = Observable.fromIterable(msgs),
                cmdToMaybe = ::cmdToMaybe,
                subToObservable = ::subToObservable,
                subscriptions = { model, modelObs, msgObs -> subscriptions(model, modelObs, msgObs) },
                update = { model, msg -> update(model, msg) }
            )

        lateinit var testObs: TestObserver<Model>

        group("given a sequence of 4 addition msgs and 1 down") {
            val msgs = msgs(1, 1, 1, -1, 1)

            beforeEachTest {
                testObs = build(msgs).test()
            }

            test("the stream should not error") {
                testObs.assertNoErrors()
            }

            test("the final state should be 3") {
                testObs.assertValueSequence(listOf(0, 1, 2, 3, 2, 3).map(::Model))
            }

            test("the stream should not complete") {
                testObs.assertNotComplete()
            }
        }

        group("given a sequence of 5 addition msgs") {
            val msgs = msgs(1, 1, 1, 1, 1)

            beforeEachTest {
                testObs = build(msgs).test()
            }

            test("the cmd should be executed") {
                cmdSubj.onNext(Unit)
                testObs.assertValueSequence(listOf(0, 1, 2, 3, 4, 5, 15).map(::Model))
            }
        }

        group("given a sequence of 5 negation msgs") {
            val msgs = msgs(-1, -1, -1, -1, -1)

            beforeEachTest {
                testObs = build(msgs).test()
            }

            test("the cmd should be executed and throw an error terminating the stream") {
                cmdSubj.onNext(Unit)
                testObs.assertError { it is CmdError }
            }
        }

        group("given a msg of value 10") {
            val msgs = msgs(10)

            beforeEachTest {
                testObs = build(msgs).test()
            }

            test("the subscription 'up' should start and increment by 100 each minute") {
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testObs.assertValueSequence(listOf(0, 10, 110, 210).map(::Model))
            }

            test("the subscription 'up' should start and throw an error after 3 minutes") {
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testObs.assertError { it is SubscriptionError }
            }
        }
    }
})
