import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import kelm.CmdError
import kelm.Kelm
import kelm.Step
import kelm.SubContext
import kelm.SubscriptionError
import kelm.UpdateContext
import org.spekframework.spek2.Spek
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.test.assertEquals

object KelmTest : Spek({
    group("Given a simple contract and an initial model with sum of 0") {
        data class Model(val count: Int)
        data class Msg(val add: Int)
        data class Cmd(val throwError: Boolean) : kelm.Cmd()
        class Sub : kelm.Sub("the-sub")

        val cmdSubj = PublishSubject.create<Unit>()
        val testScheduler = TestScheduler()

        fun msgs(vararg v: Int) = v.map(::Msg)

        fun UpdateContext<Cmd>.update(model: Model, msg: Msg) =
            when (msg.add) {
                1001 -> null // Ignore when 'add' is 1001
                else ->
                    model.copy(count = model.count + msg.add)
                        .also {
                            if (it.count.absoluteValue == 5) {
                                runCmd(Cmd(throwError = it.count < 0))
                            }
                        }
            }

        fun runSteps(msgs: List<Msg>) =
            Kelm.test<Model, Msg, Cmd>({ model, msg -> update(model, msg) }, Model(0), msgs)

        fun subToObservable(sub: Sub, msgObs: Observable<Msg>, modelObs: Observable<Model>) =
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

        fun SubContext<Sub>.subscriptions(model: Model) {
            when (model.count) {
                in 10..Int.MAX_VALUE ->
                    retain(Sub())
            }
        }

        val model = Model(0)

        fun build(msgs: List<Msg>) = Kelm
            .build<Model, Msg, Cmd, Sub>(
                initModel = model,
                msgInput = Observable.fromIterable(msgs),
                cmdToMaybe = ::cmdToMaybe,
                subToObservable = ::subToObservable,
                subscriptions = { model -> subscriptions(model) },
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

            test("the last step should sum up to 3") {
                assertEquals(
                    actual = runSteps(msgs).last(),
                    expected = Step(Model(2), Msg(1), Model(3))
                )
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

            test("the last step should sum up to 5 and CMD recorded") {
                assertEquals(
                    actual = runSteps(msgs).last(),
                    expected = Step(Model(4), Msg(1), Model(5), listOf(Cmd(false)))
                )
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

            test("the last step should sum up to -5 and CMD recorded") {
                assertEquals(
                    actual = runSteps(msgs).last(),
                    expected = Step(Model(-4), Msg(-1), Model(-5), listOf(Cmd(true)))
                )
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

        group("given a sequence of 2 additions, 1 ignored msg, 1 addition") {
            val msgs = msgs(1, 1, 1001, 2)

            beforeEachTest {
                testObs = build(msgs).test()
            }

            test("the ignored msg should not change the final result") {
                testObs.assertValueSequence(listOf(0, 1, 2, 4).map(::Model))
            }

            test("the ignored msg should not change the steps") {
                assertEquals(
                    actual = runSteps(msgs).map { it.modelPrime?.count },
                    expected = listOf(1, 2, null, 4)
                )
            }
        }
    }
})
