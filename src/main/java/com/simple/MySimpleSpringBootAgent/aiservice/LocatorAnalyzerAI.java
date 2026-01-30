package com.simple.MySimpleSpringBootAgent.aiservice;

import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * AI Service for analyzing failed Selenium locators and suggesting alternatives
 * Supports all Selenium locator types: ID, Name, ClassName, TagName, LinkText, CSS, XPath
 *
 * Now enhanced with tool calling via DomQueryTools - LLM can interactively query the DOM
 */
@AiService(tools = "domQueryTools")
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
            You are an expert Selenium automation engineer with DOM querying capabilities.

            AVAILABLE TOOLS (use these to analyze the HTML):
            - findByXPath(xpath): Test if XPath works, see what it matches
            - findByCss(selector): Test CSS selectors
            - findById(id): Check if element exists by ID
            - getAllInteractiveElements(): List all clickable/input elements
            - findByText(text): Search by visible text content
            - findByAttribute(name, value): Search by any attribute

            ANALYSIS STRATEGY:
            1. First, try the failed locator directly with findByXPath() or findByCss()
               - Understand WHY it failed (syntax error vs no matches)

            2. If no matches, call getAllInteractiveElements() to see what's available
               - This shows all buttons, inputs, links with their IDs/classes/names

            3. Search for the target element using:
               - Element description (e.g., "search box" → findByText("Search"))
               - Common attributes (findById(), findByAttribute("data-testid"))

            4. Test multiple alternative locators before suggesting them:
               - Prefer: By.id (most stable)
               - Then: By.name, By.cssSelector (semantic attributes)
               - Last resort: By.xpath (brittle)

            5. ALWAYS test your suggestions with tools before returning

            HANDLE COMPLEX LOCATORS:
            - XPath functions: //button[contains(text(),'Login')]
            - CSS pseudo-selectors: div.nav > a:first-child
            - Nested paths: //div[@id='menu']//button[2]
            - Dynamic IDs: Try fuzzy matching if exact ID fails

            SELENIUM LOCATOR TYPES:
            - By.id - Most reliable when IDs exist (preferred)
            - By.name - Good for form elements
            - By.className - When class uniquely defines element
            - By.linkText / partialLinkText - For hyperlinks only
            - By.cssSelector - Flexible and performant
            - By.xpath - Most powerful but brittle

            Return structured JSON with:
            - recommendedLocatorType: Best strategy (ID, CSS, XPATH, etc.)
            - recommendedLocator: The actual locator string
            - Alternative locators for all applicable types
            - Explanation: WHY this strategy is best
            - Warnings: Any concerns (brittleness, dynamic IDs, etc.)
            """)
    @UserMessage("""
            A Selenium test failed with this locator:
            Locator: {{failedLocator}}
            Element description: {{elementDescription}}
            Page URL: {{pageUrl}}

            Please:
            1. Use tools to understand why the locator failed
            2. Find the target element using available tools
            3. Test multiple alternative locators
            4. Return the best suggestions with explanations

            Provide alternatives across ALL Selenium types:
            - By.id (most preferred)
            - By.name (for forms)
            - By.className (unique classes)
            - By.linkText/partialLinkText (for links)
            - By.cssSelector (flexible)
            - By.xpath (least preferred, brittle)

            Return structured JSON with all applicable locator types filled in.
            """)
    LocatorAnalysisResult analyzeLocator(
            @V("failedLocator") String failedLocator,
            @V("elementDescription") String elementDescription,
            @V("pageUrl") String pageUrl);
}
