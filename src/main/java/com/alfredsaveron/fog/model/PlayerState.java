package com.alfredsaveron.fog.model;

import java.util.HashSet;
import java.util.Set;

public class PlayerState {

    private DepthZone currentZone;
    private long lastEffectTimeMillis;
    private boolean enabled;
    private final Set<String> shownMessages = new HashSet<>();

    public PlayerState() {
        this.currentZone = DepthZone.SURFACE;
        this.lastEffectTimeMillis = 0L;
        this.enabled = true;
    }

    public DepthZone getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(DepthZone currentZone) {
        this.currentZone = currentZone;
    }

    public long getLastEffectTimeMillis() {
        return lastEffectTimeMillis;
    }

    public void setLastEffectTimeMillis(long lastEffectTimeMillis) {
        this.lastEffectTimeMillis = lastEffectTimeMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasSeenMessage(String message) {
        return shownMessages.contains(message);
    }

    public void markMessageSeen(String message) {
        shownMessages.add(message);
    }

    public void clearShownMessages() {
        shownMessages.clear();
    }
}
