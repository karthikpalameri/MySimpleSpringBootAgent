package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorHints;
import com.simple.MySimpleSpringBootAgent.dto.ScoredElement;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Interface for finding candidate DOM elements following Dependency Inversion Principle
 * Enables multiple matching strategies and testing with mocks
 */
public interface CandidateFinder {

    /**
     * Find candidate elements in document based on locator hints
     *
     * @param doc The parsed HTML document
     * @param hints The parsed locator hints
     * @return List of scored candidate elements, ordered by relevance
     */
    List<ScoredElement> findCandidates(Document doc, LocatorHints hints);
}
