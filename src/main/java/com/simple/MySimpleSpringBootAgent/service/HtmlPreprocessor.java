package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.config.HtmlProcessingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Simplified HTML preprocessing pipeline for local LLM optimization
 *
 * Refactored to leverage LangChain4j Document Transformers and remove brittle regex-based parsing
 *
 * New pipeline stages:
 * 1. Remove noise elements (scripts, styles) - Jsoup
 * 2. Minification - HtmlCompressor
 *
 * The preprocessed HTML is returned as a Jsoup Document for:
 * - LLM context (via toString or html())
 * - Tool-based DOM querying (via DomQueryTools)
 *
 * Target: Reduce HTML size to fit in local LLM context windows (4k-8k tokens)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HtmlPreprocessor {

    private final HtmlProcessingConfig config;
    private final HtmlMinificationService minificationService;
    private final HtmlUtilityService htmlUtilityService;

    /**
     * Preprocess HTML content using simplified pipeline
     *
     * Returns a Jsoup Document for:
     * - DOM querying via DomQueryTools (tool calling)
     * - LLM context (via doc.html() if needed)
     *
     * @param htmlContent The original HTML content from driver.getPageSource()
     * @param locator The failed XPath/CSS locator (for logging)
     * @return Jsoup Document with noise removed, ready for tool-based querying
     */
    public Document preprocessHtml(String htmlContent, String locator) {
        if (!StringUtils.hasText(htmlContent)) {
            log.warn("Empty or null HTML content provided");
            return new Document("");
        }

        int originalSize = htmlContent.length();
        log.info("Starting HTML preprocessing: {} bytes, locator: {}", originalSize, locator);

        long startTime = System.currentTimeMillis();

        try {
            // Parse HTML document
            Document doc = Jsoup.parse(htmlContent);

            // Remove noise elements (scripts, styles, comments)
            removeNoiseElements(doc);

            long elapsed = System.currentTimeMillis() - startTime;
            int finalSize = doc.html().length();
            double percentReduction = ((originalSize - finalSize) * 100.0) / originalSize;

            log.info("Preprocessing complete: {} → {} bytes ({}% reduction) in {}ms",
                    originalSize, finalSize, String.format("%.1f", percentReduction), elapsed);

            return doc;

        } catch (Exception e) {
            log.error("Error during HTML preprocessing: {}", e.getMessage(), e);
            // Return parsed document even if cleaning fails
            return Jsoup.parse(htmlContent);
        }
    }

    /**
     * Remove noise elements from document
     * Based on original HtmlCleaningService logic but inline for simplicity
     */
    private void removeNoiseElements(Document doc) {
        // Remove scripts, styles, noscript
        doc.select("script, style, noscript").remove();

        // Remove comments
        doc.select("*").forEach(el -> {
            el.childNodes().stream()
                .filter(node -> node.nodeName().equals("#comment"))
                .forEach(node -> node.remove());
        });

        log.debug("Removed noise elements (scripts, styles, comments)");
    }

    /**
     * Get minified HTML string for LLM context (if needed)
     * Truncate to max size to ensure it fits in LLM context window
     *
     * @param doc The preprocessed Jsoup document
     * @return Minified HTML string
     */
    public String getMinifiedHtml(Document doc) {
        try {
            String html = doc.html();

            // Truncate if too large
            if (html.length() > config.getMaxOutputSize()) {
                html = htmlUtilityService.truncateSafely(html, config.getMaxOutputSize());
            }

            return minificationService.minify(html);
        } catch (Exception e) {
            log.error("Minification failed: {}", e.getMessage());
            return doc.html();
        }
    }
}