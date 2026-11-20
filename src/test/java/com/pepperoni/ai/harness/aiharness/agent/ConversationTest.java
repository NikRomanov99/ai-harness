package com.pepperoni.ai.harness.aiharness.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import static org.assertj.core.api.Assertions.assertThat;

public class ConversationTest {

    @Test
    void systemPromptAlwaysGetInPrompt() {
        final var conversation = new Conversation("SYSTEM");
        conversation.addUserMessage("привет");

        assertThat(conversation.toPromptMessages()).hasSize(2);
    }


    @Test
    void buildPromptInCorrectOrder() {
        final var conversation = new Conversation("SYSTEM");
        conversation.addUserMessage("вопрос");
        conversation.addAssistantMessage(new AssistantMessage("ответ"));

        assertThat(conversation.toPromptMessages())
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT);
    }
}
