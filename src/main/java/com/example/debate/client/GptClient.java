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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptClient implements LlmClient {

  private final WebClient webClient;
  private final String defaultModel;

  public GptClient(
      WebClient.Builder builder,
      @Value("${openai.baseUrl}") String baseUrl,
      @Value("${openai.apiKey}") String apiKey,
      @Value("${openai.model}") String model) {
    this.defaultModel = model;
    this.webClient = builder
        .baseUrl(baseUrl + "/chat/completions")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public Mono<LlmMessage> respond(LlmChat chat, LlmParams params) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("model", params.model() != null ? params.model() : this.defaultModel);
    payload.put("temperature", params.temperature());
    payload.put("max_tokens", params.maxTokens());
    List<Map<String, String>> msgs = chat.messages().stream()
        .map(m -> Map.of("role", m.role(), "content", m.content()))
        .toList();
    payload.put("messages", msgs);

    return this.webClient.post()
        .body(BodyInserters.fromValue(payload))
        .retrieve()
        .bodyToMono(Map.class)
        .map(resp -> {
          try {
            List choices = (List) resp.get("choices");
            Map choice0 = (Map) choices.get(0);
            Map msg = (Map) choice0.get("message");
            String content = (String) msg.get("content");
            return new LlmMessage("assistant", content);
          } catch (Exception e) {
            return new LlmMessage("assistant", "ERROR parsing OpenAI response: " + e.getMessage());
          }
        });
  }
}
