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
import java.util.stream.Collectors;

@Service
public class ReactorBasedCrawler {

    public Mono<CrawlingResponse> crawl(CrawlingRequest request) {
        return Flux.concat(
                request
                    .urls()
                    .entrySet()
                    .stream()
                    .map(entry ->
                            parseResponse(extractScheme(entry.getValue()), performRequest(entry.getValue()))
                            .flatMap(singleResponse -> handleChildren(request.childrenPattern(), request.childrenLevel(), singleResponse))
                            .map(singleResponse -> Map.entry(entry.getKey(), singleResponse)))
                    .collect(Collectors.toList()))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(CrawlingResponse::new);
    }

    private Mono<ResponseEntity<String>> performRequest(String url) {
        return WebClient.create().get().uri(url).retrieve().toEntity(String.class);
    }

    private Mono<SingleResponse> handleChildren(String pattern, Integer level, SingleResponse response) {
        return level == 0 ?
                Mono.just(response) :
                crawl(toRequest(pattern, level, response))
                        .map(crawlingResponse -> new SingleResponse(
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

    private Mono<SingleResponse> parseResponse(String schema, Mono<ResponseEntity<String>> responseMono) {
        return responseMono
                .map(r -> new SingleResponse(
                        Optional.of(r.getStatusCodeValue()),
                        r.getStatusCode().getReasonPhrase(),
                        r.getHeaders().toSingleValueMap(),
                        Optional.of(r.getBody()),
                        extractLinks(schema, r.getBody()),
                        Optional.empty()
                ))
                .doOnError(e -> new SingleResponse(
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

}
