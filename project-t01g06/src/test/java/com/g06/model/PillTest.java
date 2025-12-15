package com.g06.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PillTest {

    @Test
    void pill_Initialization_CorrectOrientation() {
        Pill pill = new Pill(5, 5, "RED", "BLUE");

        // Default orientation is 0 (Right)
        assertEquals(0, pill.getOrientation());

        // P1 (pivot) at (5, 5). P2 (other half) should be at (6, 5)
        assertEquals(5, pill.getPosition().getX());
        assertEquals(5, pill.getPosition().getY());
        assertEquals(6, pill.getOtherHalf().getX());
        assertEquals(5, pill.getOtherHalf().getY());
    }

    @Test
    void rotate_CyclesCorrectly_And_OtherHalfPosition() {
        Pill pill = new Pill(5, 5, "RED", "BLUE"); // P1(5, 5)

        // 0 (Right) -> 1 (Up)
        pill.rotate();
        assertEquals(1, pill.getOrientation());
        assertEquals(5, pill.getOtherHalf().getX());
        assertEquals(4, pill.getOtherHalf().getY()); // Y-1

        // 1 (Up) -> 2 (Left)
        pill.rotate();
        assertEquals(2, pill.getOrientation());
        assertEquals(4, pill.getOtherHalf().getX()); // X-1
        assertEquals(5, pill.getOtherHalf().getY());

        // 2 (Left) -> 3 (Down)
        pill.rotate();
        assertEquals(3, pill.getOrientation());
        assertEquals(5, pill.getOtherHalf().getX());
        assertEquals(6, pill.getOtherHalf().getY()); // Y+1

        // 3 (Down) -> 0 (Right)
        pill.rotate();
        assertEquals(0, pill.getOrientation());
        assertEquals(6, pill.getOtherHalf().getX()); // X+1
        assertEquals(5, pill.getOtherHalf().getY());
    }

    @Test
    void moveMethods_UpdatePositionCorrectly() {
        Pill pill = new Pill(5, 5, "RED", "BLUE");

        // Move Left
        pill.moveLeft();
        assertEquals(4, pill.getPosition().getX());

        // Move Right
        pill.moveRight();
        pill.moveRight();
        assertEquals(6, pill.getPosition().getX());

        // Move Up
        pill.moveUp();
        assertEquals(4, pill.getPosition().getY());

        // Move Down
        pill.moveDown();
        assertEquals(5, pill.getPosition().getY());
    }

    @Test
    void getOtherHalf_PositionIsRelative() {
        Pill pill = new Pill(5, 5, "RED", "BLUE"); // Orientation 0: (6, 5)

        // Move the pill
        pill.moveDown();
        pill.moveLeft();
        // New P1: (4, 6)

        // The other half must be (5, 6), relative to (4, 6)
        assertEquals(5, pill.getOtherHalf().getX());
        assertEquals(6, pill.getOtherHalf().getY());

        // Rotate (Orientation 1: Up)
        pill.rotate();

        // The other half must be (4, 5), relative to (4, 6)
        assertEquals(4, pill.getOtherHalf().getX());
        assertEquals(5, pill.getOtherHalf().getY());
    }
}