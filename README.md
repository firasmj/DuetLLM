# Two-LLM Debate Orchestrator (Spring Boot)

A ready-to-run Spring Boot service that orchestrates a short debate between **GPT** and **Gemini**, attempts to reach a **consensus**, and returns the result.

> ⚡ The frontend repository: [Next.js LLM Debate Console](https://github.com/firasmj/LLM-Debate-Client)

## Quick Start

1. **Set API keys**
   ```bash
   export OPENAI_API_KEY=sk-...   # required
   export GEMINI_API_KEY=...      # required
   ```

2. **Build & run**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Call the API**
   ```bash
   curl -X POST http://localhost:8080/api/debate      -H 'Content-Type: application/json'      -d '{ "prompt": "Write a function in Python that reverses a linked list.", "rounds": 2 }'
   ```

## Related Repositories

- [Next.js LLM Debate Console](https://github.com/firasmj/LLM-Debate-Client)

## Endpoint
- `POST /api/debate`
  - Body: `{ "prompt": "your question", "rounds": 1..5 }`
  - Returns:
    ```json
    {
      "status": "consensus | no_consensus",
      "final_answer": "string (if consensus)",
      "alternatives": [{"model":"gpt","answer":"..."},{"model":"gemini","answer":"..."}],
      "synthesis": "string (if no_consensus)",
      "rounds_used": 2,
      "judge_notes": "model-produced JSON about equivalence check"
    }
    ```

## Notes
- Request/response mapping is intentionally minimal; adjust if vendor schemas change.
- The **judge** uses GPT by default. Switch to Gemini by changing `JudgeService` wiring.
- Add rate limiting, retries, timeouts, and logging for production.
