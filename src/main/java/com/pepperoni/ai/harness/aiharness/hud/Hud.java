package com.pepperoni.ai.harness.aiharness.hud;

import com.pepperoni.ai.harness.aiharness.agent.Agent;
import com.pepperoni.ai.harness.aiharness.agent.AgentEvent;
import com.pepperoni.ai.harness.aiharness.agent.Conversation;
import com.pepperoni.ai.harness.aiharness.context.TokenEstimator;
import com.pepperoni.ai.harness.aiharness.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pepperoni.ai.harness.aiharness.hud.BodyPart.*;

@Component
@RequiredArgsConstructor
public class Hud {

    private final MachineViewPrinter machineViewPrinter;
    private final Agent agent;
    private final List<Tool<?>> tools;

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
        Optional.of(event)
                .ifPresent(finished -> machineViewPrinter.showTurnFinished(finished.duration(), finished.iteration()));
    }

    @EventListener
    public void onApplicationStart(ApplicationStartedEvent event) {
        final var beanCount = event.getApplicationContext().getBeanDefinitionCount();
        final var bootLines = this.collectBootLines(beanCount);

        machineViewPrinter.showBootLog(bootLines);
    }

    private List<BootLine> collectBootLines(int beanDefinitionCount) {
        final Map<BodyPart, BootLine> map = new EnumMap<>(BodyPart.class);

        map.put(POWER_CELL, BootLine.of(POWER_CELL, BootStatus.ONLINE, "Spring context: " + beanDefinitionCount));
        map.put(NEUROPROCESSOR, BootLine.of(NEUROPROCESSOR, BootStatus.ONLINE, agent.getChatModelName()));
        map.put(MEMORY_BANK, BootLine.of(MEMORY_BANK, BootStatus.ONLINE, agent.getConversation().getClass().getSimpleName()));
        map.put(DIAGNOSTIC_BUS, BootLine.of(DIAGNOSTIC_BUS, BootStatus.ONLINE, "Agent Events: " + AgentEvent.class.getPermittedSubclasses().length));

        tools.forEach(tool -> {
                    final var part = BodyPart.forToolName(tool.name());
                    final var bootLine = map.computeIfAbsent(part, BootLine::empty);
                    bootLine.components().add(tool.name());
                }
        );

        if (!CollectionUtils.isEmpty(tools)) {
            map.put(TOOL_BUS, BootLine.of(TOOL_BUS, BootStatus.ONLINE, "instrument"));
        }

        return List.copyOf(map.values());
    }

    @EventListener
    public void onToolCallStarter(AgentEvent.ToolCallStarted event) {
        machineViewPrinter.showToolCallStarted(
                Optional.of(event)
                        .map(AgentEvent.ToolCallStarted::description)
                        .filter(Strings::isNotBlank)
                        .orElse(event.toolName())
        );
    }

    @EventListener
    public void onToolCallFinished(AgentEvent.ToolCallFinished event) {
        machineViewPrinter.showToolCallFinished(event.isError(), event.preview(), event.duration());
    }

    @EventListener
    public void onTurnLimitReached(AgentEvent.TurnLimitReached event) {
        machineViewPrinter.showTurnLimitReached(event.maxTurns());
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
