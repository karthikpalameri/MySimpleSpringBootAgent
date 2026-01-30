package com.simple.MySimpleSpringBootAgent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for HTML preprocessing pipeline
 * Follows DRY principle by centralizing all constants
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "html.processing")
public class HtmlProcessingConfig {

    // General size limits
    private int maxOutputSize = 51200; // 50KB
    private int earlyReturnSize = 51200; // Return early if already small

    // Candidate discovery settings
    private int maxCandidates = 5;
    private int tierOneScore = 100;
    private int tierTwoScore = 75;
    private int tierThreeScore = 50;
    private int tierFourScore = 25;

    // DOM pruning settings
    private int maxParentDepth = 3;
    private int maxSiblingCount = 2;
    private int maxChildDepth = 2;
    private int maxChildrenPreserved = 5;

    // Scoring boost values
    private int attributeMatchBoost = 10;
    private int idMatchBoost = 20;
    private int classMatchBoost = 5;

    // Text similarity thresholds
    private int exactMatchBonus = 10;
    private int containsMatchScore = 25;
    private int caseInsensitiveMatchPenalty = 5;
}
