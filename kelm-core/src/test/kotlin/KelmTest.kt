import E.Cmd
import E.Model
import E.Msg
import E.Sub
import app.cash.turbine.FlowTurbine
import app.cash.turbine.testIn
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldBe
import kelm.CmdException
import kelm.DispatcherProvider
import kelm.Element
import kelm.ExternalException
import kelm.SubscriptionException
import kelm.buildModelCmds
import kelm.buildSubs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

object E : Element<Model, Msg, Cmd, Sub>() {
    data class Model(val count: Int)

    sealed class Msg {
        data class Add(val value: Int) : Msg()
        object Ignore : Msg()
        data class StartCmd(val throwError: Boolean) : Msg()
    }

    data class Cmd(val throwError: Boolean)

    object Sub : kelm.Sub("the-sub")

    override fun update(model: Model, msg: Msg) = buildModelCmds {
        when (msg) {
            is Msg.Ignore -> null // Ignore when 'add' is 1001
            is Msg.Add -> model.copy(count = model.count + msg.value)
            is Msg.StartCmd -> {
                +Cmd(throwError = msg.throwError)
                null
            }
        }
    }

    override fun subscriptions(model: Model) = buildSubs {
        when (model.count) {
            in 10..Int.MAX_VALUE -> +Sub
        }
    }

    override fun exceptionToMsg(exception: ExternalException): Msg? = null
}

class KelmTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatcherProvider = DispatcherProvider(dispatcher, dispatcher)
    private val cmdControl =
        Channel<Unit>(capacity = Channel.RENDEZVOUS, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun subToFlow(
        sub: Sub,
        modelFlow: SharedFlow<Model>,
        msgFlow: SharedFlow<Msg>
    ): Flow<Msg> =
        flow {
            while (currentCoroutineContext().isActive) {
                delay(1.minutes)
                emit(Msg.Add(100))
            }
        }.take(3)
            .onCompletion { if (it == null) throw IllegalStateException() }
            .flowOn(dispatcher)

    private suspend fun cmdExecutor(cmd: Cmd): Msg =
        when (cmd.throwError) {
            true -> {
                cmdControl.receive()
                throw IllegalStateException()
            }
            false -> {
                cmdControl.receive()
                Msg.Add(10)
            }
        }

    private fun msgs(vararg things: Any) =
        Channel<Msg>(capacity = Channel.UNLIMITED).also {
            for (t in things) {
                val asMsg = when (t) {
                    is Int -> Msg.Add(t)
                    is Msg.Ignore,
                    is Msg.StartCmd -> t
                    else -> error("Unknown v")
                } as Msg
                it.trySend(asMsg)
            }
        }

    private fun start(msgs: Channel<Msg>, scope: CoroutineScope) =
        E.buildModelFlow(
            Model(0),
            msgs,
            ::cmdExecutor,
            ::subToFlow,
            dispatcherProvider = dispatcherProvider
        ).testIn(scope)

    private lateinit var underTest: CoroutineScope.() -> FlowTurbine<Model>
    private lateinit var runTestContext: (suspend FlowTurbine<Model>.() -> Unit) -> Unit

    @Nested
    @DisplayName("Given a sequence of 4 additions and 1 subtraction")
    inner class SimpleUpdate {
        @BeforeEach
        fun setUp() {
            val msgs = msgs(1, 1, 1, -1, 1)
            underTest = { start(msgs, this) }
            runTestContext = { testBody -> runTest(dispatcher) { underTest().run { testBody() } } }
        }

        @Test
        fun `the stream should not error`() = runTestContext {
            skipItems(6)
            cancel()
        }

        @Test
        fun `the final state should be 3`() = runTestContext {
            awaitItem() shouldBe Model(0)
            awaitItem() shouldBe Model(1)
            awaitItem() shouldBe Model(2)
            awaitItem() shouldBe Model(3)
            awaitItem() shouldBe Model(2)
            awaitItem() shouldBe Model(3)
            cancel()
        }

        @Test
        fun `the stream should not complete`() = runTest(dispatcher) {
            launch {
                withTimeoutOrNull(10.minutes) {
                    underTest().run {
                        skipItems(6)
                        awaitComplete()
                    }
                } shouldBe null
            }

            advanceTimeBy(11 * 60 * 1000)
        }
    }

    @DisplayName("Given 4 additions and a cmd with msgs")
    @Nested
    inner class CmdOk {
        @BeforeEach
        fun setUp() {
            val msgs = msgs(1, 1, 1, 1, Msg.StartCmd(throwError = false))
            underTest = { start(msgs, this) }
        }

        @Test
        fun `the cmd should be executed`() = runTestContext {
            cmdControl.trySend(Unit)
            awaitItem() shouldBe Model(0)
            awaitItem() shouldBe Model(1)
            awaitItem() shouldBe Model(2)
            awaitItem() shouldBe Model(3)
            awaitItem() shouldBe Model(4)
            awaitItem() shouldBe Model(14)
            cancel()
        }
    }

    @DisplayName("Given 4 additions and a cmd with error")
    @Nested
    inner class CmdError {
        @BeforeEach
        fun setUp() {
            val msgs = msgs(1, 1, 1, 1, Msg.StartCmd(throwError = true))
            underTest = { start(msgs, this) }
        }

        @Test
        fun `the cmd should throw an error and cancel the main stream`() = runTestContext {
            cmdControl.trySend(Unit)
            awaitItem() shouldBe Model(0)
            awaitItem() shouldBe Model(1)
            awaitItem() shouldBe Model(2)
            awaitItem() shouldBe Model(3)
            awaitItem() shouldBe Model(4)
            awaitError().shouldBeTypeOf<CmdException> {
                it.cmd shouldBe Cmd(throwError = true)
            }
        }
    }

    @DisplayName("Given a msg of value 10")
    @Nested
    inner class SubTest {
        @BeforeEach
        fun setUp() {
            val msgs = msgs(10)
            underTest = { start(msgs, this) }
        }

        @Test
        fun `the subscription 'up' should start and increment by 100 each minute`() =
            runTestContext {
                awaitItem() shouldBe Model(0)
                awaitItem() shouldBe Model(10)
                awaitItem() shouldBe Model(110)
                awaitItem() shouldBe Model(210)
                cancel()
            }

        @Test
        fun `the subscription 'up' should start and throw an exception after 3 minutes`() =
            runTestContext {
                skipItems(4)
                awaitError().shouldBeTypeOf<SubscriptionException> {
                    it.subscription shouldBe Sub
                }
                cancel()
            }
    }

    @DisplayName("Given a sequence of 2 additions, 1 ignored msg, 1 addition")
    @Nested
    inner class IgnoreMsg {
        @BeforeEach
        fun setUp() {
            val msgs = msgs(1, 1, Msg.Ignore, 2)
            underTest = { start(msgs, this) }
        }

        @Test
        fun `the ignored msg should not change the final result`() = runTestContext {
            awaitItem() shouldBe Model(0)
            awaitItem() shouldBe Model(1)
            awaitItem() shouldBe Model(2)
            awaitItem() shouldBe Model(4)
            cancel()
        }
    }
}
