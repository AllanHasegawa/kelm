package kelm.sample

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.net.SocketException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FakeFoxPicsService {
    companion object {
        private const val BASE_URL_PREFIX = """https://randomfox.ca/images/"""
        private const val BASE_URL_SUFFIX = """.jpg"""

        private const val CONN_ERROR_CHANCE = 0.5f
    }

    private val random = Random(42)

    fun fetchRandomFoxPicUrl(): Single<URL> =
        Single.timer(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .map {
                val shouldThrowError = random.nextFloat() > CONN_ERROR_CHANCE
                when (shouldThrowError) {
                    true -> throw SocketException()
                    false -> random.nextInt(1, 50)
                        .let { BASE_URL_PREFIX + it + BASE_URL_SUFFIX }
                        .let(::URL)
                }
            }
}
