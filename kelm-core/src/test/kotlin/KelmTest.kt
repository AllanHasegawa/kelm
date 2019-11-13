import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kelm.CmdException
import kelm.ExternalException
import kelm.Kelm
import kelm.SubContext
import kelm.SubscriptionException
import kelm.UpdateContext
import org.spekframework.spek2.Spek
import java.util.concurrent.TimeUnit

object E : Kelm.Element<E.Model, E.Msg, E.Cmd, E.Sub>() {
    data class Model(val count: Int)
    sealed class Msg {
        data class Add(val value: Int) : Msg()
        object Ignore : Msg()
        data class StartCmd(val throwError: Boolean) : Msg()
    }

    data class Cmd(val throwError: Boolean) : kelm.Cmd()

    object Sub : kelm.Sub("the-sub")

    override fun UpdateContext<Model, Msg, Cmd, Sub>.update(model: Model, msg: Msg): Model? =
        when (msg) {
            is Msg.Ignore -> null // Ignore when 'add' is 1001
            is Msg.Add -> model.copy(count = model.count + msg.value)
            is Msg.StartCmd -> {
                +Cmd(throwError = msg.throwError)
                null
            }
        }

    override fun SubContext<Sub>.subscriptions(model: Model) {
        when (model.count) {
            in 10..Int.MAX_VALUE -> Sub.retainSub()
        }
    }

    override fun errorToMsg(error: ExternalException): Msg? = null
}

object KelmTest : Spek({
    group("Given a simple element and an initial model with sum of 0") {
        val cmdSubj = PublishSubject.create<Unit>()
        val testScheduler = TestScheduler()

        fun subToObservable(
            sub: E.Sub,
            msgObs: Observable<E.Msg>,
            modelObs: Observable<E.Model>
        ): Observable<E.Msg> =
            Observable
                .interval(1, TimeUnit.MINUTES, testScheduler)
                .map { E.Msg.Add(100) as E.Msg }
                .take(3)
                .concatWith(Observable.error(IllegalStateException()))

        fun cmdToMaybe(cmd: E.Cmd): Maybe<E.Msg> =
            when (cmd.throwError) {
                true -> cmdSubj.map { throw IllegalStateException() }
                    .cast(E.Msg::class.java)
                    .firstOrError()
                    .toMaybe()
                false -> cmdSubj.map { E.Msg.Add(10) as E.Msg }
                    .firstOrError()
                    .toMaybe()
            }

        fun msgs(vararg vs: Any) =
            ReplaySubject.create<E.Msg> { emitter ->
                vs.map { v ->
                    when (v) {
                        is Int -> E.Msg.Add(v)
                        is E.Msg.Ignore,
                        is E.Msg.StartCmd -> v
                        else -> error("Unknown v")
                    } as E.Msg
                }.forEach(emitter::onNext)
            }

        fun start(msgInput: Observable<E.Msg>) =
            E.start(
                initModel = E.Model(0),
                msgInput = msgInput,
                cmdToMaybe = ::cmdToMaybe,
                subToObs = ::subToObservable
            ).test()

        lateinit var testObs: TestObserver<E.Model>

        group("given a sequence of 4 addition msgs and 1 down") {
            val msgs = msgs(1, 1, 1, -1, 1)

            beforeEachTest { testObs = start(msgs) }

            test("the stream should not error") {
                testObs.assertNoErrors()
            }

            test("the final state should be 3") {
                testObs.assertValueSequence(listOf(0, 1, 2, 3, 2, 3).map(E::Model))
            }

            test("the stream should not complete") {
                testObs.assertNotComplete()
            }
        }

        group("given a sequence of 4 additions and start cmd msgs") {
            val msgs = msgs(1, 1, 1, 1, E.Msg.StartCmd(throwError = false))

            beforeEachTest { testObs = start(msgs) }

            test("the cmd should be executed") {
                cmdSubj.onNext(Unit)
                testObs.assertValueSequence(listOf(0, 1, 2, 3, 4, 14).map(E::Model))
            }
        }

        group("given a sequence of 4 additions and start cmd with error") {
            val msgs = msgs(1, 1, 1, 1, E.Msg.StartCmd(throwError = true))

            beforeEachTest { testObs = start(msgs) }

            test("the cmd should throw an error and stop the main stream") {
                cmdSubj.onNext(Unit)
                testObs.assertValueSequence(listOf(0, 1, 2, 3, 4).map(E::Model))
                testObs.assertError(CmdException::class.java)

                val error = testObs.errors().first()
                error.shouldBeInstanceOf<CmdException>()
                (error as CmdException).cmd shouldBe E.Cmd(throwError = true)
            }
        }

        group("given a msg of value 10") {
            val msgs = msgs(10)

            beforeEachTest {
                testObs = start(msgs)
            }

            test("the subscription 'up' should start and increment by 100 each minute") {
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testObs.assertValueSequence(listOf(0, 10, 110, 210).map(E::Model))
            }

            test("the subscription 'up' should start and throw an error after 3 minutes") {
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
                testObs.assertError(SubscriptionException::class.java)

                val error = testObs.errors().first()
                (error as SubscriptionException).subscription shouldBe E.Sub
            }
        }

        group("given a sequence of 2 additions, 1 ignored msg, 1 addition") {
            val msgs = msgs(1, 1, E.Msg.Ignore, 2)

            beforeEachTest {
                testObs = start(msgs)
            }

            test("the ignored msg should not change the final result") {
                testObs.assertValueSequence(listOf(0, 1, 2, 4).map(E::Model))
            }
        }
    }
})
