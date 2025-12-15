package com.g06.controller;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Generic controller interface for handling input and ticks.
 */
public interface Controller {
    void processKey(KeyStroke key);
    /** Called periodically (e.g., for gravity) */
    default void tick() {}
    /** Called when this controller becomes active */
    default void onEnter() {}
}

