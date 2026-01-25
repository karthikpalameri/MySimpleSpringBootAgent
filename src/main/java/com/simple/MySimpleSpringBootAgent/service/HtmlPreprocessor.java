package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.config.HtmlProcessingConfig;
import com.simple.MySimpleSpringBootAgent.dto.LocatorHints;
import com.simple.MySimpleSpringBootAgent.dto.ScoredElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Orchestrates multi-stage HTML preprocessing pipeline for local LLM optimization
 *
 * Pipeline stages:
 * 1. Locator Parsing - Extract semantic hints from XPath/CSS selectors
 * 2. Candidate Discovery - Find matching elements using multi-tier strategy
 * 3. DOM Pruning - Keep only relevant subtrees containing candidates
 * 4. Context Extraction - Limit depth, siblings, and nesting
 * 5. Minification - Apply HtmlCompressor for final size reduction
 *
 * Target: 90-97% size reduction (500KB → <50KB) for local LLM context windows
 * 
 * Refactored to follow Single Responsibility Principle - only handles pipeline orchestration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HtmlPreprocessor {

    private final HtmlProcessingConfig config;
    private final LocatorParser locatorParser;
    private final CandidateFinder candidateFinder;
    private final DomPruner domPruner;
    private final HtmlMinificationService minificationService;
    private final HtmlCleaningService htmlCleaningService;
    private final HtmlUtilityService htmlUtilityService;
    private final HtmlResponseGenerator htmlResponseGenerator;

    /**
     * Preprocess HTML content using intelligent 5-stage pipeline
     *
     * @param htmlContent The original HTML content from driver.getPageSource()
     * @param locator The failed XPath/CSS locator to analyze
     * @return Highly reduced HTML optimized for local LLM analysis
     */
    public String preprocessHtml(String htmlContent, String locator) {
        if (!StringUtils.hasText(htmlContent)) {
            log.warn("Empty or null HTML content provided");
            return htmlContent;
        }

        if (!StringUtils.hasText(locator)) {
            log.warn("Empty or null locator provided, using fallback processing");
            return fallbackProcessing(htmlContent);
        }

        int originalSize = htmlContent.length();
        log.info("Starting HTML preprocessing: {} bytes, locator: {}", originalSize, locator);

        long startTime = System.currentTimeMillis();

        try {
            // Early return if already small enough
            if (originalSize <= config.getEarlyReturnSize()) {
                log.info("HTML already small enough ({} bytes), skipping pipeline", originalSize);
                return minificationService.minify(htmlContent);
            }

            // Stage 1: Parse locator to extract semantic hints
            log.debug("Stage 1: Parsing locator");
            LocatorHints hints = locatorParser.parseLocator(locator);
            log.debug("Parsed hints: type={}, ids={}, classes={}, tags={}, text={}",
                    hints.getType(), hints.getIds().size(), hints.getClasses().size(),
                    hints.getTagNames().size(), hints.getTextContent());

            // Parse HTML document
            Document doc = Jsoup.parse(htmlContent);

            // Remove noise elements first (scripts, styles, etc.)
            htmlCleaningService.removeNoiseElements(doc);

            // Stage 2: Find candidate elements using multi-tier matching
            log.debug("Stage 2: Finding candidate elements");
            List<ScoredElement> candidates = candidateFinder.findCandidates(doc, hints);
            log.info("Found {} candidate elements", candidates.size());

            if (candidates.isEmpty()) {
                log.warn("No candidates found, returning minimal context");
                return htmlResponseGenerator.createNoMatchResponse(locator);
            }

            // Log top candidates for debugging
            for (int i = 0; i < Math.min(3, candidates.size()); i++) {
                ScoredElement scored = candidates.get(i);
                log.debug("Candidate {}: score={}, reason={}, tag={}",
                        i + 1, scored.getScore(), scored.getMatchReason(),
                        scored.getElement().tagName());
            }

            // Stage 3: Prune DOM tree to relevant subtrees
            log.debug("Stage 3: Pruning DOM tree");
            Document pruned = domPruner.pruneToRelevantSubtree(doc, candidates);
            String prunedHtml = pruned.html();
            int prunedSize = prunedHtml.length();
            log.debug("After pruning: {} bytes ({}% reduction)",
                    prunedSize, String.format("%.1f", ((originalSize - prunedSize) * 100.0 / originalSize)));

            // Stage 4: Context extraction (already done by pruning service)

            // Stage 5: Minification
            log.debug("Stage 5: Applying minification");
            String minified = minificationService.minify(prunedHtml);
            int finalSize = minified.length();

            long elapsed = System.currentTimeMillis() - startTime;
            int reduction = originalSize - finalSize;
            double percentReduction = (reduction * 100.0) / originalSize;

            log.info("Preprocessing complete: {} → {} bytes ({} bytes reduced, {}% reduction) in {}ms",
                    originalSize, finalSize, reduction, String.format("%.1f", percentReduction), elapsed);

            return minified;

        } catch (Exception e) {
            log.error("Error during HTML preprocessing: {}", e.getMessage(), e);
            return fallbackProcessing(htmlContent);
        }
    }

    /**
     * Fallback processing when pipeline fails or locator is missing
     */
    private String fallbackProcessing(String htmlContent) {
        log.warn("Using fallback processing");

        try {
            Document doc = Jsoup.parse(htmlContent);
            htmlCleaningService.removeNoiseElements(doc);

            // Truncate to max size if needed
            String html = doc.html();
            if (html.length() > config.getMaxOutputSize()) {
                html = htmlUtilityService.truncateSafely(html, config.getMaxOutputSize());
            }

            return minificationService.minify(html);
        } catch (Exception e) {
            log.error("Fallback processing failed: {}", e.getMessage());
            return htmlUtilityService.truncateSafely(htmlContent, config.getMaxOutputSize());
        }
    }

    /**
     * Check if HTML content needs preprocessing
     *
     * @param htmlContent The HTML content to check
     * @return true if preprocessing is needed
     */
    public boolean needsPreprocessing(String htmlContent) {
        return StringUtils.hasText(htmlContent) && htmlContent.length() > config.getEarlyReturnSize();
    }
}