package ipogudin;

import ipogudin.crawler.FutureBasedCrawler;
import ipogudin.crawler.ReactorBasedCrawler;
import ipogudin.crawler.CrawlingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;


@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Autowired
    private ReactorBasedCrawler reactorBasedCrawler;

    @Autowired
    private FutureBasedCrawler futureBasedCrawler;

    @Bean
    public RouterFunction<?> router() {
        return RouterFunctions.route()
                .POST("/reactor-crawler", accept(MediaType.APPLICATION_JSON),
                        request -> request.bodyToMono(CrawlingRequest.class)
                            .flatMap(crawlingRequest -> reactorBasedCrawler.crawl(crawlingRequest))
                            .flatMap(body -> ServerResponse.ok().bodyValue(body)))
                .POST("/future-crawler", accept(MediaType.APPLICATION_JSON),
                        request -> request.bodyToMono(CrawlingRequest.class)
                                .flatMap(crawlingRequest -> Mono.fromFuture(futureBasedCrawler.crawl(crawlingRequest)))
                                .flatMap(body -> ServerResponse.ok().bodyValue(body)))
                .build();
    }

}
