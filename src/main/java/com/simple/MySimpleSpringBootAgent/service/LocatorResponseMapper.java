package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResponse;
import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for mapping LocatorAnalysisResult to LocatorAnalysisResponse
 * Follows Single Responsibility Principle - only handles DTO mapping
 */
@Service
@RequiredArgsConstructor
public class LocatorResponseMapper {

    private final LocatorResponseFormatter formatter;

    /**
     * Convert AI result to response format mapping all Selenium locator types
     *
     * @param aiResult The AI analysis result
     * @return The mapped response DTO
     */
    public LocatorAnalysisResponse toResponse(LocatorAnalysisResult aiResult) {
        List<String> alternativeXPaths = new ArrayList<>();
        List<String> alternativeCss = new ArrayList<>();

        // Collect XPath alternatives
        if (aiResult.getPrimaryXPath() != null) {
            alternativeXPaths.add(aiResult.getPrimaryXPath());
        }
        if (aiResult.getAlternativeXPaths() != null) {
            alternativeXPaths.addAll(aiResult.getAlternativeXPaths());
        }

        // Collect CSS alternatives
        if (aiResult.getPrimaryCssSelector() != null) {
            alternativeCss.add(aiResult.getPrimaryCssSelector());
        }
        if (aiResult.getAlternativeCssSelectors() != null) {
            alternativeCss.addAll(aiResult.getAlternativeCssSelectors());
        }

        return LocatorAnalysisResponse.builder()
                // Recommended strategy
                .recommendedLocatorType(aiResult.getRecommendedLocatorType())
                .recommendedLocator(aiResult.getRecommendedLocator())

                // All Selenium locator types
                .byId(aiResult.getPrimaryId())
                .byName(aiResult.getPrimaryName())
                .byClassName(aiResult.getPrimaryClassName())
                .byTagName(aiResult.getPrimaryTagName())
                .byLinkText(aiResult.getPrimaryLinkText())
                .byPartialLinkText(aiResult.getPrimaryPartialLinkText())
                .primaryCssSelector(aiResult.getPrimaryCssSelector())
                .alternativeCssSelectors(alternativeCss)
                .primaryXPath(aiResult.getPrimaryXPath())
                .alternativeXPaths(alternativeXPaths)

                // Metadata
                .confidence(aiResult.getConfidence())
                .explanation(aiResult.getExplanation())
                .elementFound(aiResult.getElementFound())
                .warnings(aiResult.getWarnings())
                .llmResponse(formatter.formatAIResult(aiResult))
                .build();
    }

    /**
     * Create error response
     *
     * @param message The error message
     * @return Error response DTO
     */
    public LocatorAnalysisResponse createErrorResponse(String message) {
        return LocatorAnalysisResponse.builder()
                .elementFound(false)
                .explanation(message)
                .confidence(0)
                .build();
    }
}
