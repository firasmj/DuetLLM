package com.example.debate.service;

import com.example.debate.client.LlmClient;
import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import com.example.debate.model.Prompts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class JudgeService {

  private final LlmClient judgeClient;

  public JudgeService(@Qualifier("gptClient") LlmClient gptClient) {
    // Use GPT by default as judge; change wiring if needed.
    this.judgeClient = gptClient;
  }

  public static record JudgeResult(boolean equivalent, String notes, String better) {}

  public Mono<JudgeResult> equivalent(String userPrompt, String a, String b) {
    var chat = new LlmChat(java.util.List.of(
        new LlmMessage("system", "You are a strict evaluator that answers only in JSON."),
        new LlmMessage("user", Prompts.judgeEquivalence(userPrompt, a, b))
    ));
    return judgeClient.respond(chat, new LlmParams(500, 0.0, null))
        .map(response -> {
          String resp = response.content();
          boolean eq = resp.toLowerCase().contains("\"equivalent\": true");
          String better = resp.contains("\"better\"") ? resp : "tie";
          return new JudgeResult(eq, resp, better);
        });
  }

  public Mono<String> selectBest(String a, String b) {
    return Mono.just(a.length() <= b.length() ? a : b);
  }

  public Mono<String> synthesize(String userPrompt, String a, String b) {
    var chat = new LlmChat(java.util.List.of(
        new LlmMessage("system", "You produce a concise, correct synthesis."),
        new LlmMessage("user", Prompts.judgeSynthesis(userPrompt, a, b))
    ));
    return judgeClient.respond(chat, new LlmParams(700, 0.2, null)).map(LlmMessage::content);
  }
}
