package com.simple.MySimpleSpringBootAgent.aiservice;

import com.simple.MySimpleSpringBootAgent.dto.XPathAnalysisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * AI Service for finding XPath and CSS selectors
 * This interface is automatically implemented by langchain4j at runtime
 */
@AiService
public interface XPathFinderAI {

    /**
     * Find XPath and CSS selectors for a web element based on HTML analysis
     *
     * @param elementDescription Human-readable description of the element to find
     * @param htmlContent The HTML source code to analyze
     * @param failedXPath The XPath that failed (optional, for context)
     * @param pageUrl The URL of the page (optional, for context)
     * @return Structured analysis result with XPath and CSS selector suggestions
     */
    @SystemMessage("""
            You are an expert web automation engineer specializing in creating robust XPath and CSS selectors.

            Your expertise includes:
            - Analyzing HTML structure to identify elements
            - Creating reliable, maintainable selectors
            - Understanding best practices for element location strategies
            - Recognizing dynamic vs static attributes

            When providing selectors:
            1. Prefer ID-based selectors when available (most reliable)
            2. Use specific attributes that are unlikely to change
            3. Avoid positional selectors unless necessary
            4. Provide both XPath and CSS selector alternatives
            5. Explain your reasoning clearly

            Always respond with valid JSON matching the expected structure.
            """)
    @UserMessage("""
            I need to locate a web element with the following details:

            Element Description: {{elementDescription}}

            HTML Content:
            ```html
            {{htmlContent}}
            ```

            Please analyze the HTML and provide:
            1. The best XPath selector (preferring ID-based if available)
            2. Alternative XPath selectors (at least 2-3 options)
            3. The best CSS selector
            4. Alternative CSS selectors
            5. Your confidence level (0-100)
            6. A brief explanation of your recommendations
            7. Any warnings about the selectors' reliability

            Focus on finding the element that matches: {{elementDescription}}
            """)
    XPathAnalysisResult findSelectors(
            @V("elementDescription") String elementDescription,
            @V("htmlContent") String htmlContent
    );

}
