package com.simple.MySimpleSpringBootAgent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * Service responsible for HTML cleaning operations
 * Follows Single Responsibility Principle - only handles HTML cleaning
 */
@Slf4j
@Service
public class HtmlCleaningService {

    /**
     * Remove noise elements that don't contribute to locator analysis
     *
     * @param doc The Jsoup document to clean
     */
    public void removeNoiseElements(Document doc) {
        // Remove scripts, styles, and non-visual elements
        doc.select("script, style, noscript, link[rel=stylesheet]").remove();

        // Remove metadata
        doc.select("meta, link[rel!=stylesheet]").remove();

        // Remove hidden elements (they can't be interacted with)
        doc.select("[style*=display:none], [style*=visibility:hidden]").remove();
        doc.select("[hidden], [aria-hidden=true]").remove();

        // Remove comments
        doc.select("comment").remove();

        log.debug("Removed noise elements from DOM");
    }

    /**
     * HTML escaping using Apache Commons Text
     *
     * @param text The text to escape
     * @return Escaped HTML text
     */
    public String escapeHtml(String text) {
        return StringEscapeUtils.escapeHtml4(text);
    }
}
