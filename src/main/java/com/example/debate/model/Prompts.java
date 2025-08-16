package com.example.debate.model;

public class Prompts {

  public static String systemPrompt(String domain) {
    return """
You are a senior %s expert collaborating with another expert model to produce a precise, correct answer.
Respond with this structure:
1) Direct Answer
2) Key Assumptions
3) Step-by-step Reasoning (concise)
4) Tests/Validation
5) Confidence (0..1)
Keep code runnable and minimal. Avoid speculation. Cite trade-offs.""".formatted(domain);
  }

  public static String critiquePrompt(String opponent, String yours) {
    return """
The other model proposed:
---
%s
---
Compare to your previous answer:
---
%s
---
- List Agreements and Disagreements
- Revise your answer if needed
- If differences are minor, propose a single consensus answer.
Return JSON with fields: agreements[], disagreements[], revised_answer, consensus_proposal?""".formatted(opponent, yours);
  }

  public static String judgeEquivalence(String userPrompt, String a, String b) {
    return """
Given Answer A and Answer B, decide if they are substantively equivalent for the user's question:
USER PROMPT:
%s
A:
%s
B:
%s

Return strict JSON: { "equivalent": true|false, "notes": "...", "better": "A|B|tie" }""".formatted(userPrompt, a, b);
  }

  public static String judgeSynthesis(String userPrompt, String a, String b) {
    return """
Synthesize the best possible single answer for the user's question, combining A and B where appropriate.
Be concise, correct, and include key trade-offs.

USER PROMPT:
%s
A:
%s
B:
%s
""".formatted(userPrompt, a, b);
  }
}
