package com.example.debate.stream;

import jakarta.validation.constraints.NotBlank;

public record DebateRequest(@NotBlank String prompt, Integer rounds) {}
