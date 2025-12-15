package com.g06.model;

public class Pill {
    private Position position; // The position of the "main" half (pivot)
    private String color1;
    private String color2;
    private int orientation;   // 0: Right, 1: Up, 2: Left, 3: Down

    public Pill(int x, int y, String color1, String color2) {
        this.position = new Position(x, y);
        this.color1 = color1;
        this.color2 = color2;
        this.orientation = 0;
    }

    public Position getPosition() {
        return position;
    }

    // Calculates the other half's position based on 4-way orientation
    public Position getOtherHalf() {
        switch (orientation) {
            case 0: return new Position(position.getX() + 1, position.getY()); // Right
            case 1: return new Position(position.getX(), position.getY() - 1); // Up
            case 2: return new Position(position.getX() - 1, position.getY()); // Left
            case 3: return new Position(position.getX(), position.getY() + 1); // Down
            default: return position;
        }
    }

    // Cycles 0 -> 1 -> 2 -> 3 -> 0
    public void rotate() {
        orientation = (orientation + 1) % 4;
    }

    public void moveLeft() {
        position.setX(position.getX() - 1);
    }

    public void moveRight() {
        position.setX(position.getX() + 1);
    }

    public void moveDown() {
        position.setY(position.getY() + 1);
    }

    public void moveUp() {
        position.setY(position.getY() - 1);
    }

    public String getColor1() { return color1; }
    public String getColor2() { return color2; }


    public int getOrientation() {
        return orientation;
    }
}