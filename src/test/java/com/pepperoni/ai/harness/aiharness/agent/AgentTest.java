package com.pepperoni.ai.harness.aiharness.agent;

import com.pepperoni.ai.harness.aiharness.agent.script.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentTest {
    private final RecordingEventPublisher publisher = new RecordingEventPublisher();

    @Test
    void shouldReturnAnswer() {
        final var agent = new Agent(publisher, ScriptedChatModel.replyingWith("I am Harness."));

        assertThat(agent.run("Кто ты?")).isEqualTo("I am Harness.");
    }

    @Test
    void firstMessageIsAlwaysSystemPrompt() {
        final var model = ScriptedChatModel.replyingWith("ok");
        final var agent = new Agent(publisher, model);

        agent.run("Привет");

        final var sent = model.lastPrompt().getInstructions();
        assertThat(sent.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(sent.getFirst().getText()).contains("Harness");
    }

    @Test
    void rememberLastStep() {
        final var model = ScriptedChatModel.replyingWith("Привет!", "Тебя зовут Саша.");
        final var agent = new Agent(publisher, model);

        agent.run("Меня зовут Саша");
        agent.run("Как меня зовут?");

        // Второй запрос содержит всю историю: у модели нет памяти между вызовами,
        // помнит за неё Conversation.
        final var secondCall = model.lastPrompt().getInstructions().stream().map(Message::getText).toList();
        assertThat(secondCall)
                .containsExactly(
                        secondCall.getFirst(), "Меня зовут Саша", "Привет!", "Как меня зовут?");
    }
}
