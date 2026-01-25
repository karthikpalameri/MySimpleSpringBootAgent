package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.ScoredElement;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Interface for DOM tree pruning following Dependency Inversion Principle
 * Enables multiple pruning strategies and testing with mocks
 */
public interface DomPruner {

    /**
     * Prune DOM tree to keep only relevant subtrees containing candidates
     *
     * @param doc The full parsed HTML document
     * @param candidates The list of candidate elements to preserve
     * @return Pruned document containing only relevant subtrees
     */
    Document pruneToRelevantSubtree(Document doc, List<ScoredElement> candidates);
}
