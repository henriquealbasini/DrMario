package com.g06.controller;

import com.g06.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ArenaControllerTest {

    private Arena mockArena;
    private ArenaController controller;
    private Pill mockPill;

    @BeforeEach
    void setUp() {
        mockArena = Mockito.mock(Arena.class);
        controller = new ArenaController(mockArena);

        mockPill = Mockito.mock(Pill.class);

        when(mockArena.getWidth()).thenReturn(10);
        when(mockArena.getHeight()).thenReturn(20);
        when(mockArena.getCurrentPill()).thenReturn(mockPill);
        when(mockArena.getWalls()).thenReturn(Collections.emptyList());
        when(mockArena.getMatrix()).thenReturn(new Block[10][20]);
        when(mockArena.isInside(any(Position.class))).thenReturn(true);

        when(mockPill.getPosition()).thenReturn(new Position(5, 5));
        when(mockPill.getOtherHalf()).thenReturn(new Position(6, 5));
    }

    private Arena setupArenaForComplexGravity(int width, int height) {
        Arena arena = new Arena(width, height);
        arena.setCurrentPill(null);

        Block[][] matrix = arena.getMatrix();
        for (int x = 1; x < width - 1; x++)
            for (int y = 1; y < height - 1; y++)
                matrix[x][y] = null;

        return arena;
    }

    @Test
    void movePillLeft_Success() {
        Pill realPill = new Pill(5, 5, "RED", "BLUE");
        when(mockArena.getCurrentPill()).thenReturn(realPill);

        controller.movePillLeft();

        assertEquals(4, realPill.getPosition().getX());
    }

    @Test
    void movePillLeft_BlockCollision_UndoMove() {
        Pill realPill = new Pill(2, 5, "RED", "BLUE");
        when(mockArena.getCurrentPill()).thenReturn(realPill);

        Block[][] matrixWithCollision = new Block[10][20];
        matrixWithCollision[1][5] = new Block(1, 5, "RED");
        when(mockArena.getMatrix()).thenReturn(matrixWithCollision);

        controller.movePillLeft();

        assertEquals(2, realPill.getPosition().getX());
    }

    @Test
    void movePillRight_WallCollision_UndoMove() {
        Pill realPill = new Pill(8, 5, "RED", "BLUE");
        when(mockArena.getCurrentPill()).thenReturn(realPill);

        Wall rightWall = new Wall(9, 5);
        when(mockArena.getWalls()).thenReturn(List.of(rightWall));

        controller.movePillRight();

        assertEquals(8, realPill.getPosition().getX());
    }

    @Test
    void rotatePill_WallCollision_UndoRotate() {
        Pill realPill = new Pill(8, 1, "RED", "BLUE");
        when(mockArena.getCurrentPill()).thenReturn(realPill);

        Wall topWall = new Wall(8, 0);
        when(mockArena.getWalls()).thenReturn(List.of(topWall));

        controller.rotatePill();

        assertEquals(0, realPill.getOrientation());
    }


}
