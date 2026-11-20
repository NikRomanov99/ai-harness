package com.pepperoni.ai.harness.aiharness.agent;

import com.pepperoni.ai.harness.aiharness.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringAiToolAdapter {
    private final ToolRegistry toolRegistry;

    public List<ToolCallback> toolCallbacks() {
        return toolRegistry.getTools().stream()
                .map(SpringAiToolAdapter::describe)
                .toList();
    }

    private static ToolCallback describe(Tool<?> tool) {
        final var toolDefinition = ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(tool.inputShema())
                .build();

        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            public String call(String toolInput) {
                throw new IllegalStateException("Tool should be exec by ToolRegistry, not tool AI: " + tool.name());
            }
        };
    }
}
