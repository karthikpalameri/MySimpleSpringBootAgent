package com.simple.MySimpleSpringBootAgent.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service responsible for HTML utility operations
 * Follows Single Responsibility Principle - handles truncation and token counting
 */
@Service
public class HtmlUtilityService {

    /**
     * Safely truncate HTML content at tag boundary
     *
     * @param html The HTML content to truncate
     * @param maxLength Maximum length
     * @return Truncated HTML
     */
    public String truncateSafely(String html, int maxLength) {
        if (html.length() <= maxLength) {
            return html;
        }

        int tagEnd = html.lastIndexOf("<", maxLength);
        if (tagEnd > maxLength * 0.8) {
            return html.substring(0, tagEnd) + "\n<!-- Truncated -->";
        }

        return html.substring(0, maxLength) + "\n<!-- Truncated -->";
    }

    /**
     * Count tokens in text (simple word-based approximation)
     *
     * @param text The text to count tokens in
     * @return Number of tokens
     */
    public int countTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return text.split("\\s+").length;
    }
}
