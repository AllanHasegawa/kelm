package kelm

internal sealed class Optional<out T>
internal data class Some<out T>(val value: T) : Optional<T>()
internal object None : Optional<Nothing>()

internal fun <T> T?.toOptional() = when (this) {
    null -> None
    else -> Some(this)
}

internal fun <T> Optional<T>.toNullable(): T? =
    when (this) {
        is Some<T> -> value
        is None -> null
    }
