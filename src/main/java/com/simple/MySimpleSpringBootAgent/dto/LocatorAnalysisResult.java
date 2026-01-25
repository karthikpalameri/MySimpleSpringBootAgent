package com.simple.MySimpleSpringBootAgent.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured AI response for general Selenium locator analysis
 * Supports all Selenium locator types: ID, Name, ClassName, TagName, LinkText, CSS, XPath
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocatorAnalysisResult {

    @Description("The best ID-based locator if available (By.id)")
    private String primaryId;

    @Description("The best name-based locator if available (By.name)")
    private String primaryName;

    @Description("The best class name locator if available (By.className)")
    private String primaryClassName;

    @Description("The best tag name if this is a group selection (By.tagName)")
    private String primaryTagName;

    @Description("The best link text for hyperlinks (By.linkText)")
    private String primaryLinkText;

    @Description("The best partial link text for hyperlinks (By.partialLinkText)")
    private String primaryPartialLinkText;

    @Description("The best and most reliable CSS selector found (By.cssSelector)")
    private String primaryCssSelector;

    @Description("List of alternative CSS selectors in order of reliability")
    private List<String> alternativeCssSelectors;

    @Description("The best XPath selector found (By.xpath)")
    private String primaryXPath;

    @Description("List of alternative XPath selectors in order of reliability")
    private List<String> alternativeXPaths;

    @Description("Overall confidence that the element was correctly identified, from 0 to 100")
    private Integer confidence;

    @Description("Brief explanation of which locator strategy is recommended and why")
    private String explanation;

    @Description("Whether the element was successfully found in the HTML")
    private Boolean elementFound;

    @Description("Recommended locator type to use: ID, NAME, CLASS_NAME, TAG_NAME, LINK_TEXT, PARTIAL_LINK_TEXT, CSS_SELECTOR, or XPATH")
    private String recommendedLocatorType;

    @Description("The actual locator string for the recommended type")
    private String recommendedLocator;

    @Description("Any warnings, caveats, or notes about the selectors (e.g., brittle XPath, dynamic IDs)")
    private String warnings;
}
