package com.pepperoni.ai.harness.aiharness.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Agent {
    private static final String SYSTEM_PROMPT = """
            You are Harness, a coding agent that works in a local repository.
            Answer concisely.""";

    @Getter
    private final Conversation conversation = new Conversation(SYSTEM_PROMPT);
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChatModel chatModel;

    public String run(String userMessage) {
        final var startTime = Instant.now();

        conversation.addUserMessage(userMessage);

        applicationEventPublisher.publishEvent(new AgentEvent.TurnStarted(userMessage));

        final var prompt = new Prompt(conversation.toPromptMessages());
        final var assistantMessage = Optional.of(chatModel.call(prompt))
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .orElseThrow(() -> new RuntimeException("No output result found"));//TODO кастомный экспешен

        conversation.addAssistantMessage(assistantMessage);

        applicationEventPublisher.publishEvent(new AgentEvent.AgentSpoke(assistantMessage.getText()));
        applicationEventPublisher.publishEvent(new AgentEvent.TurnFinished(Duration.between(startTime, Instant.now())));

        return assistantMessage.getText();
    }


    public String getChatModelName() {
        return Optional.of(chatModel)
                .map(ChatModel::getOptions)
                .map(ChatOptions::getModel)
                .orElseThrow(() -> new RuntimeException("Can't get model name. No options found"));
    }
}
