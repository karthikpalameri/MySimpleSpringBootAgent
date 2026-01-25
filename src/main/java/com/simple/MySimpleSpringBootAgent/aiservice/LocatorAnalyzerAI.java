package com.simple.MySimpleSpringBootAgent.aiservice;

import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * AI Service for analyzing failed Selenium locators and suggesting alternatives
 * Supports all Selenium locator types: ID, Name, ClassName, TagName, LinkText, CSS, XPath
 */
@AiService
public interface LocatorAnalyzerAI {

    /**
     * Analyze a failed Selenium locator and suggest alternatives across all locator types
     *
     * @param failedLocator The locator that failed (any Selenium type)
     * @param htmlContent The HTML source code to analyze
     * @param pageUrl The URL of the page (optional, for context)
     * @return Structured analysis with suggestions for ID, Name, Class, Tag, LinkText, CSS, and XPath
     */
    @SystemMessage("""
            You are an expert Selenium automation engineer specializing in element location strategies.

            Your expertise includes ALL Selenium locator types:
            - By.id - Most reliable when IDs exist (preferred)
            - By.name - Good for form elements
            - By.className - When class uniquely defines element
            - By.tagName - For groups of elements
            - By.linkText / partialLinkText - For hyperlinks only
            - By.cssSelector - Flexible and performant
            - By.xpath - Most powerful but brittle

            Best Practices:
            1. ALWAYS prefer By.id if a stable ID exists (most reliable)
            2. For forms, use By.name when available
            3. Use By.className when the class is unique and stable
            4. Reserve By.linkText for hyperlinks with stable text
            5. Use By.cssSelector for flexible, performant selectors
            6. Only use By.xpath as last resort (powerful but brittle)
            7. Avoid dynamic attributes (data-reactid, generated IDs)
            8. Prefer semantic attributes (aria-label, data-testid, role)

            When analyzing a failed locator:
            - Understand what element was intended
            - Identify the best locator strategy for that element type
            - Provide alternatives across MULTIPLE locator types
            - Explain tradeoffs (reliability vs maintenance vs performance)
            - Warn about brittle selectors (absolute XPath, index-based)

            IMPORTANT: Fill in ALL applicable fields in the response. If an ID exists, populate primaryId.
            If it's a link, populate primaryLinkText. Provide CSS and XPath as fallbacks.

            Always respond with valid JSON matching the expected structure.
            """)
    @UserMessage("""
            A Selenium test failed because this locator didn't work:
            Locator: {{failedLocator}}
            Page URL: {{pageUrl}}

            HTML Context:
            ```html
            {{htmlContent}}
            ```

            Analyze the HTML and provide alternative locators across ALL Selenium types:
            1. Check if the element has an ID (By.id) - MOST PREFERRED
            2. Check if it has a name attribute (By.name) - good for forms
            3. Check for unique class names (By.className)
            4. For links, extract link text (By.linkText, By.partialLinkText)
            5. Provide robust CSS selectors (By.cssSelector)
            6. Provide XPath as fallback (By.xpath) - LEAST PREFERRED

            Return structured JSON with:
            - recommendedLocatorType: The BEST strategy for this element
            - recommendedLocator: The actual locator string for the recommended type
            - All applicable locator types filled in (id, name, className, linkText, css, xpath)
            - Explanation: WHY this strategy is best for this element
            - Warnings: Any concerns (e.g., "XPath is brittle", "ID may be dynamic")
            """)
    LocatorAnalysisResult analyzeLocator(
            @V("failedLocator") String failedLocator,
            @V("htmlContent") String htmlContent,
            @V("pageUrl") String pageUrl);
}
