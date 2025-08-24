package com.example.debate.stream;

import com.example.debate.client.GeminiStreamClient;
import com.example.debate.client.OpenAiStreamClient;
import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import com.example.debate.service.JudgeService;
import com.example.debate.model.Prompts;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StreamingOrchestrator {

        private final SimpMessagingTemplate broker;
        private final OpenAiStreamClient gpt;
        private final GeminiStreamClient gemini;
        private final JudgeService judge;

        public StreamingOrchestrator(SimpMessagingTemplate broker,
                        OpenAiStreamClient gpt,
                        GeminiStreamClient gemini,
                        JudgeService judge) {
                this.broker = broker;
                this.gpt = gpt;
                this.gemini = gemini;
                this.judge = judge;
        }

        public String start(String prompt, Integer rounds, String requestId) {
                String rid = (requestId != null && !requestId.isBlank()) ? requestId : UUID.randomUUID().toString();
                emitStatus(rid, "started", "received prompt");

                String sys = Prompts.systemPrompt("generalist");
                var chatA = new LlmChat(List.of(new LlmMessage("system", sys), new LlmMessage("user", prompt)));
                var chatB = new LlmChat(List.of(new LlmMessage("system", sys), new LlmMessage("user", prompt)));

                StringBuilder aBuf = new StringBuilder();
                StringBuilder bBuf = new StringBuilder();
                AtomicInteger ixA = new AtomicInteger(0);
                AtomicInteger ixB = new AtomicInteger(0);

                Mono<Void> streamA = gpt.streamTokens(chatA, LlmParams.defaults())
                                .doOnNext(tok -> {
                                        System.out.println("GPT token: " + tok); // Add this line
                                        aBuf.append(tok);
                                        emitToken(rid, "gpt", tok, ixA.getAndIncrement());
                                })
                                .then();

                Mono<Void> streamB = gemini.streamTokens(chatB, LlmParams.defaults())
                                .doOnNext(tok -> {
                                        bBuf.append(tok);
                                        emitToken(rid, "gemini", tok, ixB.getAndIncrement());
                                })
                                .then();

                Mono.when(streamA, streamB)
                                .then(judge.synthesize(prompt, aBuf.toString(), bBuf.toString()))
                                .subscribe(
                                                synth -> {
                                                        var result = Map.of(
                                                                        "status", "no_consensus",
                                                                        "final_answer", synth,
                                                                        "alternatives", List.of(
                                                                                        Map.of("model", "gpt", "answer",
                                                                                                        aBuf.toString()),
                                                                                        Map.of("model", "gemini",
                                                                                                        "answer",
                                                                                                        bBuf.toString())));
                                                        emitFinal(rid, result);
                                                        emitDone(rid);
                                                },
                                                err -> emitError(rid, err.getMessage()));

                return rid;
        }

        private void emitToken(String rid, String src, String token, int ix) {
                broker.convertAndSend("/topic/stream/" + rid,
                                new StreamEvents.Token("token", rid, src, token, ix));
        }

        private void emitStatus(String rid, String stage, String detail) {
                broker.convertAndSend("/topic/stream/" + rid,
                                new StreamEvents.Status("status", rid, stage, detail));
        }

        private void emitFinal(String rid, Object result) {
                broker.convertAndSend("/topic/stream/" + rid,
                                new StreamEvents.FinalResult("final", rid, result));
        }

        private void emitDone(String rid) {
                broker.convertAndSend("/topic/stream/" + rid,
                                new StreamEvents.Done("done", rid));
        }

        private void emitError(String rid, String message) {
                broker.convertAndSend("/topic/stream/" + rid,
                                new StreamEvents.Error("error", rid, message));
        }
}
