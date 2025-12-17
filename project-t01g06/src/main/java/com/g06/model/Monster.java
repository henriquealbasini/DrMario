package com.g06.model;

public class Monster extends Element {
    private boolean movingRight;

    public Monster(int x, int y) {
        super(x, y);
        this.movingRight = true;
    }

    public boolean isMovingRight() {
        return movingRight;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }
}