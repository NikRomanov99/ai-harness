package com.pepperoni.ai.harness.aiharness.agent;

import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("harness.agent")
public record AgentProperties(
        @DefaultValue("20") int maxTurners,
        @DefaultValue(".") String workspaceRoot) {

    public AgentProperties {
        if (maxTurners < 1) {
            throw new IllegalStateException("maxTurners must be greater than 0. Found: " + maxTurners);
        }

        if (Strings.isBlank(workspaceRoot)) {
            throw new IllegalStateException("workspaceRoot must not be null or empty. Found: " + workspaceRoot);
        }
    }
}
