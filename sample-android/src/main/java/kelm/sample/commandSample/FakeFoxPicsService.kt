package kelm.sample.commandSample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.SocketException
import java.net.URL
import kotlin.random.Random

class FakeFoxPicsService {
    companion object {
        private const val BASE_URL_PREFIX = """https://randomfox.ca/images/"""
        private const val BASE_URL_SUFFIX = """.jpg"""

        private const val CONN_ERROR_CHANCE = 0.5f
    }

    private val random = Random(42)

    suspend fun fetchRandomFoxPicUrl(): URL {
        return withContext(Dispatchers.IO) {
            delay(500)

            val shouldThrowError = random.nextFloat() > CONN_ERROR_CHANCE
            when (shouldThrowError) {
                true -> throw SocketException()
                false -> random.nextInt(1, 50)
                    .let { "$BASE_URL_PREFIX$it$BASE_URL_SUFFIX" }
                    .let(::URL)
            }
        }
    }
}
