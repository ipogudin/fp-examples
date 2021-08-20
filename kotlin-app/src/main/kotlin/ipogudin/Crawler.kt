package ipogudin

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.lang.Exception

data class CrawlingRequest(
    val urls: PersistentMap<String, String>,
    val childrenPattern: String,
    val childrenLevel: Int)

data class SingleResponse(
    val successful: Boolean,
    val code: Int?,
    val message: String,
    val headers: PersistentMap<String, String>,
    val body: String?,
    val links: PersistentList<String>,
    val children: CrawlingResponse?
)

data class CrawlingResponse(val responses: PersistentMap<String, SingleResponse>)

suspend fun crawl(request: CrawlingRequest): CrawlingResponse {
    val client = HttpClient(CIO)
    val responses = merge(
        request
            .urls
            .map { e ->
                suspend {
                    Pair(
                        e.key,
                        handleChildren(request.childrenPattern, request.childrenLevel, parse {client.get(e.value)})
                    )
                }
            }.toPersistentList()
    ).toMap().toPersistentMap()
    return CrawlingResponse(responses = responses)
}

suspend fun parse(f: suspend () -> HttpResponse): SingleResponse {
    try {
        val response = f.invoke()
        val body = response.receive<String>()
        return SingleResponse(
            successful = true,
            code = response.status.value,
            message = response.status.description,
            headers = response.headers.entries().map { e -> Pair(e.key, e.value.first()) }.toMap().toPersistentMap(),
            body = body,
            links = extractLinks(response.request.url.protocol.name, body),
            children = null
        )
    } catch (e: Exception) {
        return SingleResponse(
            successful = true,
            code = null,
            message = e.message.orEmpty(),
            headers = emptyMap<String, String>().toPersistentMap(),
            body = e.stackTraceToString(),
            links = emptyList<String>().toPersistentList(),
            children = null
        )
    }
}

fun toRequest(pattern: String, level: Int, response: SingleResponse): CrawlingRequest =
    CrawlingRequest(
        urls = response.links
            .filter { l -> l.matches(Regex(pattern)) }
            .map { l -> Pair(l, l) }
            .toMap()
            .toPersistentMap(),
        childrenPattern = pattern,
        childrenLevel = level - 1
    )

suspend fun handleChildren(pattern: String, level: Int, response: SingleResponse): SingleResponse =
    if (level == 0)
        response
    else
        response.copy(children = crawl(toRequest(pattern, level, response)))

fun extractLinks(parentUrlSchema: String, body: String): PersistentList<String> =
    Jsoup.parse(body)
        .select("a")
        .map { e -> e.attr("href") }
        .map { url -> processParentUrlSchema(parentUrlSchema, url) }
        .toPersistentList()

fun processParentUrlSchema(parentUrlSchema: String, url: String): String =
    if (url.startsWith("//"))
        "$parentUrlSchema:$url"
    else
        url

suspend fun <T> merge(suspended: PersistentList<suspend () -> T>): PersistentList<T> =
    coroutineScope {
        suspended
            .map { f -> async { f.invoke() } }
            .fold(persistentListOf<T>()) { l: PersistentList<T>, d: Deferred<T> -> l.add(d.await()) }
    }