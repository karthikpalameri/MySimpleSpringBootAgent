package com.simple.MySimpleSpringBootAgent.config;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listener for all ChatModel interactions in the Locator Analyzer service
 * Provides centralized logging, monitoring, and debugging capabilities
 * Monitors ALL locator type analysis (ID, Name, ClassName, TagName, LinkText, CSS, XPath)
 */
@Slf4j
@Component
public class LocatorAnalyzerChatModelListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        var request = requestContext.chatRequest();
        var params = request.parameters();

        log.info("=".repeat(80));
        log.info("LLM REQUEST - Locator Analysis");
        log.info("=".repeat(80));
        log.info("Model: {}", params.modelName());
        log.info("Messages: {}", request.messages().size());

        request.messages().forEach(message -> {
            String messageText = switch (message) {
                case dev.langchain4j.data.message.UserMessage um -> um.singleText();
                case dev.langchain4j.data.message.SystemMessage sm -> sm.text();
                case dev.langchain4j.data.message.AiMessage am -> am.text();
                case dev.langchain4j.data.message.ToolExecutionResultMessage tm -> tm.text();
                default -> message.toString();
            };
            log.info("  - {}: {}", message.type(), truncate(messageText, 200));
        });

        if (params.temperature() != null) {
            log.info("Temperature: {}", params.temperature());
        }
        if (params.maxOutputTokens() != null) {
            log.info("Max Tokens: {}", params.maxOutputTokens());
        }

        log.info("=".repeat(80));
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        var response = responseContext.chatResponse();

        log.info("=".repeat(80));
        log.info("LLM RESPONSE - Locator Analysis");
        log.info("=".repeat(80));

        if (response.aiMessage() != null) {
            log.info("Response: {}", truncate(response.aiMessage().text(), 500));
        }

        if (response.metadata() != null && response.metadata().tokenUsage() != null) {
            var tokenUsage = response.metadata().tokenUsage();
            log.info("Token Usage:");
            log.info("  - Input: {}", tokenUsage.inputTokenCount());
            log.info("  - Output: {}", tokenUsage.outputTokenCount());
            log.info("  - Total: {}", tokenUsage.totalTokenCount());
        }

        if (response.metadata() != null && response.metadata().finishReason() != null) {
            log.info("Finish Reason: {}", response.metadata().finishReason());
        }

        log.info("=".repeat(80));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("=".repeat(80));
        log.error("LLM ERROR - Locator Analysis");
        log.error("=".repeat(80));
        log.error("Request: {}", errorContext.chatRequest());
        log.error("Error: {}", errorContext.error().getMessage(), errorContext.error());
        log.error("=".repeat(80));
    }

    /**
     * Truncate long strings for logging
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
}
