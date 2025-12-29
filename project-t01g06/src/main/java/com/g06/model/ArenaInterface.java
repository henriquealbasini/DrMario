package com.g06.model;

import java.util.List;


public interface ArenaInterface {
    int getWidth();
    int getHeight();
    List<Wall> getWalls();
    Block[][] getMatrix();
    Pill getCurrentPill();
    boolean spawnNewPill();
    boolean isInside(Position p);
    void setCurrentPill(Pill pill);

    int getVirusCount();

    // Next pill support for preview
    Pill getNextPill();
    void setNextPill(Pill pill);
    boolean generateNextPill();
    int getLevel();
    // Sword support
    Sword getCurrentSword();
    void setCurrentSword(Sword sword);
    boolean spawnSwordAt(int x);
}