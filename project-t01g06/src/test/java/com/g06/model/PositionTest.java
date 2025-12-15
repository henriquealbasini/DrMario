package com.g06.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void equals_behaviour_is_correct() {
        Position a = new Position(1, 2);
        Position b = new Position(1, 2);
        Position c = new Position(2, 1);

        assertEquals(a, b, "Positions with same coordinates should be equal");
        assertNotEquals(a, c, "Different coordinates should not be equal");
        assertFalse(a.equals(null), "com.g06.model.Position.equals should handle null");
    }
}
