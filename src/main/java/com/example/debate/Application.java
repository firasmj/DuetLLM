package com.example.debate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    String openAiKey = System.getenv("OPENAI_API_KEY");
    if (openAiKey != null && !openAiKey.isEmpty()) {
      System.setProperty("openai.apiKey", openAiKey);
    }
    String geminiKey = System.getenv("GEMINI_API_KEY");
    if (geminiKey != null && !geminiKey.isEmpty()) {
      System.setProperty("gemini.apiKey", geminiKey);
    }
    SpringApplication.run(Application.class, args);
  }
}
