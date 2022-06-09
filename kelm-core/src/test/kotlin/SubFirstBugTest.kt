import SubBugE.Model
import SubBugE.Msg.A
import SubBugE.Msg.B
import SubBugE.Msg.C
import SubBugE.Msg.D
import SubBugE.Sub
import app.cash.turbine.test
import io.kotlintest.shouldBe
import kelm.DispatcherProvider
import kelm.Element
import kelm.ExternalException
import kelm.buildModelCmds
import kelm.buildSubs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

object SubBugE : Element<Model, SubBugE.Msg, SubBugE.Cmd, Sub>() {
    data class Model(val msgs: List<Msg>)

    sealed class Msg {
        object A : Msg()
        object B : Msg()
        object C : Msg()
        object D : Msg()
    }

    object Cmd

    sealed class Sub(id: String) : kelm.Sub(id) {
        object A : Sub("sub-a")
        object B : Sub("sub-b")
        object C : Sub("sub-c")
    }

    fun initModel() = Model(emptyList())

    override fun update(model: Model, msg: Msg) = buildModelCmds {
        model.copy(msgs = model.msgs + msg)
    }

    override fun subscriptions(model: Model): List<Sub> = buildSubs {
        +Sub.A
        +Sub.B
        +Sub.C
    }

    override fun exceptionToMsg(exception: ExternalException): Msg? = null
}

class SubFirstBugTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `Given a sub sends msgs right at its start, then all msgs should be handled`() =
        runTest(dispatcher) {
            val subAFlow = flow<SubBugE.Msg> {
                while (currentCoroutineContext().isActive) {
                    emit(A)
                    delay(1000)
                }
            }.flowOn(dispatcher)

            val subBFlow = flow<SubBugE.Msg> {
                while (currentCoroutineContext().isActive) {
                    emit(B)
                    delay(1000)
                }
            }.flowOn(dispatcher)
            val subCFlow = flowOf(C)

            val msgInput = Channel<SubBugE.Msg>(1)
            launch {
                SubBugE
                    .buildModelFlow(
                        initModel = SubBugE.initModel(),
                        msgInput = msgInput,
                        cmdExecutor = { null },
                        subToFlow = { sub, _, _ ->
                            when (sub) {
                                is Sub.A -> subAFlow
                                is Sub.B -> subBFlow
                                is Sub.C -> subCFlow
                            }
                        },
                        dispatcherProvider = DispatcherProvider(dispatcher, dispatcher)
                    )
                    .take(5)
                    .test {
                        awaitItem() shouldBe Model(emptyList())
                        awaitItem() shouldBe Model(listOf(A))
                        awaitItem() shouldBe Model(listOf(A, B))
                        awaitItem() shouldBe Model(listOf(A, B, C))
                        awaitItem() shouldBe Model(listOf(A, B, C, D))
                        awaitComplete()
                    }
            }

            launch {
                delay(1000)
                msgInput.trySend(D)
            }

            advanceUntilIdle()
        }
}
