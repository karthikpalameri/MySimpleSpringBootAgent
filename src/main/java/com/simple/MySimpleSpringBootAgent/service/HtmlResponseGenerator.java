package com.simple.MySimpleSpringBootAgent.service;

import org.springframework.stereotype.Service;

/**
 * Service responsible for generating HTML response messages
 * Follows Single Responsibility Principle - only handles response generation
 */
@Service
public class HtmlResponseGenerator {

    /**
     * Create response when no matching elements found
     *
     * @param locator The locator that had no matches
     * @return HTML response message
     */
    public String createNoMatchResponse(String locator) {
        return String.format(
                "<!DOCTYPE html><html><head><title>No Match</title></head><body>" +
                "<p>No matching elements found for locator: <code>%s</code></p>" +
                "<p>The locator may be incorrect or the element may not exist in the page.</p>" +
                "</body></html>",
                escapeHtml(locator)
        );
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
