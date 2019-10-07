package kelm

class CmdFactoryNotImplementedException :
    RuntimeException("Cmd factory not implemented")

class SubFactoryNotImplementedException :
    RuntimeException("Subscription factory not implemented")

sealed class ExternalError(message: String, cause: Throwable) :
    RuntimeException(message, cause)

data class SubscriptionError(val subscription: Any, override val cause: Throwable) :
    ExternalError("The subscription [$subscription] threw an error", cause)

data class CmdError(val cmd: Any, override val cause: Throwable) :
    ExternalError("The command [$cmd] threw an error", cause)
