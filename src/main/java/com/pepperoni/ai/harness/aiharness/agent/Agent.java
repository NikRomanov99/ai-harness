package com.pepperoni.ai.harness.aiharness.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Agent {
    private static final String SYSTEM_PROMPT = """
            You are Harness, a coding agent that works in a local repository.
            
            You have tools for inspecting the project. Use them instead of guessing:
            if the user asks about a file, read it first, then answer from what you read.
            Never claim you have seen a file that you did not read with a tool.
            
            Answer concisely.""";

    @Getter
    private final Conversation conversation = new Conversation(SYSTEM_PROMPT);
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChatModel chatModel;
    private final ToolRegistry toolRegistry;
    private final SpringAiToolAdapter springAiToolAdapter;
    private final AgentProperties agentProperties;

    public String run(String userMessage) {
        final var startTime = Instant.now();

        conversation.addUserMessage(userMessage);

        applicationEventPublisher.publishEvent(new AgentEvent.TurnStarted(userMessage));

        for (int iteration = 1; iteration <= agentProperties.maxTurners(); iteration++) {
            final var prompt = Prompt.builder()
                    .messages(conversation.toPromptMessages())
                    .chatOptions(getToolOptions())
                    .build();

            final var assistantMessage = Optional.of(chatModel.call(prompt))
                    .map(ChatResponse::getResult)
                    .map(Generation::getOutput)
                    .orElseThrow(() -> new RuntimeException("No output result found"));//TODO кастомный экспешен

            conversation.addAssistantMessage(assistantMessage);

            applicationEventPublisher.publishEvent(new AgentEvent.AgentSpoke(assistantMessage.getText()));

            if (!assistantMessage.hasToolCalls()) {
                applicationEventPublisher.publishEvent(new AgentEvent.TurnFinished(Duration.between(startTime, Instant.now()), iteration));
                return assistantMessage.getText();
            }

            conversation.addToolResponses(executeAll(assistantMessage));
        }

        applicationEventPublisher.publishEvent(new AgentEvent.TurnLimitReached(agentProperties.maxTurners()));
        applicationEventPublisher.publishEvent(new AgentEvent.TurnFinished(Duration.between(startTime, Instant.now()), agentProperties.maxTurners()));

        return "Reached limit of iteration %s task wasn't finish.".formatted(agentProperties.maxTurners());
    }

    private ToolResponseMessage executeAll(AssistantMessage assistantMessage) {
        List<ToolResponseMessage.ToolResponse> toolResponseMessages = new LinkedList<>();

        assistantMessage.getToolCalls().forEach(toolCall -> {
            final var toolResult = toolRegistry.execute(toolCall.name(), toolCall.arguments());
            toolResponseMessages.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), toolResult.content()));
        });

        return ToolResponseMessage.builder()
                .responses(toolResponseMessages)
                .build();
    }


    public String getChatModelName() {
        return Optional.of(chatModel)
                .map(ChatModel::getOptions)
                .map(ChatOptions::getModel)
                .orElseThrow(() -> new RuntimeException("Can't get model name. No options found"));
    }

    private ChatOptions getToolOptions() {
        return chatModel.getOptions()
                .mutate()
                .combineWith(
                        ToolCallingChatOptions.builder()
                                .toolCallbacks(springAiToolAdapter.toolCallbacks())
                )
                .build();
    }
}
