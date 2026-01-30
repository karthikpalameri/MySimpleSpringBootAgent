package com.simple.MySimpleSpringBootAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for general Selenium locator analysis
 * Returns suggestions for all Selenium locator types
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocatorAnalysisResponse {

    /**
     * Recommended locator type (ID, NAME, CLASS_NAME, TAG_NAME, LINK_TEXT, PARTIAL_LINK_TEXT, CSS_SELECTOR, XPATH)
     */
    private String recommendedLocatorType;

    /**
     * The recommended locator string to use
     */
    private String recommendedLocator;

    /**
     * ID-based locator (By.id) - most reliable when available
     */
    private String byId;

    /**
     * Name-based locator (By.name) - good for forms
     */
    private String byName;

    /**
     * Class name locator (By.className) - when class uniquely defines element
     */
    private String byClassName;

    /**
     * Tag name (By.tagName) - for groups of elements
     */
    private String byTagName;

    /**
     * Link text (By.linkText) - for hyperlinks (exact match)
     */
    private String byLinkText;

    /**
     * Partial link text (By.partialLinkText) - for hyperlinks (partial match)
     */
    private String byPartialLinkText;

    /**
     * Primary CSS selector (By.cssSelector) - flexible and performant
     */
    private String primaryCssSelector;

    /**
     * Alternative CSS selectors
     */
    private List<String> alternativeCssSelectors;

    /**
     * Primary XPath (By.xpath) - most powerful but brittle
     */
    private String primaryXPath;

    /**
     * Alternative XPath selectors
     */
    private List<String> alternativeXPaths;

    /**
     * Explanation of why the recommended locator is best
     */
    private String explanation;

    /**
     * Confidence score (0-100)
     */
    private Integer confidence;

    /**
     * Whether the element was found in the HTML
     */
    private Boolean elementFound;

    /**
     * Any warnings or suggestions (e.g., "Avoid this XPath, it's brittle")
     */
    private String warnings;

    /**
     * Full LLM response for debugging
     */
    private String llmResponse;
}
