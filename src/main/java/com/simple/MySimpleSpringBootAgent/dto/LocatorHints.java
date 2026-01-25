package com.simple.MySimpleSpringBootAgent.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class LocatorHints {
    private LocatorType type;

    @Builder.Default
    private List<String> ids = new ArrayList<>();

    @Builder.Default
    private List<String> classes = new ArrayList<>();

    @Builder.Default
    private List<String> tagNames = new ArrayList<>();

    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    private String textContent;
    private String rawLocator;
}
