package com.simple.MySimpleSpringBootAgent.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured representation of a single element selector
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElementSelector {

    @Description("The XPath or CSS selector string")
    private String selector;

    @Description("Type of selector: 'xpath' or 'css'")
    private String type;

    @Description("Brief explanation of why this selector works")
    private String explanation;

    @Description("Confidence score from 0 to 100")
    private Integer confidence;
}
