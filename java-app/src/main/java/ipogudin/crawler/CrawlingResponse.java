package ipogudin.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CrawlingResponse(
        @JsonProperty("responses")
        Map<String, SingleResponse> responses) {}
