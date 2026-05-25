package com.alfredsaveron.fog.model;

public enum DepthZone {

    SURFACE("surface"),
    SHALLOW("shallow"),
    DEEP("deep"),
    ABYSS("abyss");

    private final String configKey;

    DepthZone(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static DepthZone fromY(int y, int shallowBelow, int deepBelow, int abyssBelow) {
        if (y <= abyssBelow) return ABYSS;
        if (y <= deepBelow) return DEEP;
        if (y <= shallowBelow) return SHALLOW;
        return SURFACE;
    }
}
