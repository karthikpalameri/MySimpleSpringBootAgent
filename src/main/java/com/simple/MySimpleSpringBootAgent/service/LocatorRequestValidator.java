package com.simple.MySimpleSpringBootAgent.service;

import com.simple.MySimpleSpringBootAgent.dto.LocatorAnalysisRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for validating locator analysis requests
 * Follows Single Responsibility Principle - only handles validation logic
 */
@Service
public class LocatorRequestValidator {

    /**
     * Validates the locator analysis request
     *
     * @param request The request to validate
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validate(LocatorAnalysisRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Request cannot be null");
            return errors;
        }

        if (!StringUtils.hasText(request.getHtmlContent())) {
            errors.add("HTML content is required");
        }

        if (!StringUtils.hasText(request.getLocator())) {
            errors.add("Locator is required");
        }

        return errors;
    }

    /**
     * Check if request is valid
     *
     * @param request The request to check
     * @return true if valid, false otherwise
     */
    public boolean isValid(LocatorAnalysisRequest request) {
        return validate(request).isEmpty();
    }
}
