package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.config.HtmlProcessingConfig;
import com.simple.MySimpleSpringBootAgent.dto.ScoredElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of DomPruner using configurable preservation strategy
 * Follows SOLID principles with externalized configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomPruningService implements DomPruner {

    private final HtmlProcessingConfig config;

    public Document pruneToRelevantSubtree(Document doc, List<ScoredElement> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            log.warn("No candidates provided for pruning, returning minimal document");
            return createMinimalDocument(doc);
        }

        log.debug("Pruning DOM tree with {} candidates", candidates.size());

        // Build preservation set
        Set<Element> preserve = buildPreservationSet(candidates);
        log.debug("Preservation set contains {} elements", preserve.size());

        // Create pruned document
        Document pruned = cloneAndPrune(doc, preserve);

        return pruned;
    }

    private Set<Element> buildPreservationSet(List<ScoredElement> candidates) {
        Set<Element> preserve = new HashSet<>();

        for (ScoredElement scored : candidates) {
            Element candidate = scored.getElement();

            // 1. Preserve the candidate itself
            preserve.add(candidate);

            // 2. Preserve ancestor chain (limited depth)
            addAncestorChain(candidate, preserve, config.getMaxParentDepth());

            // 3. Preserve immediate siblings (limited count)
            addSiblings(candidate, preserve, config.getMaxSiblingCount());

            // 4. Preserve some children (limited depth)
            addChildren(candidate, preserve, config.getMaxChildDepth(), 0);
        }

        return preserve;
    }

    private void addAncestorChain(Element element, Set<Element> preserve, int maxDepth) {
        Element current = element.parent();
        int depth = 0;

        while (current != null && depth < maxDepth) {
            preserve.add(current);
            current = current.parent();
            depth++;
        }

        // Always preserve up to <body> and <html> for valid structure
        current = element.parent();
        while (current != null) {
            if ("body".equals(current.tagName()) || "html".equals(current.tagName())) {
                preserve.add(current);
            }
            current = current.parent();
        }
    }

    private void addSiblings(Element element, Set<Element> preserve, int maxCount) {
        List<Element> siblings = element.siblingElements();

        if (siblings.isEmpty()) {
            return;
        }

        int index = siblings.indexOf(element);
        if (index == -1) {
            index = element.elementSiblingIndex();
        }

        // Add previous siblings
        int prevCount = 0;
        for (int i = index - 1; i >= 0 && prevCount < maxCount; i--) {
            if (i < siblings.size()) {
                preserve.add(siblings.get(i));
                prevCount++;
            }
        }

        // Add next siblings
        int nextCount = 0;
        for (int i = index + 1; i < siblings.size() && nextCount < maxCount; i++) {
            preserve.add(siblings.get(i));
            nextCount++;
        }
    }

    private void addChildren(Element element, Set<Element> preserve, int maxDepth, int currentDepth) {
        if (currentDepth >= maxDepth) {
            return;
        }

        List<Element> children = element.children();
        int childCount = 0;

        for (Element child : children) {
            if (childCount >= config.getMaxChildrenPreserved()) {
                log.trace("Reached max children limit: {}", config.getMaxChildrenPreserved());
                break;
            }

            preserve.add(child);
            addChildren(child, preserve, maxDepth, currentDepth + 1);
            childCount++;
        }
    }

    private Document cloneAndPrune(Document original, Set<Element> preserve) {
        // Create new document with same settings
        Document pruned = Document.createShell(original.baseUri());

        // Clone HTML structure
        Element html = original.selectFirst("html");
        if (html != null) {
            Element prunedHtml = pruned.appendChild(new Element(Tag.valueOf("html"), ""));

            // Clone head (minimally - just keep basic structure)
            Element head = html.selectFirst("head");
            if (head != null && preserve.contains(head)) {
                Element prunedHead = prunedHtml.appendChild(new Element(Tag.valueOf("head"), ""));
                // Only preserve title if exists
                Element title = head.selectFirst("title");
                if (title != null) {
                    prunedHead.appendChild(title.clone());
                }
            }

            // Clone body with pruning
            Element body = html.selectFirst("body");
            if (body != null && preserve.contains(body)) {
                Element prunedBody = prunedHtml.appendChild(new Element(Tag.valueOf("body"), ""));
                clonePreservedElements(body, prunedBody, preserve);
            }
        }

        return pruned;
    }

    private void clonePreservedElements(Element source, Element destination, Set<Element> preserve) {
        for (Element child : source.children()) {
            if (preserve.contains(child)) {
                Element cloned = child.shallowClone();
                destination.appendChild(cloned);

                // Recursively clone preserved children
                if (!child.children().isEmpty()) {
                    clonePreservedElements(child, cloned, preserve);
                }
            }
        }
    }

    private Document createMinimalDocument(Document original) {
        Document minimal = Document.createShell(original.baseUri());

        Element body = minimal.body();
        if (body != null) {
            body.append("<p>No matching elements found for the given locator.</p>");
        }

        return minimal;
    }
}
