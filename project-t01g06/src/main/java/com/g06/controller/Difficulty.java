package com.g06.controller;

/**
 * Difficulty levels with associated falling delay (ms).
 */
public enum Difficulty {
    EASY(700),
    NORMAL(500),
    HARD(300),
    INSANE(150);

    private final long delayMs;

    Difficulty(long delayMs) { this.delayMs = delayMs; }

    public long getDelayMs() { return delayMs; }

    public Difficulty next() {
        Difficulty[] vals = values();
        int idx = (this.ordinal() + 1) % vals.length;
        return vals[idx];
    }

    public Difficulty previous() {
        Difficulty[] vals = values();
        int idx = (this.ordinal() - 1 + vals.length) % vals.length;
        return vals[idx];
    }
}

