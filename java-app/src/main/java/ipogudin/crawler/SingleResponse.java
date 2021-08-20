package ipogudin.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SingleResponse(
        @JsonProperty("code")
        Optional<Integer> code,
        @JsonProperty("message")
        String message,
        @JsonProperty("headers")
        Map<String, String> headers,
        @JsonProperty("body")
        Optional<String> body,
        @JsonProperty("links")
        List<String> links,
        @JsonProperty("children")
        Optional<CrawlingResponse> children) {}
