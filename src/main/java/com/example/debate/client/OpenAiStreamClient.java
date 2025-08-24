package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAiStreamClient implements LlmStreamClient {

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String defaultModel;

    public OpenAiStreamClient(WebClient.Builder builder,
            @Value("${openai.baseUrl}") String baseUrl,
            @Value("${openai.apiKey}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.timeoutMs:30000}") long timeoutMs) {
        this.defaultModel = model;
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Flux<String> streamTokens(LlmChat chat, LlmParams params) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", params.model() == null ? defaultModel : params.model());
        // body.put("model", "gpt-4o-mini"); /// hardcoded model
        body.put("stream", true);
        if (params.maxTokens() != null)
            body.put("max_tokens", params.maxTokens());
        if (params.temperature() != null)
            body.put("temperature", params.temperature());
        List<Map<String, String>> messages = chat.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .collect(Collectors.toList());
        body.put("messages", messages);

        System.out.println("Request body: " + body);

        return client.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(Throwable::printStackTrace)
                .map(line -> line.trim())
                // .filter(line -> line.startsWith("data:"))
                //log lines
                // .doOnNext(line -> System.out.println("before trim: " + line))
                // .map(line -> line.substring(6).trim())
                //log lines
                // .doOnNext(line -> System.out.println("OpenAIStreamClient: " + line))
                .takeUntil(line -> line.equals("[DONE]"))
                .filter(line -> !line.equals("[DONE]"))
                .concatMap(json -> {
                    try {
                        System.out.println("Received json: " + json);
                        JsonNode root = mapper.readTree(json);
                        JsonNode delta = root.path("choices").get(0).path("delta");
                        JsonNode content = delta.get("content");
                        if (content != null && !content.isNull()) {
                            return Flux.just(content.asText());
                        } else {
                            return Flux.empty();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Flux.empty();
                    }
                });
    }

    @Override
    public Mono<String> completeOnce(LlmChat chat, LlmParams params) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", params.model() == null ? defaultModel : params.model());
        body.put("stream", false);
        if (params.maxTokens() != null)
            body.put("max_tokens", params.maxTokens());
        if (params.temperature() != null)
            body.put("temperature", params.temperature());
        List<Map<String, String>> messages = chat.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .collect(Collectors.toList());
        body.put("messages", messages);

        return client.post()
                .uri("/chat/completions")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(json -> {
                    try {
                        JsonNode root = mapper.readTree(json);
                        JsonNode content = root.path("choices").get(0).path("message").path("content");
                        return Mono.just(content.isMissingNode() ? "" : content.asText());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(60));
    }
}
