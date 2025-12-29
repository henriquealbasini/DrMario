package com.g06.model;

import java.util.ArrayList;
import java.util.List;

public class Sword {
    private Position position; // top segment position (pivot)
    private String color;

    public Sword(int x, int y, String color) {
        this.position = new Position(x, y);
        this.color = color == null ? "RED" : color;
    }

    public Position getPosition() {
        return position;
    }

    public String getColor() { return color; }

    // Returns the 4 vertical segment positions from top to bottom
    public List<Position> getSegments() {
        List<Position> segs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            segs.add(new Position(position.getX(), position.getY() + i));
        }
        return segs;
    }

    public void moveLeft() { position.setX(position.getX() - 1); }
    public void moveRight() { position.setX(position.getX() + 1); }
    public void moveDown() { position.setY(position.getY() + 1); }
    public void moveUp() { position.setY(position.getY() - 1); }
}

