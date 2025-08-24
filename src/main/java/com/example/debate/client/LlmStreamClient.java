package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmParams;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LlmStreamClient {
    Flux<String> streamTokens(LlmChat chat, LlmParams params);
    Mono<String> completeOnce(LlmChat chat, LlmParams params);
}
