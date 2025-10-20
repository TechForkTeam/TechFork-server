package com.techfork.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techfork.domain.post.enums.EDifficultyLevel;

import java.util.List;

public record ExtractedMetadata(
        @JsonProperty("main_topics")
        List<String> mainTopics,

        @JsonProperty("tech_stack")
        TechStack techStack,

        @JsonProperty("difficulty")
        String difficulty
) {
    public record TechStack(
            @JsonProperty("languages")
            List<String> languages,

            @JsonProperty("frameworks")
            List<String> frameworks,

            @JsonProperty("tools")
            List<String> tools
    ) {}

    public EDifficultyLevel getDifficultyLevel() {
        if (difficulty == null) {
            return EDifficultyLevel.INTERMEDIATE;
        }

        return switch (difficulty.toLowerCase()) {
            case "beginner" -> EDifficultyLevel.BEGINNER;
            case "advanced" -> EDifficultyLevel.ADVANCED;
            default -> EDifficultyLevel.INTERMEDIATE;
        };
    }

    public String getMainTopicsAsString() {
        return mainTopics != null ? String.join(", ", mainTopics) : "";
    }

    public String getLanguagesAsString() {
        return techStack != null && techStack.languages() != null
                ? String.join(", ", techStack.languages())
                : "";
    }

    public String getFrameworksAsString() {
        return techStack != null && techStack.frameworks() != null
                ? String.join(", ", techStack.frameworks())
                : "";
    }

    public String getToolsAsString() {
        return techStack != null && techStack.tools() != null
                ? String.join(", ", techStack.tools())
                : "";
    }
}
