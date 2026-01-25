package com.simple.MySimpleSpringBootAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for analyzing failed locators and finding alternatives
 * Supports XPath, CSS selectors, and other locator strategies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocatorAnalysisRequest {

    /**
     * The HTML content of the page (will be preprocessed to handle large payloads)
     */
    private String htmlContent;

    /**
     * Base64 encoded screenshot of the page (optional, for visual context)
     */
    private String screenshotBase64;

    /**
     * The locator that failed (XPath, CSS selector, etc.)
     */
    private String locator;

    /**
     * Current page URL (optional, for context)
     */
    private String pageUrl;
}