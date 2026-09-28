package com.algopoolja.quiz.domain;

import java.util.List;

public record DifficultyMetadata(
        String algorithm,
        String estimatedLevel,
        String timeComplexity,
        String spaceComplexity,
        List<String> requiredTechniques
) {
}
