package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorHints;

/**
 * Interface for locator parsing following Dependency Inversion Principle
 * Enables multiple implementations and testing with mocks
 */
public interface LocatorParser {

    /**
     * Parse a locator string and extract semantic hints
     *
     * @param locator The XPath or CSS selector string
     * @return LocatorHints containing extracted information
     */
    LocatorHints parseLocator(String locator);
}
