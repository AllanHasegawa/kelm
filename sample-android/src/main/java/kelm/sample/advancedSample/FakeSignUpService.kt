package kelm.sample.advancedSample

import kotlinx.coroutines.delay
import java.net.SocketTimeoutException
import java.util.Random
import kotlin.time.Duration.Companion.seconds

class FakeSignUpService {
    private val random = Random()

    suspend fun submitForm(email: String, password: String, idempotencyKey: String): String {
        println("Submitting form: $email, $password, $idempotencyKey")

        delay(1.seconds)

        return when (random.nextBoolean()) {
            true -> "user-id-012abc"
            false -> throw SocketTimeoutException()
        }
    }

    suspend fun registerPet(userId: String, petName: String): String {
        println("Registering pet: $userId, $petName")

        delay(2.seconds)

        return when (random.nextFloat() > 0.1f) {
            true -> "pet-id-01234"
            false -> throw SocketTimeoutException()
        }
    }
}
