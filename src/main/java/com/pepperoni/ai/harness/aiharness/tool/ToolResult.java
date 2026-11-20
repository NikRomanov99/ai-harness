package com.pepperoni.ai.harness.aiharness.tool;

import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public record ToolResult(String content, boolean isError) {
    public static ToolResult ok(String content) {
        return new ToolResult(content, false);
    }

    public static ToolResult error(String content) {
        return new ToolResult(content, true);
    }

    public String preview(int maxChars) {
        final var firstLine = content.lines().filter(Strings::isNotBlank).findFirst().orElse("");

        return Optional.of(firstLine).filter(line -> line.length() > maxChars)
                .map(line -> line.substring(0, maxChars) + "...")
                .orElse(firstLine);
    }
}
