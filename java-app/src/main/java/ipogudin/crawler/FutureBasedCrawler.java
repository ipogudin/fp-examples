package ipogudin.crawler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class FutureBasedCrawler {

    public CompletableFuture<CrawlingResponse> crawl(CrawlingRequest request) {
        return concat(
                request
                    .urls()
                    .entrySet()
                    .stream()
                    .map(entry ->
                            parseResponse(extractScheme(entry.getValue()), performRequest(entry.getValue()))
                            .thenCompose(singleResponse -> handleChildren(request.childrenPattern(), request.childrenLevel(), singleResponse))
                            .thenApply(singleResponse -> Map.entry(entry.getKey(), singleResponse)))
                    .collect(Collectors.toList()))
            .thenApply(l -> l.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .thenApply(CrawlingResponse::new);
    }

    private CompletableFuture<ResponseEntity<String>> performRequest(String url) {
        return WebClient.create().get().uri(url).retrieve().toEntity(String.class).toFuture();
    }

    private CompletableFuture<SingleResponse> handleChildren(String pattern, Integer level, SingleResponse response) {
        return level == 0 ?
                CompletableFuture.completedFuture(response) :
                crawl(toRequest(pattern, level, response))
                        .thenApply(crawlingResponse -> new SingleResponse(
                            response.code(),
                            response.message(),
                            response.headers(),
                            response.body(),
                            response.links(),
                            Optional.of(crawlingResponse)
                        ));
    }

    private CrawlingRequest toRequest(String pattern, Integer level, SingleResponse response) {
        return new CrawlingRequest(
                response.links().stream()
                        .filter(l -> l.matches(pattern))
                        .map(l -> Map.entry(l, l))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                pattern,
                level - 1);
    }

    private CompletableFuture<SingleResponse> parseResponse(String schema, CompletableFuture<ResponseEntity<String>> responseFuture) {
        return responseFuture
                .thenApply(r -> new SingleResponse(
                        Optional.of(r.getStatusCodeValue()),
                        r.getStatusCode().getReasonPhrase(),
                        r.getHeaders().toSingleValueMap(),
                        Optional.of(r.getBody()),
                        extractLinks(schema, r.getBody()),
                        Optional.empty()
                ))
                .exceptionally(e -> new SingleResponse(
                        Optional.empty(),
                        e.getMessage(),
                        Collections.emptyMap(),
                        Optional.of(ExceptionUtils.getStackTrace(e)),
                        Collections.emptyList(),
                        Optional.empty()
                ));
    }

    private List<String> extractLinks(String schema, String body) {
        return Jsoup.parse(body)
                .select("a")
                .stream()
                .map(e -> e.attr("href"))
                .map(url -> (url.startsWith("//") ? schema + ":" + url : url))
                .collect(Collectors.toList());
    }

    private static String extractScheme(String url) {
        try {
            return new URI(url).getScheme();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> CompletableFuture<List<T>> concat(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

}
