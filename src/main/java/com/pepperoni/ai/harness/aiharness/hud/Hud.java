package com.pepperoni.ai.harness.aiharness.hud;

import com.pepperoni.ai.harness.aiharness.agent.Agent;
import com.pepperoni.ai.harness.aiharness.agent.AgentEvent;
import com.pepperoni.ai.harness.aiharness.agent.Conversation;
import com.pepperoni.ai.harness.aiharness.context.TokenEstimator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pepperoni.ai.harness.aiharness.hud.BodyPart.*;

@Component
@RequiredArgsConstructor
public class Hud {

    private final MachineViewPrinter machineViewPrinter;
    private final Agent agent;

    @EventListener
    public void onTurnStarted(AgentEvent.TurnStarted event) {
        final var userInput = event.userMessage();
        machineViewPrinter.showOrder(userInput);

        Optional.of(agent.getConversation())
                .map(Conversation::toPromptMessages)
                .ifPresent(prompts -> machineViewPrinter.showContext(prompts, TokenEstimator.estimate(prompts)));
    }

    @EventListener
    public void onTurnFinished(AgentEvent.TurnFinished event) {
        Optional.of(event).map(AgentEvent.TurnFinished::duration).ifPresent(machineViewPrinter::showTurnFinished);
    }

    @EventListener
    public void onApplicationStart(ApplicationStartedEvent event) {
        final var beanCount = event.getApplicationContext().getBeanDefinitionCount();
        final var bootLines = this.collectBootLines(beanCount);

        machineViewPrinter.showBootLog(bootLines);
    }

    private List<BootLine> collectBootLines(int beanDefinitionCount) {
        final var lines = new ArrayList<BootLine>();

        lines.add(BootLine.of(POWER_CELL, BootStatus.ONLINE, "Spring context: " + beanDefinitionCount));
        lines.add(BootLine.of(NEUROPROCESSOR, BootStatus.ONLINE, agent.getChatModelName()));
        lines.add(BootLine.of(MEMORY_BANK, BootStatus.ONLINE, agent.getConversation().getClass().getSimpleName()));
        lines.add(BootLine.of(DIAGNOSTIC_BUS, BootStatus.ONLINE, "Agent Events: " + AgentEvent.class.getPermittedSubclasses().length));

        return lines;
    }

    //@EventListener
    public void onAgentSpokeEvent(AgentEvent.AgentSpoke event) {
        final var assistantMessage = event.assistantMessage();

        Optional.of(Pattern.compile("[А-Яа-яЁё]"))
                .map(pattern -> pattern.matcher(assistantMessage))
                .map(Matcher::find)
                .filter(isContainsRus -> !isContainsRus)
                .orElseGet(() -> {
                    System.err.println("НЕ ГОВОРИ ПО РУССКИ");
                    throw new RuntimeException(assistantMessage);
                });
    }
}
