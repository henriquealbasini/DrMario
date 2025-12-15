package com.g06.model;

import com.g06.model.Block;
import com.g06.model.Pill;
import com.g06.model.Virus;
import com.g06.model.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Arena implements ArenaInterface {

    private int height;
    private int width;
    private Pill currentPill;

    private final String[] VALID_COLORS = {"RED", "YELLOW", "BLUE"};
    private final Random random = new Random();

    private Block[][] matrix;
    private List<Wall> walls;

    public Arena(int width, int height){
        this.height = height;
        this.width = width;
        this.matrix = new Block[width][height];
        this.walls = createWalls();
        spawnViruses(5);
        // Attempt to spawn an initial pill; ignore result here (controller will check for game over after settles)
        spawnNewPill();
    }

    public boolean spawnNewPill() {
        String color1 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
        String color2 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
        Pill candidate = new Pill(width / 2, 1, color1, color2);

        // If either pivot or other half collides with an occupied cell or wall, spawning fails -> game over
        Position p1 = candidate.getPosition();
        Position p2 = candidate.getOtherHalf();

        // Check bounds and occupancy
        if (!isInside(p1) || !isInside(p2)) return false;

        // Check walls
        for (Wall w : walls) {
            if (w.getPosition().equals(p1) || w.getPosition().equals(p2)) return false;
        }

        // Check existing blocks
        if (matrix[p1.getX()][p1.getY()] != null || matrix[p2.getX()][p2.getY()] != null) return false;

        this.currentPill = candidate;
        return true;
    }

    public void spawnViruses(int count){
        int maxAttempts = count * 2;
        int attempts = 0;
        int placed = 0;

        int minX = 1;
        int maxX = width - 2;
        int minY = 4;
        int maxY = height - 2;

        while (placed < count && attempts < maxAttempts) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            attempts++;
            if (matrix[x][y] == null) {
                String color = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
                matrix[x][y] = new Virus(x, y, color);
                placed++;
            }
        }
    }

    private List<Wall> createWalls(){
        List<Wall> walls = new ArrayList<>();
        for(int c=0; c < width; c++){
            walls.add(new Wall(c, 0));
            walls.add(new Wall(c, height-1));
        }
        for(int r=0; r < height; r++){
            walls.add(new Wall(0, r));
            walls.add(new Wall(width-1, r));
        }
        return walls;
    }

    // --- Métodos de Acesso (Dados) ---

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Wall> getWalls() { return walls; }
    public Block[][] getMatrix() { return matrix; }
    public Pill getCurrentPill() { return currentPill; }

    // Adicionado: Setter para o Controller poder limpar ou alterar a pílula
    public void setCurrentPill(Pill pill) { this.currentPill = pill; }

    // Adicionado: Mudado para PUBLIC para o Controller usar
    public boolean isInside(Position p) {
        return p.getX() >= 0 && p.getX() < width && p.getY() >= 0 && p.getY() < height;
    }
}