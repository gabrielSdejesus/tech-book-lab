package com.dataintensive.lab.domain;

public enum ScenarioType {
    CANVAS_MODELING,
    SLIDER_SIMULATION;

    public static ScenarioType fromString(String val) {
        if (val == null) return CANVAS_MODELING;
        try {
            return ScenarioType.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CANVAS_MODELING;
        }
    }
}
