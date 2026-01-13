package com.simple.MySimpleSpringBootAgent.controller;

import com.simple.MySimpleSpringBootAgent.aiservice.XPathFinderAI;
import com.simple.MySimpleSpringBootAgent.dto.XPathAnalysisResult;
import com.simple.MySimpleSpringBootAgent.dto.XPathFinderRequest;
import com.simple.MySimpleSpringBootAgent.dto.XPathFinderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for XPath finder service
 * Simplified using @AiService - no manual prompt building or parsing needed!
 */
@Slf4j
@RestController
@RequestMapping("/api/xpath")
@RequiredArgsConstructor
public class XPathFinderController {

    private final XPathFinderAI xpathFinderAI;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("XPath Finder Service is running");
    }

    /**
     * Find XPath for an element using AI
     * POST /api/xpath/find
     */
    @PostMapping("/find")
    public ResponseEntity<XPathFinderResponse> findXPath(@RequestBody XPathFinderRequest request) {
        log.info("Received XPath finder request for element: {}", request.getElementDescription());

        try {
            // Validate request
            if (request.getHtmlContent() == null || request.getHtmlContent().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("HTML content is required"));
            }

            if (request.getElementDescription() == null || request.getElementDescription().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Element description is required"));
            }

            // Call AI service - it handles everything!
            XPathAnalysisResult aiResult = xpathFinderAI.findSelectors(
                    request.getElementDescription(),
                    request.getHtmlContent()
            );

            // Convert to response format (for backward compatibility)
            XPathFinderResponse response = convertToResponse(aiResult);

            log.info("XPath finder response: elementFound={}, confidence={}, primaryXPath={}",
                    response.getElementFound(),
                    response.getConfidence(),
                    response.getPrimaryXPath());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing XPath finder request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint with simple HTML
     * GET /api/xpath/test
     */
    @GetMapping("/test")
    public ResponseEntity<XPathFinderResponse> testXPathFinder() {
        String testHtml = """
                <html>
                <head><title>Test Page</title></head>
                <body>
                    <div id="header">
                        <input id="searchBox" type="text" name="q" placeholder="Search..." class="search-input"/>
                        <button id="searchBtn" class="btn-primary">Search</button>
                    </div>
                    <div id="content">
                        <p>Welcome to our website</p>
                    </div>
                </body>
                </html>
                """;

        XPathAnalysisResult aiResult = xpathFinderAI.findSelectors("search box", testHtml);
        XPathFinderResponse response = convertToResponse(aiResult);

        return ResponseEntity.ok(response);
    }

    /**
     * Convert AI result to response format (for backward compatibility with existing clients)
     */
    private XPathFinderResponse convertToResponse(XPathAnalysisResult aiResult) {
        List<String> suggestedXPaths = new ArrayList<>();
        List<String> alternativeSelectors = new ArrayList<>();

        // Add primary XPath
        if (aiResult.getPrimaryXPath() != null) {
            suggestedXPaths.add(aiResult.getPrimaryXPath());
        }

        // Add alternative XPaths
        if (aiResult.getAlternativeXPaths() != null) {
            suggestedXPaths.addAll(aiResult.getAlternativeXPaths());
        }

        // Add CSS selectors as alternatives
        if (aiResult.getPrimaryCssSelector() != null) {
            alternativeSelectors.add(aiResult.getPrimaryCssSelector());
        }
        if (aiResult.getAlternativeCssSelectors() != null) {
            alternativeSelectors.addAll(aiResult.getAlternativeCssSelectors());
        }

        return XPathFinderResponse.builder()
                .primaryXPath(aiResult.getPrimaryXPath())
                .suggestedXPaths(suggestedXPaths)
                .alternativeSelectors(alternativeSelectors)
                .confidence(aiResult.getConfidence())
                .explanation(aiResult.getExplanation())
                .elementFound(aiResult.getElementFound())
                .warnings(aiResult.getWarnings())
                .llmResponse(formatAIResult(aiResult))
                .build();
    }

    /**
     * Create error response
     */
    private XPathFinderResponse createErrorResponse(String message) {
        return XPathFinderResponse.builder()
                .elementFound(false)
                .explanation(message)
                .confidence(0)
                .build();
    }

    /**
     * Format AI result for logging/debugging
     */
    private String formatAIResult(XPathAnalysisResult result) {
        return String.format(
                "Primary XPath: %s\nConfidence: %d%%\nExplanation: %s",
                result.getPrimaryXPath(),
                result.getConfidence(),
                result.getExplanation()
        );
    }
}
