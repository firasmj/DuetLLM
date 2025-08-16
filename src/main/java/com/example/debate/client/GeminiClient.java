package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import org.springframework.beans.factory.annotation.Value;
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
  private final String baseUrl;
  private final String apiKey;

  public GeminiClient(
      WebClient.Builder builder,
      @Value("${gemini.baseUrl}") String baseUrl,
      @Value("${gemini.apiKey}") String apiKey,
      @Value("${gemini.model}") String model) {
    this.model = model;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.webClient = builder
        .baseUrl(baseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public Mono<LlmMessage> respond(LlmChat chat, LlmParams params) {
    // Minimal "contents" mapping
    var contents = chat.messages().stream()
        .map(m -> Map.of("role", m.role().equals("system") ? "user" : m.role(),
            "parts", java.util.List.of(Map.of("text", m.content()))))
        .toList();
    var payload = Map.of("contents", contents);

    return this.webClient.post()
        .body(BodyInserters.fromValue(payload))
        .retrieve()
        .bodyToMono(Map.class)
        .map(resp -> {
          try {
            var candidates = (java.util.List) resp.get("candidates");
            var c0 = (java.util.Map) candidates.get(0);
            var content = (java.util.Map) c0.get("content");
            var parts = (java.util.List) content.get("parts");
            var p0 = (java.util.Map) parts.get(0);
            String text = (String) p0.get("text");
            return new LlmMessage("assistant", text);
          } catch (Exception e) {
            return new LlmMessage("assistant", "ERROR parsing Gemini response: " + e.getMessage());
          }
        });
  }
}
