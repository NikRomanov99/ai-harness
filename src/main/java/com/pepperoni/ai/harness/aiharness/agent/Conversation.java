package com.pepperoni.ai.harness.aiharness.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class Conversation {
    private final String systemPrompt;
    private List<Message> messages = new ArrayList<>();

    public void addUserMessage(String userMessage) {
        messages.add(new UserMessage(userMessage));
    }

    public void addAssistantMessage(AssistantMessage message) {
        messages.add(message);
    }

    public List<Message> toPromptMessages() {
        final var prompt = new ArrayList<Message>(messages.size() + 1);
        prompt.add(new SystemMessage(systemPrompt));
        prompt.addAll(messages);

        return prompt;
    }

    public void addToolResponses(ToolResponseMessage toolResponseMessage) {
        messages.add(toolResponseMessage);
    }
}
