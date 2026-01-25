package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorHints;
import com.simple.MySimpleSpringBootAgent.dto.LocatorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LocatorParserService implements LocatorParser {

    // XPath patterns
    private static final Pattern XPATH_ID_PATTERN = Pattern.compile("@id\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern XPATH_CLASS_PATTERN = Pattern.compile("@class\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern XPATH_ATTR_PATTERN = Pattern.compile("@([a-zA-Z-]+)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern XPATH_TEXT_PATTERN = Pattern.compile("text\\(\\)\\s*=\\s*['\"]([^'\"]+)['\"]|contains\\s*\\(\\s*text\\(\\)\\s*,\\s*['\"]([^'\"]+)['\"]\\)");
    private static final Pattern XPATH_TAG_PATTERN = Pattern.compile("//([a-zA-Z][a-zA-Z0-9]*)|/([a-zA-Z][a-zA-Z0-9]*)");

    // CSS patterns
    private static final Pattern CSS_ID_PATTERN = Pattern.compile("#([a-zA-Z][a-zA-Z0-9_-]*)");
    private static final Pattern CSS_CLASS_PATTERN = Pattern.compile("\\.([a-zA-Z][a-zA-Z0-9_-]*)");
    private static final Pattern CSS_ATTR_PATTERN = Pattern.compile("\\[([a-zA-Z-]+)\\s*=\\s*['\"]([^'\"]+)['\"]\\]|\\[([a-zA-Z-]+)\\]");
    private static final Pattern CSS_TAG_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9]*)|[>\\s+~]\\s*([a-zA-Z][a-zA-Z0-9]*)");

    public LocatorHints parseLocator(String locator) {
        if (locator == null || locator.trim().isEmpty()) {
            log.warn("Empty or null locator provided");
            return LocatorHints.builder()
                    .type(LocatorType.UNKNOWN)
                    .rawLocator(locator)
                    .build();
        }

        LocatorType type = detectLocatorType(locator);
        log.debug("Detected locator type: {} for locator: {}", type, locator);

        return switch (type) {
            case XPATH -> parseXPath(locator);
            case CSS_SELECTOR -> parseCss(locator);
            case UNKNOWN -> LocatorHints.builder()
                    .type(LocatorType.UNKNOWN)
                    .rawLocator(locator)
                    .build();
        };
    }

    private LocatorType detectLocatorType(String locator) {
        locator = locator.trim();

        // XPath indicators
        if (locator.startsWith("//") || locator.startsWith("/") ||
                locator.contains("[@") || locator.contains("[contains(") ||
                locator.contains("text()") || locator.matches(".*\\[\\d+\\].*")) {
            return LocatorType.XPATH;
        }

        // CSS indicators
        if (locator.startsWith("#") || locator.startsWith(".") ||
                locator.contains(">") || locator.contains("+") || locator.contains("~") ||
                locator.matches(".*\\[.*=.*\\].*") || locator.matches("^[a-zA-Z][a-zA-Z0-9]*.*")) {
            return LocatorType.CSS_SELECTOR;
        }

        return LocatorType.UNKNOWN;
    }

    /**
     * DRY helper: Extract values from pattern matcher
     * Eliminates code duplication in parseXPath() and parseCss()
     */
    private List<String> extractMatches(Pattern pattern, String input, int groupIndex) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group(groupIndex);
            if (match != null && !match.isEmpty()) {
                matches.add(match);
            }
        }
        log.trace("Pattern {} found {} matches in input", pattern.pattern(), matches.size());
        return matches;
    }

    /**
     * DRY helper: Extract tag names handling multiple groups
     */
    private List<String> extractTags(Pattern pattern, String input) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String tag = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (tag != null && !tag.isEmpty() && !"*".equals(tag)) {
                tags.add(tag.toLowerCase());
            }
        }
        return tags;
    }

    private LocatorHints parseXPath(String xpath) {
        log.debug("Parsing XPath locator: {}", xpath);

        // Extract IDs using DRY helper
        List<String> ids = extractMatches(XPATH_ID_PATTERN, xpath, 1);

        // Extract and split class values
        List<String> rawClasses = extractMatches(XPATH_CLASS_PATTERN, xpath, 1);
        List<String> classes = new ArrayList<>();
        for (String classValue : rawClasses) {
            for (String cls : classValue.split("\\s+")) {
                if (!cls.isEmpty()) {
                    classes.add(cls);
                }
            }
        }

        // Extract attributes (excluding id and class)
        Map<String, String> attributes = new HashMap<>();
        Matcher attrMatcher = XPATH_ATTR_PATTERN.matcher(xpath);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1);
            String attrValue = attrMatcher.group(2);
            if (!"id".equals(attrName) && !"class".equals(attrName)) {
                attributes.put(attrName, attrValue);
                log.trace("Extracted attribute: {}={}", attrName, attrValue);
            }
        }

        // Extract text content
        String textContent = null;
        Matcher textMatcher = XPATH_TEXT_PATTERN.matcher(xpath);
        if (textMatcher.find()) {
            textContent = textMatcher.group(1) != null ? textMatcher.group(1) : textMatcher.group(2);
            log.trace("Extracted text content: {}", textContent);
        }

        // Extract tag names using DRY helper
        List<String> tagNames = extractTags(XPATH_TAG_PATTERN, xpath);

        LocatorHints hints = LocatorHints.builder()
                .type(LocatorType.XPATH)
                .rawLocator(xpath)
                .ids(ids)
                .classes(classes)
                .tagNames(tagNames)
                .attributes(attributes)
                .textContent(textContent)
                .build();

        log.debug("Parsed XPath hints: ids={}, classes={}, tags={}, attrs={}, text={}",
                ids.size(), classes.size(), tagNames.size(), attributes.size(), textContent != null);

        return hints;
    }

    private LocatorHints parseCss(String css) {
        log.debug("Parsing CSS selector: {}", css);

        // Extract IDs using DRY helper
        List<String> ids = extractMatches(CSS_ID_PATTERN, css, 1);

        // Extract classes using DRY helper
        List<String> classes = extractMatches(CSS_CLASS_PATTERN, css, 1);

        // Extract attributes
        Map<String, String> attributes = new HashMap<>();
        Matcher attrMatcher = CSS_ATTR_PATTERN.matcher(css);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1) != null ? attrMatcher.group(1) : attrMatcher.group(3);
            String attrValue = attrMatcher.group(2);
            if (attrName != null) {
                attributes.put(attrName, attrValue != null ? attrValue : "");
                log.trace("Extracted attribute: {}={}", attrName, attrValue);
            }
        }

        // Extract tag names using DRY helper
        List<String> tagNames = extractTags(CSS_TAG_PATTERN, css);

        LocatorHints hints = LocatorHints.builder()
                .type(LocatorType.CSS_SELECTOR)
                .rawLocator(css)
                .ids(ids)
                .classes(classes)
                .tagNames(tagNames)
                .attributes(attributes)
                .build();

        log.debug("Parsed CSS hints: ids={}, classes={}, tags={}, attrs={}",
                ids.size(), classes.size(), tagNames.size(), attributes.size());

        return hints;
    }
}
