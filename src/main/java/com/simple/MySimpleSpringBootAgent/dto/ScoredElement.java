package com.simple.MySimpleSpringBootAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jsoup.nodes.Element;

@Data
@AllArgsConstructor
public class ScoredElement implements Comparable<ScoredElement> {
    private Element element;
    private int score;
    private String matchReason;

    @Override
    public int compareTo(ScoredElement other) {
        return Integer.compare(other.score, this.score); // Descending order
    }
}
