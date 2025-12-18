package com.g06.controller;

import com.g06.model.Arena;
import com.g06.model.Block;
import com.g06.model.Monster;
import com.g06.model.Pill;
import com.g06.model.Position;
import com.g06.model.Virus;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArenaController implements Controller {
    private final Arena arena;

    private long lastMonsterMoveTime;

    private boolean gameOver = false;
    private boolean victory = false;

    public ArenaController(Arena arena) {
        this.arena = arena;
        this.lastMonsterMoveTime = System.currentTimeMillis();
    }

    @Override
    public void processKey(KeyStroke key) {
        if (key == null) return;

        switch (key.getKeyType()) {
            case ArrowLeft:
                movePillLeft();
                break;
            case ArrowRight:
                movePillRight();
                break;
            case ArrowUp:
                rotatePill();
                break;
            case ArrowDown:
                fallPill(); // Soft drop
                break;
            default:
                break;
        }
    }

    // --- Lógica Principal (Chamada pelo Game.java) ---

    public void step() {
        moveMonster();
        checkMonsterCollision();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isVictory() {
        return victory;
    }

    // --- Movimento da Pílula ---

    public boolean fallPill() {
        if (gameOver) return false;

        Pill currentPill = arena.getCurrentPill();
        if (currentPill == null) return false;

        Pill nextState = copyPill(currentPill);
        nextState.moveDown();

        if (canMove(nextState)) {
            currentPill.moveDown();
            checkMonsterCollision(); // Verifica colisão a cada descida
            return true;
        } else {
            settlePill();
            return false;
        }
    }

    public void movePillLeft() {
        Pill currentPill = arena.getCurrentPill();
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.moveLeft();
        if (canMove(nextState)) {
            currentPill.moveLeft();
            checkMonsterCollision();
        }
    }

    public void movePillRight() {
        Pill currentPill = arena.getCurrentPill();
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.moveRight();
        if (canMove(nextState)) {
            currentPill.moveRight();
            checkMonsterCollision();
        }
    }

    public void rotatePill() {
        Pill currentPill = arena.getCurrentPill();
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.rotate();
        if (canMove(nextState)) {
            currentPill.rotate();
            checkMonsterCollision();
        }
    }

    // --- Lógica de Fixação, Limpeza e Vitória ---

    public void settlePill() {
        Pill currentPill = arena.getCurrentPill();
        if (currentPill == null) return;

        Position p1 = currentPill.getPosition();
        Position p2 = currentPill.getOtherHalf();
        Block[][] matrix = arena.getMatrix();

        if (arena.isInside(p1))
            matrix[p1.getX()][p1.getY()] = new Block(p1.getX(), p1.getY(), currentPill.getColor1());
        if (arena.isInside(p2))
            matrix[p2.getX()][p2.getY()] = new Block(p2.getX(), p2.getY(), currentPill.getColor2());

        arena.setCurrentPill(null);

        checkAndClearLines();

        // Verificar Vitória (0 vírus restantes)
        if (arena.getVirusCount() == 0) {
            this.victory = true;
            return;
        }

        // Criar nova pílula. Se não conseguir, é Game Over.
        boolean spawned = arena.spawnNewPill();
        if (!spawned) {
            this.gameOver = true;
        }
    }

    private void checkAndClearLines() {
        Set<Position> toRemove = new HashSet<>();
        Block[][] matrix = arena.getMatrix();

        for (int y = 1; y < arena.getHeight() - 1; y++) {
            for (int x = 1; x < arena.getWidth() - 1; x++) {
                Block current = matrix[x][y];
                if (current == null) continue;

                checkDirection(current, toRemove, x, y, 1, 0);
                checkDirection(current, toRemove, x, y, 0, 1);
            }
        }
        removeMarkedBlocks(toRemove);
    }

    private void checkDirection(Block startBlock, Set<Position> toRemove, int x, int y, int dx, int dy) {
        String targetColor = startBlock.getColor();
        List<Block> line = new ArrayList<>();
        line.add(startBlock);
        Block[][] matrix = arena.getMatrix();

        for (int i = 1; i <= 3; i++) {
            int nextX = x + i * dx;
            int nextY = y + i * dy;

            if (!arena.isInside(new Position(nextX, nextY))) break;
            Block next = matrix[nextX][nextY];
            if (next == null || !next.getColor().equals(targetColor)) break;
            line.add(next);
        }

        if (line.size() >= 4) {
            for (Block block : line) {
                toRemove.add(block.getPosition());
            }
        }
    }

    private void removeMarkedBlocks(Set<Position> toRemove) {
        if (toRemove.isEmpty()) return;
        Block[][] matrix = arena.getMatrix();
        for (Position p : toRemove) {
            matrix[p.getX()][p.getY()] = null;
        }
        applyClusterGravity();
    }

    private void applyClusterGravity() {
        boolean moved;
        int width = arena.getWidth();
        int height = arena.getHeight();
        Block[][] matrix = arena.getMatrix();

        do {
            moved = false;
            boolean[][] visited = new boolean[width][height];

            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    if (matrix[x][y] == null || visited[x][y] || (matrix[x][y] instanceof Virus))
                        continue;

                    List<Position> cluster = new ArrayList<>();
                    collectCluster(x, y, visited, cluster);

                    int fall = computeClusterFall(cluster);

                    if (fall > 0) {
                        moveCluster(cluster, fall);
                        moved = true;
                    }
                }
            }
        } while (moved);
    }

    private void collectCluster(int sx, int sy, boolean[][] visited, List<Position> cluster) {
        int width = arena.getWidth();
        int height = arena.getHeight();
        Block[][] matrix = arena.getMatrix();
        List<Position> stack = new ArrayList<>();
        stack.add(new Position(sx, sy));
        visited[sx][sy] = true;

        while (!stack.isEmpty()) {
            Position p = stack.remove(stack.size() - 1);
            cluster.add(p);
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

            for (int[] d : dirs) {
                int nx = p.getX() + d[0];
                int ny = p.getY() + d[1];

                if (nx <= 0 || nx >= width-1 || ny <= 0 || ny >= height-1) continue;
                if (visited[nx][ny]) continue;
                Block neighbor = matrix[nx][ny];
                if (neighbor == null || (neighbor instanceof Virus)) continue;

                visited[nx][ny] = true;
                stack.add(new Position(nx, ny));
            }
        }
    }

    private int computeClusterFall(List<Position> cluster) {
        int fall = Integer.MAX_VALUE;
        Block[][] matrix = arena.getMatrix();
        int height = arena.getHeight();

        for (Position p : cluster) {
            int x = p.getX();
            int y = p.getY();
            int drop = 0;
            while (true) {
                int ny = y + drop + 1;
                if (ny >= height - 1) break;
                Block below = matrix[x][ny];
                if (below != null && !isInCluster(cluster, x, ny)) break;
                drop++;
            }
            fall = Math.min(fall, drop);
        }
        return fall;
    }

    private boolean isInCluster(List<Position> cluster, int x, int y) {
        for (Position p : cluster) {
            if (p.getX() == x && p.getY() == y) return true;
        }
        return false;
    }

    private void moveCluster(List<Position> cluster, int dist) {
        Block[][] matrix = arena.getMatrix();
        cluster.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (Position p : cluster) {
            Block b = matrix[p.getX()][p.getY()];
            matrix[p.getX()][p.getY()] = null;
            int nx = p.getX();
            int ny = p.getY() + dist;
            matrix[nx][ny] = b;
            b.getPosition().setY(ny);
        }
    }

    // --- Lógica do Monstro ---

    private void moveMonster() {
        Monster monster = arena.getMonster();
        if (monster == null) return;

        // Velocidade baseada no nível (Nível 1 = 700ms, Nível 5 = 300ms)
        long delay = Math.max(100, 800 - (arena.getLevel() * 100));

        if (System.currentTimeMillis() - lastMonsterMoveTime > delay) {
            Position pos = monster.getPosition();
            if (monster.isMovingRight()) {
                if (pos.getX() < arena.getWidth() - 2) pos.setX(pos.getX() + 1);
                else monster.setMovingRight(false);
            } else {
                if (pos.getX() > 1) pos.setX(pos.getX() - 1);
                else monster.setMovingRight(true);
            }
            lastMonsterMoveTime = System.currentTimeMillis();
        }
    }

    private void checkMonsterCollision() {
        Pill pill = arena.getCurrentPill();
        Monster monster = arena.getMonster();
        if (pill == null || monster == null) return;

        Position p1 = pill.getPosition();
        Position p2 = pill.getOtherHalf();
        Position mPos = monster.getPosition();

        if (p1.equals(mPos) || p2.equals(mPos)) {
            this.gameOver = true;
        }
    }

    // --- Helpers ---

    private boolean canMove(Pill pill) {
        Position p1 = pill.getPosition();
        Position p2 = pill.getOtherHalf();
        return isValidPosition(p1) && isValidPosition(p2);
    }

    private boolean isValidPosition(Position p) {
        if (!arena.isInside(p)) return false;
        if (arena.getWalls().stream().anyMatch(w -> w.getPosition().equals(p))) return false;
        if (arena.getMatrix()[p.getX()][p.getY()] != null) return false;
        return true;
    }

    private Pill copyPill(Pill original) {
        Pill copy = new Pill(original.getPosition().getX(), original.getPosition().getY(),
                original.getColor1(), original.getColor2());
        Position originalOther = original.getOtherHalf();
        while (!copy.getOtherHalf().equals(originalOther)) {
            copy.rotate();
        }
        return copy;
    }
}