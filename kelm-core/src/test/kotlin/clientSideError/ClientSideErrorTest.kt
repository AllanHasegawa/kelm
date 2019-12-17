package clientSideError

import clientSideError.AElement.Cmd
import clientSideError.AElement.Model
import clientSideError.AElement.Msg
import clientSideError.AElement.Sub
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.reactivex.Observable
import kelm.ExternalException
import kelm.Kelm
import kelm.SubContext
import kelm.UnhandledException
import kelm.UpdateContext
import org.spekframework.spek2.Spek

object AElement : Kelm.Element<Model, Msg, Cmd, Sub>() {
    var errorToMsgSaved: ExternalException? = null

    object Model
    object Msg
    object Cmd : kelm.Cmd()
    object Sub : kelm.Sub("sub")

    override fun UpdateContext<Model, Msg, Cmd, Sub>.update(model: Model, msg: Msg): Model? {
        +Cmd
        return null
    }

    override fun SubContext<Sub>.subscriptions(model: Model) {
        +Sub
    }

    override fun errorToMsg(error: ExternalException): Msg? {
        errorToMsgSaved = error
        return null
    }
}

object ClientSideErrorTest : Spek({
    group("given an element with faulty client side functions") {
        val cmdToMaybe = { cmd: Cmd -> error("ops") }
        var disposedSub = false
        val subToObs = { _: Sub, _: Observable<Msg>, _: Observable<Model> ->
            Observable.never<Msg>()
                .doOnDispose { disposedSub = true }
        }

        test("make sure in case of faulty client-side error, Kelm disposes all subs") {
            val ts = AElement
                .start(
                    initModel = Model,
                    msgInput = Observable.just(Msg),
                    cmdToMaybe = cmdToMaybe,
                    subToObs = subToObs
                )
                .test()

            ts.assertError { it is IllegalStateException && it.localizedMessage == "ops" }
            disposedSub shouldBe true
            AElement.errorToMsgSaved.shouldBeInstanceOf<UnhandledException>()
        }
    }
})
