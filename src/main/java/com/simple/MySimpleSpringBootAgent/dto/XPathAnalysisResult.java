package com.simple.MySimpleSpringBootAgent.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured AI response for XPath/CSS selector analysis
 * This POJO will be automatically populated by langchain4j from LLM response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XPathAnalysisResult {

    @Description("The best and most reliable XPath selector found")
    private String primaryXPath;

    @Description("List of alternative XPath selectors in order of reliability")
    private List<String> alternativeXPaths;

    @Description("The best CSS selector for the element")
    private String primaryCssSelector;

    @Description("List of alternative CSS selectors")
    private List<String> alternativeCssSelectors;

    @Description("Overall confidence that the element was correctly identified, from 0 to 100")
    private Integer confidence;

    @Description("Brief explanation of how the element was identified and why the primary selector is recommended")
    private String explanation;

    @Description("Whether the element was successfully found in the HTML")
    private Boolean elementFound;

    @Description("Any warnings, caveats, or notes about the selectors")
    private String warnings;
}
