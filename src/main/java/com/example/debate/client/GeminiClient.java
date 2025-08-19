package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeminiClient implements LlmClient {

  private final WebClient webClient;
  private final String model;

  public GeminiClient(
      WebClient.Builder builder,
      @Value("${gemini.baseUrl}") String baseUrl,
      @Value("${gemini.apiKey}") String apiKey,
      @Value("${gemini.model}") String model) {
    this.model = model;
    this.webClient = builder
        .baseUrl(baseUrl) // e.g. https://generativelanguage.googleapis.com/v1beta
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-goog-api-key", apiKey)
        .build();
  }

  @Override
  public Mono<LlmMessage> respond(LlmChat chat, LlmParams params) {
    // Map internal messages to Gemini "contents"
    List<Map<String, Object>> contents = chat.messages().stream()
        .map(m -> {
          String role = m.role();
          // Gemini expects "user" for prompts and "model" for responses
          if ("assistant".equals(role)) {
            role = "model";
          } else {
            role = "user"; // collapse system/user to user
          }
            return Map.of(
                "role", role,
                "parts", List.of(Map.of("text", m.content()))
            );
        })
        .toList();

    Map<String, Object> payload = Map.of("contents", contents);

    return this.webClient.post()
        .uri("/models/{model}:generateContent", model)
        .body(BodyInserters.fromValue(payload))
        .retrieve()
        .bodyToMono(Map.class)
        .map(resp -> {
          try {
            var candidates = (List<?>) resp.get("candidates");
            var c0 = (Map<?, ?>) candidates.get(0);
            var content = (Map<?, ?>) c0.get("content");
            var parts = (List<?>) content.get("parts");
            var p0 = (Map<?, ?>) parts.get(0);
            String text = (String) p0.get("text");
            return new LlmMessage("assistant", text);
          } catch (Exception e) {
            return new LlmMessage("assistant", "ERROR parsing Gemini response: " + e.getMessage());
          }
        });
  }
}
