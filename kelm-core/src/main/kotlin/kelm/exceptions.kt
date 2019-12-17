package kelm

class CmdFactoryNotImplementedException :
    RuntimeException("Cmd factory not implemented")

class SubFactoryNotImplementedException :
    RuntimeException("Subscription factory not implemented")

sealed class ExternalException(message: String, cause: Throwable) :
    RuntimeException(message, cause)

data class SubscriptionException(val subscription: Any, override val cause: Throwable) :
    ExternalException("The subscription [$subscription] threw an error", cause)

data class CmdException(val cmd: Any, override val cause: Throwable) :
    ExternalException("The command [$cmd] threw an error", cause)

data class UnhandledException(override val cause: Throwable) :
    ExternalException(
        "An unhandled exception was caught. Could be caused by a client side function.",
        cause
    )
