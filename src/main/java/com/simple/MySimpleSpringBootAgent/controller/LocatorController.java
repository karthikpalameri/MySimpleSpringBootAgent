package com.simple.MySimpleSpringBootAgent.controller;

import com.simple.MySimpleSpringBootAgent.aiservice.LocatorAnalyzerAI;
import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResult;
import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResponse;
import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisRequest;
import com.simple.MySimpleSpringBootAgent.service.HtmlPreprocessor;
import com.simple.MySimpleSpringBootAgent.service.LocatorRequestValidator;
import com.simple.MySimpleSpringBootAgent.service.LocatorResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for general Selenium locator analysis
 * Supports ALL Selenium locator types: ID, Name, ClassName, TagName, LinkText, CSS, XPath
 * Analyzes failed locators and suggests alternatives using AI
 * 
 * Refactored to follow Single Responsibility Principle - only handles HTTP concerns
 */
@Slf4j
@RestController
@RequestMapping("/api/locators")
@RequiredArgsConstructor
public class LocatorController {

    private final LocatorAnalyzerAI locatorAnalyzerAI;
    private final HtmlPreprocessor htmlPreprocessor;
    private final LocatorRequestValidator requestValidator;
    private final LocatorResponseMapper responseMapper;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Locator Analysis Service is running");
    }

    /**
     * Test endpoint with simple HTML demonstrating all Selenium locator types
     * GET /api/locators/test
     */
    @GetMapping("/test")
    public ResponseEntity<LocatorAnalysisResponse> testLocatorAnalysis() {
        String testHtml = """
                <html>
                <head><title>Test Page</title></head>
                <body>
                    <div id="header">
                        <input id="searchBox" type="text" name="q" placeholder="Search..." class="search-input" data-testid="search-input"/>
                        <button id="searchBtn" class="btn btn-primary" name="searchButton">Search</button>
                        <a href="/help" class="help-link">Help Center</a>
                    </div>
                    <div id="content">
                        <p class="welcome-text">Welcome to our website</p>
                        <form name="loginForm">
                            <input type="text" name="username" id="usernameField" class="form-control"/>
                            <input type="password" name="password" id="passwordField" class="form-control"/>
                            <button type="submit">Login</button>
                        </form>
                    </div>
                </body>
                </html>
                """;

        LocatorAnalysisRequest request = LocatorAnalysisRequest.builder()
                .htmlContent(testHtml)
                .locator("//*[@id='wrongSearchBox']")  // Failed locator
                .pageUrl("https://example.com")
                .build();

        return analyzeLocator(request);
    }

    /**
     * Analyze failed Selenium locator and find alternatives across ALL locator types
     * POST /api/locators/analyze
     *
     * Supports all Selenium locator types:
     * - By.id (most reliable)
     * - By.name (good for forms)
     * - By.className (when unique)
     * - By.tagName (for groups)
     * - By.linkText / partialLinkText (hyperlinks only)
     * - By.cssSelector (flexible, performant)
     * - By.xpath (powerful but brittle)
     */
    @PostMapping("/analyze")
    public ResponseEntity<LocatorAnalysisResponse> analyzeLocator(@RequestBody LocatorAnalysisRequest request) {
        log.info("Received locator analysis request for locator: {}", request.getLocator());

        try {
            // Validate request using dedicated validator
            if (!requestValidator.isValid(request)) {
                String errorMessage = String.join("; ", requestValidator.validate(request));
                return ResponseEntity.badRequest()
                        .body(responseMapper.createErrorResponse(errorMessage));
            }

            // Apply HTML preprocessing to handle large payloads
            String preprocessedHtml = htmlPreprocessor.preprocessHtml(
                    request.getHtmlContent(),
                    request.getLocator()
            );

            log.info("HTML preprocessed: {} -> {} bytes",
                    request.getHtmlContent().length(),
                    preprocessedHtml.length());

            // Call AI service with general locator analysis
            String pageUrl = request.getPageUrl() != null ? request.getPageUrl() : "";
            LocatorAnalysisResult aiResult = locatorAnalyzerAI.analyzeLocator(
                    request.getLocator(),
                    preprocessedHtml,
                    pageUrl
            );

            // Convert to response format using dedicated mapper
            LocatorAnalysisResponse response = responseMapper.toResponse(aiResult);

            log.info("Locator analysis: elementFound={}, recommended={} (type={}), confidence={}",
                    response.getElementFound(),
                    response.getRecommendedLocator(),
                    response.getRecommendedLocatorType(),
                    response.getConfidence());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing locator analysis request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(responseMapper.createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
}