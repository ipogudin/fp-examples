package ipogudin

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ipogudin.jackson.PersitentMapDeserializer
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

//curl -vvv -X POST "http://localhost:8080/crawler" -H "Content-Type: application/json" -d '{"urls": {"ya": "https://ya.ru"}, "childrenLevel": 1, "childrenPattern": ".*yandex\\.ru.*"}'
fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
                val m = SimpleModule()
                m.addDeserializer(PersistentMap::class.java, PersitentMapDeserializer<Any, Any>())
                registerModule(m)
            }
        }

        routing {
            post("/crawler") {
                withContext(Dispatchers.IO) {
                    val request = call.receive<CrawlingRequest>()
                    val response = crawl(request)
                    call.respond(response)
                }
            }
        }
    }
    server.start(wait = true)
}