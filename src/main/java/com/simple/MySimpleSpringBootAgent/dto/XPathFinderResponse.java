package com.simple.MySimpleSpringBootAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for XPath finder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XPathFinderResponse {

    /**
     * The suggested XPath(s) to use
     */
    private List<String> suggestedXPaths;

    /**
     * Primary/recommended XPath
     */
    private String primaryXPath;

    /**
     * Alternative CSS selectors
     */
    private List<String> alternativeSelectors;

    /**
     * Explanation of why this XPath was suggested
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
     * Any warnings or suggestions
     */
    private String warnings;

    /**
     * Full LLM response for debugging
     */
    private String llmResponse;
}
