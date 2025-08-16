package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import reactor.core.publisher.Mono;

public interface LlmClient {
  Mono<LlmMessage> respond(LlmChat chat, LlmParams params);
}
