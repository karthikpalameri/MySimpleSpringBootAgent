package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Service responsible for formatting AI results for display/debugging
 * Follows Single Responsibility Principle - only handles response formatting
 */
@Service
public class LocatorResponseFormatter {

    /**
     * Format AI result for logging/debugging showing all locator types
     *
     * @param result The AI analysis result
     * @return Formatted string representation
     */
    public String formatAIResult(LocatorAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Locator Analysis Result ===\n");
        sb.append(String.format("Recommended: %s = %s\n",
                result.getRecommendedLocatorType(),
                result.getRecommendedLocator()));
        sb.append(String.format("Confidence: %d%%\n", result.getConfidence()));

        if (result.getPrimaryId() != null) {
            sb.append(String.format("By.id: %s\n", result.getPrimaryId()));
        }
        if (result.getPrimaryName() != null) {
            sb.append(String.format("By.name: %s\n", result.getPrimaryName()));
        }
        if (result.getPrimaryClassName() != null) {
            sb.append(String.format("By.className: %s\n", result.getPrimaryClassName()));
        }
        if (result.getPrimaryLinkText() != null) {
            sb.append(String.format("By.linkText: %s\n", result.getPrimaryLinkText()));
        }
        if (result.getPrimaryCssSelector() != null) {
            sb.append(String.format("By.cssSelector: %s\n", result.getPrimaryCssSelector()));
        }
        if (result.getPrimaryXPath() != null) {
            sb.append(String.format("By.xpath: %s\n", result.getPrimaryXPath()));
        }

        sb.append(String.format("\nExplanation: %s\n", result.getExplanation()));
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            sb.append(String.format("Warnings: %s\n", result.getWarnings()));
        }

        return sb.toString();
    }
}
