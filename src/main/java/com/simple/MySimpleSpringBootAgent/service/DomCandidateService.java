package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.config.HtmlProcessingConfig;
import com.simple.MySimpleSpringBootAgent.dto.LocatorHints;
import com.simple.MySimpleSpringBootAgent.dto.LocatorType;
import com.simple.MySimpleSpringBootAgent.dto.ScoredElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of CandidateFinder using multi-tier matching strategy
 * Follows SOLID principles with externalized configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomCandidateService implements CandidateFinder {

    private final HtmlProcessingConfig config;

    public List<ScoredElement> findCandidates(Document doc, LocatorHints hints) {
        log.debug("Finding candidates for locator hints: {}", hints);

        List<ScoredElement> allCandidates = new ArrayList<>();

        // Try direct locator execution first
        if (hints.getType() == LocatorType.XPATH) {
            try {
                Elements directMatches = doc.selectXpath(hints.getRawLocator());
                for (Element el : directMatches) {
                    allCandidates.add(new ScoredElement(el, config.getTierOneScore(), "Direct XPath match"));
                }
                log.debug("Direct XPath execution found {} matches", directMatches.size());
            } catch (Exception e) {
                log.debug("Direct XPath execution failed: {}", e.getMessage());
            }
        } else if (hints.getType() == LocatorType.CSS_SELECTOR) {
            try {
                Elements directMatches = doc.select(hints.getRawLocator());
                for (Element el : directMatches) {
                    allCandidates.add(new ScoredElement(el, config.getTierOneScore(), "Direct CSS match"));
                }
                log.debug("Direct CSS execution found {} matches", directMatches.size());
            } catch (Exception e) {
                log.debug("Direct CSS execution failed: {}", e.getMessage());
            }
        }

        // If direct match found and confident, return early
        if (!allCandidates.isEmpty() && allCandidates.size() <= 3) {
            log.debug("Returning {} direct matches", allCandidates.size());
            return limitCandidates(allCandidates);
        }

        // Tier 1: ID and unique attributes
        allCandidates.addAll(tierOneMatch(doc, hints));

        // Tier 2: Semantic attributes
        allCandidates.addAll(tierTwoMatch(doc, hints));

        // Tier 3: Class and tag matching
        allCandidates.addAll(tierThreeMatch(doc, hints));

        // Tier 4: Text content similarity
        if (hints.getTextContent() != null && !hints.getTextContent().isEmpty()) {
            allCandidates.addAll(tierFourMatch(doc, hints));
        }

        // Remove duplicates and sort by score
        List<ScoredElement> uniqueCandidates = deduplicateAndSort(allCandidates);

        log.debug("Found {} total unique candidates", uniqueCandidates.size());
        return limitCandidates(uniqueCandidates);
    }

    private List<ScoredElement> tierOneMatch(Document doc, LocatorHints hints) {
        List<ScoredElement> candidates = new ArrayList<>();

        // Match by ID with null safety
        for (String id : hints.getIds()) {
            if (id == null || id.isEmpty()) continue;
            Element el = doc.getElementById(id);
            if (el != null) {
                candidates.add(new ScoredElement(el, config.getTierOneScore(), "ID match: " + id));
                log.trace("Found element by ID: {}", id);
            }
        }

        // Match by unique attributes with null safety and escaping
        for (Map.Entry<String, String> attr : hints.getAttributes().entrySet()) {
            if (attr.getKey() == null || attr.getValue() == null) continue;
            try {
                String selector = String.format("[%s='%s']",
                    escapeCssSelector(attr.getKey()), escapeCssSelector(attr.getValue()));
                Elements matches = doc.select(selector);
                if (matches.size() == 1 && matches.first() != null) {
                    candidates.add(new ScoredElement(matches.first(), config.getTierOneScore(),
                            "Unique attribute: " + attr.getKey()));
                    log.trace("Found unique element by attribute: {}={}", attr.getKey(), attr.getValue());
                }
            } catch (Exception e) {
                log.warn("Error selecting by attribute {}={}: {}", attr.getKey(), attr.getValue(), e.getMessage());
            }
        }

        log.debug("Tier 1 found {} candidates", candidates.size());
        return candidates;
    }

    private List<ScoredElement> tierTwoMatch(Document doc, LocatorHints hints) {
        List<ScoredElement> candidates = new ArrayList<>();

        // Semantic attributes: aria-label, placeholder, name, title
        String[] semanticAttrs = {"aria-label", "placeholder", "name", "title", "alt"};

        for (Map.Entry<String, String> attr : hints.getAttributes().entrySet()) {
            if (Arrays.asList(semanticAttrs).contains(attr.getKey())) {
                String selector = String.format("[%s*='%s']", attr.getKey(), attr.getValue());
                Elements matches = doc.select(selector);
                for (Element el : matches) {
                    candidates.add(new ScoredElement(el, config.getTierTwoScore(),
                            "Semantic attr: " + attr.getKey()));
                }
            }
        }

        log.debug("Tier 2 found {} candidates", candidates.size());
        return candidates;
    }

    private List<ScoredElement> tierThreeMatch(Document doc, LocatorHints hints) {
        List<ScoredElement> candidates = new ArrayList<>();

        // Match by tag + class combinations
        for (String tag : hints.getTagNames()) {
            for (String cls : hints.getClasses()) {
                String selector = tag + "." + cls;
                Elements matches = doc.select(selector);
                for (Element el : matches) {
                    int score = scoreElement(el, hints);
                    candidates.add(new ScoredElement(el, score, "Tag+Class: " + selector));
                }
            }
        }

        // Match by class only if no tag+class matches
        if (candidates.isEmpty()) {
            for (String cls : hints.getClasses()) {
                Elements matches = doc.select("." + cls);
                for (Element el : matches) {
                    int score = scoreElement(el, hints);
                    candidates.add(new ScoredElement(el, score, "Class: " + cls));
                }
            }
        }

        // Match by tag only if still no matches
        if (candidates.isEmpty()) {
            for (String tag : hints.getTagNames()) {
                Elements matches = doc.select(tag);
                for (Element el : matches) {
                    int score = scoreElement(el, hints);
                    candidates.add(new ScoredElement(el, score, "Tag: " + tag));
                }
            }
        }

        log.debug("Tier 3 found {} candidates", candidates.size());
        return candidates;
    }

    private List<ScoredElement> tierFourMatch(Document doc, LocatorHints hints) {
        List<ScoredElement> candidates = new ArrayList<>();
        String targetText = hints.getTextContent().toLowerCase();

        // Find elements with similar text content
        Elements allElements = doc.select("*");
        for (Element el : allElements) {
            String elementText = el.ownText().toLowerCase();
            if (elementText.contains(targetText) || targetText.contains(elementText)) {
                int similarity = calculateTextSimilarity(targetText, elementText);
                if (similarity > 0) {
                    candidates.add(new ScoredElement(el, similarity, "Text similarity"));
                }
            }
        }

        log.debug("Tier 4 found {} candidates", candidates.size());
        return candidates;
    }

    private int scoreElement(Element el, LocatorHints hints) {
        int score = config.getTierThreeScore();

        // Boost score if element has matching attributes
        for (Map.Entry<String, String> attr : hints.getAttributes().entrySet()) {
            if (attr.getValue() != null && attr.getValue().equals(el.attr(attr.getKey()))) {
                score += config.getAttributeMatchBoost();
            }
        }

        // Boost if has matching ID
        if (!el.id().isEmpty() && hints.getIds().contains(el.id())) {
            score += config.getIdMatchBoost();
        }

        // Boost if has matching classes
        for (String cls : hints.getClasses()) {
            if (cls != null && el.hasClass(cls)) {
                score += config.getClassMatchBoost();
            }
        }

        return Math.min(score, config.getTierOneScore() - 1);
    }

    private int calculateTextSimilarity(String text1, String text2) {
        if (text1.isEmpty() || text2.isEmpty()) {
            return 0;
        }

        // Simple contains-based similarity using config
        if (text1.equals(text2)) {
            return config.getTierFourScore() + config.getExactMatchBonus();
        } else if (text1.contains(text2) || text2.contains(text1)) {
            return config.getContainsMatchScore();
        } else if (text1.toLowerCase().contains(text2.toLowerCase()) ||
                text2.toLowerCase().contains(text1.toLowerCase())) {
            return config.getTierFourScore() - config.getCaseInsensitiveMatchPenalty();
        }

        return 0;
    }

    private List<ScoredElement> deduplicateAndSort(List<ScoredElement> candidates) {
        // Use a map to keep highest score for each element
        Map<Element, ScoredElement> elementMap = new HashMap<>();

        for (ScoredElement scored : candidates) {
            Element el = scored.getElement();
            if (!elementMap.containsKey(el) || elementMap.get(el).getScore() < scored.getScore()) {
                elementMap.put(el, scored);
            }
        }

        List<ScoredElement> unique = new ArrayList<>(elementMap.values());
        Collections.sort(unique);
        return unique;
    }

    private List<ScoredElement> limitCandidates(List<ScoredElement> candidates) {
        if (candidates.size() <= config.getMaxCandidates()) {
            return candidates;
        }

        log.debug("Limiting candidates from {} to {}", candidates.size(), config.getMaxCandidates());
        return candidates.subList(0, config.getMaxCandidates());
    }

    /**
     * Escape special characters in CSS selectors to prevent malformed queries
     * KISS principle: Simple escaping for common special chars
     */
    private String escapeCssSelector(String value) {
        if (value == null) return "";
        // Escape special CSS selector characters
        return value.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("[", "\\[")
                   .replace("]", "\\]");
    }
}
