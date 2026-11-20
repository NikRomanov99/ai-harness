package com.pepperoni.ai.harness.aiharness.agent;

import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class RecordingEventPublisher implements ApplicationEventPublisher {

    private final List<AgentEvent> events = new ArrayList<>();

    @Override
    public void publishEvent(Object event) {
        if(event instanceof AgentEvent) {
            events.add((AgentEvent) event);
        }
    }

    public List<AgentEvent> getEvents() {
        return List.copyOf(events);
    }
}
