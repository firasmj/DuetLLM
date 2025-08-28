package com.example.debate.controller;

import com.example.debate.model.ApiModels.DebateRequest;
import com.example.debate.model.ApiModels.DebateResult;
import com.example.debate.service.OrchestratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/debate")
@CrossOrigin(origins = "*")
public class DebateController {

  private final OrchestratorService orchestrator;
  private final double similarityThreshold;

  public DebateController(OrchestratorService orchestrator,
      @Value("${app.similarityThreshold:0.88}") double similarityThreshold) {
    this.orchestrator = orchestrator;
    this.similarityThreshold = similarityThreshold;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<DebateResult> run(@Valid @RequestBody DebateRequest req) {
    return orchestrator.debate(req.prompt(), req.rounds(), similarityThreshold);
  }
}
