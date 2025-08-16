package com.example.debate.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ApiModels {

  public record DebateRequest(@NotBlank String prompt, Integer rounds) {}
  public record AltAnswer(String model, String answer) {}

  public static class DebateResult {
    public String status; // "consensus" | "no_consensus"
    public String final_answer;
    public List<AltAnswer> alternatives;
    public String synthesis;
    public int rounds_used;
    public String judge_notes;

    public static DebateResult consensus(String answer, int rounds, String notes) {
      DebateResult r = new DebateResult();
      r.status = "consensus";
      r.final_answer = answer;
      r.rounds_used = rounds;
      r.judge_notes = notes;
      return r;
    }
    public static DebateResult noConsensus(String aModel, String a, String bModel, String b, String synth, int rounds, String notes) {
      DebateResult r = new DebateResult();
      r.status = "no_consensus";
      r.alternatives = java.util.List.of(new AltAnswer(aModel, a), new AltAnswer(bModel, b));
      r.synthesis = synth;
      r.rounds_used = rounds;
      r.judge_notes = notes;
      return r;
    }
  }

  public record LlmMessage(String role, String content) {}
  public record LlmChat(java.util.List<LlmMessage> messages) {}
  public record LlmParams(int maxTokens, double temperature, String model){}
}
