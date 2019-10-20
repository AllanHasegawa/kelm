package kelm.sample.signUp

import io.reactivex.Completable
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.Random
import java.util.concurrent.TimeUnit

class FakeSignUpService {
    private val random = Random()

    fun submitForm(email: String, password: String, idempotencyKey: String): Single<String> =
        Single
            .fromCallable {
                println("Submitting form: $email, $password, $idempotencyKey")
                when (random.nextBoolean()) {
                    true -> "user-id-012abc"
                    false -> throw SocketTimeoutException()
                }
            }
            .onErrorResumeNext {
                Completable.timer(2, TimeUnit.SECONDS)
                    .andThen(Single.error(it))
            }
            .delay(4, TimeUnit.SECONDS)
            // We should always have a sensible timeout in a real app
            .timeout(30, TimeUnit.SECONDS)

    fun registerDevice(userId: String): Completable =
        Completable
            .fromCallable {
                println("Registering device: $userId")
                when (random.nextBoolean()) {
                    true -> Unit
                    false -> throw SocketTimeoutException()
                }
            }
            .onErrorResumeNext {
                Completable.timer(2, TimeUnit.SECONDS)
                    .andThen(Completable.error(it))
            }
            .delay(2, TimeUnit.SECONDS)
            .timeout(30, TimeUnit.SECONDS)

    fun registerPet(userId: String, petName: String): Single<String> =
        Single
            .fromCallable {
                println("Registering pet: $userId, $petName")
                when (random.nextFloat() > 0.1f) {
                    true -> "pet-id-01234"
                    false -> throw SocketTimeoutException()
                }
            }
            .onErrorResumeNext {
                Completable.timer(2, TimeUnit.SECONDS)
                    .andThen(Single.error(it))
            }
            .delay(2, TimeUnit.SECONDS)
            .timeout(30, TimeUnit.SECONDS)
}
