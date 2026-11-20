package com.pepperoni.ai.harness.aiharness.agent;

import java.time.Duration;

public sealed interface AgentEvent {

    record TurnStarted(String userMessage) implements AgentEvent {}

    record AgentSpoke(String assistantMessage) implements AgentEvent {}

    record TurnFinished(Duration duration) implements AgentEvent {}
}
