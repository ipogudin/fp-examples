package ipogudin.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CrawlingRequest(
        @JsonProperty("urls")
        Map<String, String> urls,
        @JsonProperty("childrenPattern")
        String childrenPattern,
        @JsonProperty("childrenLevel")
        Integer childrenLevel) {}
