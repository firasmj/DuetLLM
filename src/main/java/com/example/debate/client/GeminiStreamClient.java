package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GeminiStreamClient implements LlmStreamClient {

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String defaultModel;

    public GeminiStreamClient(WebClient.Builder builder,
            @Value("${gemini.baseUrl}") String baseUrl,
            @Value("${gemini.apiKey}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("${gemini.timeoutMs:30000}") long timeoutMs) {
        this.defaultModel = model;
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Flux<String> streamTokens(LlmChat chat, LlmParams params) {

        Map<String, Object> body = new HashMap<>();
        // body {
        // "contents": [
        // {
        // "role": "user",
        // "parts": [
        // {
        // "text": "Explain how AI works"
        // }
        // ]
        // }
        // ]
        // } this is the body
        String model = params.model() == null ? defaultModel : params.model();
        List<Map<String, Object>> messages = chat.messages().stream()
                .map(m -> {
                    String role = m.role();
                    // Replace "system" with "model" for Gemini API compatibility
                    if ("system".equals(role)) {
                        role = "model";
                    }
                    return Map.of("role", role, "parts", List.of(Map.of("text", m.content())));
                })
                .collect(Collectors.toList());
        body.put("contents", messages);

        try {
            System.out.println("Gemini request body: " + mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return client.post()
                .uri("/models/{model}:streamGenerateContent?alt=sse", model)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(Throwable::printStackTrace)
                .doOnNext(line -> System.out.println("Gemini raw line: " + line))
                .takeUntil(s -> s.contains("[DONE]"))
                .filter(s -> !s.contains("[DONE]"))
                .concatMap(json -> {
                    try {
                        //gemini response is {"candidates": [{"content": {"parts": [{"text": " purpose/output:** Solve problems, create digital solutions (apps, systems).\n*   **Add value proposition:** Ensure efficiency, reliability, functionality, performance.\n*   **Draft and refine:** Combine these elements, ensuring conciseness and adherence"}],"role": "model"},"index": 0}],"usageMetadata": {"promptTokenCount": 90,"candidatesTokenCount": 195,"totalTokenCount": 432,"promptTokensDetails": [{"modality": "TEXT","tokenCount": 90}],"thoughtsTokenCount": 147},"modelVersion": "gemini-2.5-flash","responseId": "X4GraNftMbqwxN8PmryqwAI"}
                        Map<String, Object> data = mapper.readValue(json, Map.class);
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                            if (content != null) {
                                List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                                if (parts != null && !parts.isEmpty()) {
                                    String text = parts.get(0).get("text");
                                    if (text != null) {
                                        return Flux.just(text);
                                    }
                                }
                            }
                        }
                        return Flux.empty();
                    }catch (Exception e) {
                        e.printStackTrace();
                        return Flux.empty();
                    }
                });
    }

    @Override
    public Mono<String> completeOnce(LlmChat chat, LlmParams params) {
        // Placeholder: replace with real Gemini call if desired.
        String prompt = chat.messages().isEmpty() ? "" : chat.messages().get(chat.messages().size() - 1).content();
        String txt = "Gemini (simulated) response to: " + prompt;
        return Mono.just(txt);
    }
}
