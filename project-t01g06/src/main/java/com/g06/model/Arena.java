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
    private Pill nextPill;

    private final String[] VALID_COLORS = {"RED", "YELLOW", "BLUE"};
    private final Random random = new Random();

    private Block[][] matrix;
    private List<Wall> walls;
    private Sword currentSword;
    private int level = 1;

    public Arena(int width, int height){
        this(width, height, 5); // default to 5 viruses for backward compatibility
    }

    // New constructor allowing control of virus count per arena/level
    public Arena(int width, int height, int virusCount){
        this.height = height;
        this.width = width;
        this.matrix = new Block[width][height];
        this.walls = createWalls();
        spawnViruses(virusCount);
        // Initialize current and next pills
        // Generate next pill then set current from it, and generate another next
        generateNextPill();
        // Move next into current
        if (this.nextPill != null) this.currentPill = this.nextPill;
        else this.currentPill = null;
        // Generate a fresh next pill for preview
        generateNextPill();
    }

    public boolean spawnNewPill() {
        // Compatibility wrapper: promote nextPill to currentPill if available, else try to generate one
        if (this.nextPill == null) {
            boolean ok = generateNextPill();
            if (!ok) return false;
        }

        // Place next as current at spawn position
        this.nextPill.getPosition().setX(width / 2);
        this.nextPill.getPosition().setY(1);
        // Ensure orientation reset
        // (Pill constructor sets orientation 0)
        this.currentPill = this.nextPill;

        // Attempt to generate a fresh next pill for preview; if it fails, nextPill will be null which is acceptable
        boolean gen = generateNextPill();

        // Validate that the new current pill fits
        Position p1 = currentPill.getPosition();
        Position p2 = currentPill.getOtherHalf();
        if (!isInside(p1) || !isInside(p2)) return false;
        for (Wall w : walls) {
            if (w.getPosition().equals(p1) || w.getPosition().equals(p2)) return false;
        }
        if (matrix[p1.getX()][p1.getY()] != null || matrix[p2.getX()][p2.getY()] != null) return false;

        return true;
    }

    // Generate a next pill candidate and store it in nextPill. Returns true if created successfully.
    public boolean generateNextPill() {
        String color1 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
        String color2 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
        Pill candidate = new Pill(width / 2, 1, color1, color2);

        // For next pill we just store colors and orientation; placement happens when promoted to current
        this.nextPill = candidate;
        return true;
    }

    public Pill getNextPill() { return nextPill; }
    public void setNextPill(Pill pill) { this.nextPill = pill; }

    public void spawnViruses(int count){
        int maxAttempts = count * 4; // allow more attempts to place viruses safely
        int attempts = 0;
        int placed = 0;

        int minX = 1;
        int maxX = width - 2;
        int minY = 6; // raise minY to spawn viruses lower (avoid top rows)
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
    public int getVirusCount() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Block b = matrix[x][y];
                // Verifica se o bloco existe e se é um vírus
                if (b != null && b.isVirus()) {
                    count++;
                }
            }
        }
        return count;
    }

    public Sword getCurrentSword() { return currentSword; }
    public void setCurrentSword(Sword sword) { this.currentSword = sword; }

    public int getLevel() { return level; }
    public void levelUp() { this.level++; }
    // Allows external code (Game) to set the level so controllers can initialize powerups correctly
    public void setLevel(int lvl) { this.level = Math.max(1, lvl); }

    // Attempts to spawn a sword at top of column x (top y = 1). Returns false if blocked.
    public boolean spawnSwordAt(int x) {
        int topY = 1; // spawn top segment at y=1 so segments occupy y = 1..4
        // Validate all four positions are inside and empty
        for (int i = 0; i < 4; i++) {
            int yy = topY + i;
            Position p = new Position(x, yy);
            if (!isInside(p)) return false;
            for (Wall w : walls) if (w.getPosition().equals(p)) return false;
            if (matrix[x][yy] != null) return false;
        }
        this.currentSword = new Sword(x, topY, "RED");
        return true;
    }
}