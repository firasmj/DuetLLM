package com.example.debate.stream;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debate")
@CrossOrigin(origins = "*")
public class DebateStreamController {

    private final StreamingOrchestrator orchestrator;

    public DebateStreamController(StreamingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/stream")
    public ResponseEntity<Map<String,Object>> start(@Valid @RequestBody DebateRequest req,
                                                    @RequestParam(value = "requestId", required = false) String requestId) {
        String rid = orchestrator.start(req.prompt(), req.rounds(), requestId);
        return ResponseEntity.ok(Map.of("ok", true, "requestId", rid));
    }
}
