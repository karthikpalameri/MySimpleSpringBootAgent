package com.simple.MySimpleSpringBootAgent.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * DOM Query Tools for LLM-based locator analysis
 * Provides @Tool methods that allow the LLM to interactively query the HTML DOM
 * Uses ThreadLocal for thread-safe concurrent request handling
 */
@Slf4j
@Component
public class DomQueryTools {

    private ThreadLocal<Document> currentDoc = new ThreadLocal<>();

    /**
     * Set the HTML document for this thread (thread-safe for concurrent requests)
     */
    public void setDocument(Document doc) {
        this.currentDoc.set(doc);
    }

    /**
     * Clear the document after processing
     */
    public void clearDocument() {
        this.currentDoc.remove();
    }

    @Tool("Find element by ID. Returns element details if found, 'Not found' otherwise.")
    public String findById(@P("The element ID to search for") String id) {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        Element el = doc.getElementById(id);
        if (el != null) {
            log.debug("Found element by ID: {}", id);
            return formatElement(el);
        }
        log.debug("Element not found by ID: {}", id);
        return "Not found";
    }

    @Tool("Find elements by CSS selector. Returns matching elements or error message.")
    public String findByCss(@P("CSS selector to match") String selector) {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        try {
            Elements els = doc.select(selector);
            log.debug("CSS selector '{}' found {} elements", selector, els.size());
            return formatElements(els, 10);
        } catch (Exception e) {
            log.warn("Invalid CSS selector: {}", selector, e);
            return "Invalid selector: " + e.getMessage();
        }
    }

    @Tool("Find elements by XPath expression. Returns matching elements or error message.")
    public String findByXPath(@P("XPath expression to evaluate") String xpath) {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        try {
            Elements els = doc.selectXpath(xpath);
            log.debug("XPath '{}' found {} elements", xpath, els.size());
            return formatElements(els, 10);
        } catch (Exception e) {
            log.warn("Invalid XPath: {}", xpath, e);
            return "Invalid XPath: " + e.getMessage();
        }
    }

    @Tool("Get all interactive elements (inputs, buttons, links, etc.) with their IDs, names, and classes. Limited to first 50 elements.")
    public String getAllInteractiveElements() {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        Elements els = doc.select("input, button, a, select, textarea");
        log.debug("Found {} interactive elements", els.size());
        return formatElements(els, 50);
    }

    @Tool("Search for elements containing specific text. Returns matching elements.")
    public String findByText(@P("Text content to search for") String text) {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        try {
            String escapedText = text.replace("\"", "\\\"");
            Elements els = doc.select(String.format(":containsOwn(%s)", escapedText));
            log.debug("Text search for '{}' found {} elements", text, els.size());
            return formatElements(els, 20);
        } catch (Exception e) {
            log.warn("Text search failed for: {}", text, e);
            return "Search failed: " + e.getMessage();
        }
    }

    @Tool("Get all elements with a specific attribute. Example: data-testid, aria-label, role")
    public String findByAttribute(
            @P("Attribute name") String attrName,
            @P(value = "Attribute value (optional)", required = false) String attrValue) {
        Document doc = currentDoc.get();
        if (doc == null) return "Document not set";

        try {
            String selector = attrValue != null && !attrValue.isEmpty()
                    ? String.format("[%s='%s']", attrName, attrValue)
                    : String.format("[%s]", attrName);
            Elements els = doc.select(selector);
            log.debug("Attribute search [{}={}] found {} elements", attrName, attrValue, els.size());
            return formatElements(els, 20);
        } catch (Exception e) {
            return "Search failed: " + e.getMessage();
        }
    }

    /**
     * Format a single element with key attributes
     */
    private String formatElement(Element el) {
        String text = el.text();
        String truncatedText = text.length() > 50 ? text.substring(0, 50) + "..." : text;

        return String.format("<%s id=\"%s\" class=\"%s\" name=\"%s\" data-testid=\"%s\">%s</%s>",
                el.tagName(),
                el.id(),
                el.className(),
                el.attr("name"),
                el.attr("data-testid"),
                truncatedText,
                el.tagName());
    }

    /**
     * Format multiple elements with limit
     */
    private String formatElements(Elements elements, int limit) {
        if (elements.isEmpty()) {
            return "No elements found";
        }

        String formatted = elements.stream()
                .limit(limit)
                .map(this::formatElement)
                .collect(Collectors.joining("\n"));

        if (elements.size() > limit) {
            formatted += String.format("\n... and %d more elements", elements.size() - limit);
        }

        return String.format("Found %d elements:\n%s",
                Math.min(elements.size(), limit), formatted);
    }
}
