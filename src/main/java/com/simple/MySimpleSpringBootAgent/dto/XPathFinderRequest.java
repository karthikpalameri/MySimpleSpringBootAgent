package com.simple.MySimpleSpringBootAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for finding XPath of elements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XPathFinderRequest {

    /**
     * The HTML content of the page
     */
    private String htmlContent;

    /**
     * Base64 encoded screenshot of the page
     */
    private String screenshotBase64;

    /**
     * Description of the element to find (e.g., "search box", "login button")
     */
    private String elementDescription;

    /**
     * The old/failed XPath that didn't work
     */
    private String failedXPath;

    /**
     * Current page URL
     */
    private String pageUrl;

    /**
     * Additional context or hints
     */
    private String additionalContext;
}
