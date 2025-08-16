package com.example.debate.service;

import com.example.debate.client.LlmClient;
import com.example.debate.model.ApiModels;
import com.example.debate.model.ApiModels.DebateResult;
import com.example.debate.model.ApiModels.LlmChat;
import com.example.debate.model.ApiModels.LlmMessage;
import com.example.debate.model.ApiModels.LlmParams;
import com.example.debate.model.Prompts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
public class OrchestratorService {

  private final LlmClient gpt;
  private final LlmClient gemini;
  private final JudgeService judge;
  private final int maxRounds;

  public OrchestratorService(@Qualifier("gptClient") LlmClient gpt, @Qualifier("geminiClient") LlmClient gemini,
      JudgeService judge,
      @Value("${app.maxRounds:3}") int maxRounds) {
    this.gpt = gpt;
    this.gemini = gemini;
    this.judge = judge;
    this.maxRounds = maxRounds;
  }

  public Mono<DebateResult> debate(String userPrompt, Integer roundsNullable, double simThreshold) {
    int roundsMax = (roundsNullable == null) ? maxRounds : Math.max(1, Math.min(5, roundsNullable));

    var sys = Prompts.systemPrompt("coding");
    var chatA = new java.util.ArrayList<LlmMessage>(
        java.util.List.of(new LlmMessage("system", sys), new LlmMessage("user", userPrompt)));
    var chatB = new java.util.ArrayList<LlmMessage>(
        java.util.List.of(new LlmMessage("system", sys), new LlmMessage("user", userPrompt)));

    Mono<LlmMessage> aMsgMono = gpt.respond(new LlmChat(chatA), new LlmParams(900, 0.2, null));
    Mono<LlmMessage> bMsgMono = gemini.respond(new LlmChat(chatB), new LlmParams(900, 0.2, null));

    return Mono.zip(aMsgMono, bMsgMono)
        .flatMap(tuple -> {
          LlmMessage aMsg = tuple.getT1();
          LlmMessage bMsg = tuple.getT2();
          chatA.add(aMsg);
          chatB.add(bMsg);

          return debateRound(userPrompt, simThreshold, roundsMax, 1, chatA, chatB, aMsg, bMsg);
        });
  }

  private Mono<DebateResult> debateRound(String userPrompt, double simThreshold, int roundsMax, int round,
      ArrayList<LlmMessage> chatA, ArrayList<LlmMessage> chatB,
      LlmMessage aMsg, LlmMessage bMsg) {
    if (round > roundsMax) {
      return judge.synthesize(userPrompt, aMsg.content(), bMsg.content())
          .map(synth -> ApiModels.DebateResult.noConsensus("gpt", aMsg.content(), "gemini", bMsg.content(), synth,
              round - 1, "No consensus after rounds"));
    }

    return judge.equivalent(userPrompt, aMsg.content(), bMsg.content())
        .flatMap(judgeRes -> {
          if (judgeRes.equivalent()) {
            return judge.selectBest(aMsg.content(), bMsg.content())
                .map(best -> DebateResult.consensus(best, round, judgeRes.notes()));
          } else {
            String aCrit = Prompts.critiquePrompt(bMsg.content(), aMsg.content());
            String bCrit = Prompts.critiquePrompt(aMsg.content(), bMsg.content());
            chatA.add(new LlmMessage("user", aCrit));
            chatB.add(new LlmMessage("user", bCrit));

            Mono<LlmMessage> nextAMono = gpt.respond(new LlmChat(chatA), new LlmParams(900, 0.2, null));
            Mono<LlmMessage> nextBMono = gemini.respond(new LlmChat(chatB), new LlmParams(900, 0.2, null));

            return Mono.zip(nextAMono, nextBMono)
                .flatMap(tuple -> {
                  LlmMessage nextA = tuple.getT1();
                  LlmMessage nextB = tuple.getT2();
                  chatA.add(nextA);
                  chatB.add(nextB);
                  return debateRound(userPrompt, simThreshold, roundsMax, round + 1, chatA, chatB, nextA, nextB);
                });
          }
        });
  }
}
