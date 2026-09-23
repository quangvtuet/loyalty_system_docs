package com.loyalty.capstone.broker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * I-4 container 'Message Broker', collapsed to an in-process bus.
 * Topic names are the event names from the Lab 3 contract register.
 */
public final class MessageBroker {

    private final Map<String, List<Consumer<Map<String, Object>>>> subscribers = new LinkedHashMap<>();
    private final List<String> publishedLog = new ArrayList<>();

    public String containerName() { return "Message Broker"; }

    public void subscribe(String topic, Consumer<Map<String, Object>> handler) {
        subscribers.computeIfAbsent(topic, key -> new ArrayList<>()).add(handler);
    }

    public void publish(String topic, Map<String, Object> event) {
        publishedLog.add(topic);
        for (Consumer<Map<String, Object>> handler : subscribers.getOrDefault(topic, new ArrayList<>())) {
            handler.accept(event);
        }
    }

    public int publishCount(String topic) {
        int count = 0;
        for (String t : publishedLog) if (t.equals(topic)) count++;
        return count;
    }
}
