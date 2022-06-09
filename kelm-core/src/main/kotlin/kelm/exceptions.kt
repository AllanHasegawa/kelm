package kelm

public class CmdFactoryNotImplementedException :
    RuntimeException("Cmd factory not implemented")

public class SubFactoryNotImplementedException :
    RuntimeException("Subscription factory not implemented")

public sealed class ExternalException(message: String, cause: Throwable) :
    RuntimeException(message, cause)

public data class SubscriptionException(val subscription: Sub, override val cause: Throwable) :
    ExternalException("The subscription [$subscription] threw an error", cause)

public data class CmdException(val cmd: Any, override val cause: Throwable) :
    ExternalException("The command [$cmd] threw an error", cause)

public data class UnhandledException(override val cause: Throwable) :
    ExternalException(
        "An unhandled exception was caught. Could be caused by a client side function.",
        cause
    )
