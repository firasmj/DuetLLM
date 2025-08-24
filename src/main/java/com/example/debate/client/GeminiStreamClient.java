package com.example.debate.client;

import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmParams;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class GeminiStreamClient implements LlmStreamClient {

    @Override
    public Flux<String> streamTokens(LlmChat chat, LlmParams params) {
        // Simulate token stream by emitting words from a one-shot completion
        return completeOnce(chat, params)
                .flatMapMany(txt -> {
                    String[] parts = txt.split(" ");
                    AtomicInteger i = new AtomicInteger();
                    return Flux.fromStream(Stream.of(parts))
                            .map(w -> w + " ")
                            .delayElements(Duration.ofMillis(60))
                            .onBackpressureDrop();
                });
    }

    @Override
    public Mono<String> completeOnce(LlmChat chat, LlmParams params) {
        // Placeholder: replace with real Gemini call if desired.
        String prompt = chat.messages().isEmpty() ? "" : chat.messages().get(chat.messages().size()-1).content();
        String txt = "Gemini (simulated) response to: " + prompt;
        return Mono.just(txt);
    }
}
