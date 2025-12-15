package com.g06.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaTest {

    @Test
    void walls_count_bounds_and_currentPill() {
        int w = 10, h = 18;
        Arena arena = new Arena(w, h);

        assertEquals(w, arena.getWidth());
        assertEquals(h, arena.getHeight());

        assertEquals(2 * (w + h), arena.getWalls().size());

        assertTrue(arena.isInside(new Position(1, 1)));
        assertFalse(arena.isInside(new Position(-1, 0)));
        assertFalse(arena.isInside(new Position(w, 0)));

        assertNotNull(arena.getCurrentPill());
    }

    @Test
    void spawnViruses_places_up_to_requested_amount() {
        Arena arena = new Arena(10, 18);
        Block[][] matrix = arena.getMatrix();

        int virusCount = 0;
        for (int x = 0; x < arena.getWidth(); x++) {
            for (int y = 0; y < arena.getHeight(); y++) {
                Block b = matrix[x][y];
                if (b != null && Boolean.TRUE.equals(b.isVirus())) virusCount++;
            }
        }

        assertTrue(virusCount >= 0 && virusCount <= 5);
    }
}
