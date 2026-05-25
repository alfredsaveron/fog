package com.alfredsaveron.fog.model;

import java.util.List;

public class Atmosphere {

    private final List<String> messages;
    private final List<String> sounds;

    public Atmosphere(List<String> messages, List<String> sounds) {
        this.messages = List.copyOf(messages);
        this.sounds = List.copyOf(sounds);
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getSounds() {
        return sounds;
    }
}
