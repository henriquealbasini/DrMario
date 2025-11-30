import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;


public class Arena {

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
        spawnNewPill();
    }


    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Wall> getWalls() { return walls; }
    public Block[][] getMatrix() { return matrix; }
    public Pill getCurrentPill() { return currentPill; }

    public void spawnNewPill() {

        String color1 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];
        String color2 = VALID_COLORS[random.nextInt(VALID_COLORS.length)];

        this.currentPill = new Pill(width / 2, 1, color1, color2);
    }

    public void spawnViruses(int count){
        int maxAttempts = count * 2;
        int attempts = 0;
        int placed = 0;


        int minX = 1;
        int maxX = width - 2;
        int minY = 4; // Start well below the top rows
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

    public boolean fallPill() {
        if (currentPill == null) return false;

        Pill nextState = copyPill(currentPill);
        nextState.moveDown();

        if (canMove(nextState)) {
            currentPill.moveDown();
            return true;
        }
        return false;
    }

    public void settlePill() {
        if(currentPill == null) return;

        Position p1 = currentPill.getPosition();
        Position p2 = currentPill.getOtherHalf();


        if (isInside(p1)) matrix[p1.getX()][p1.getY()] = new Block(p1.getX(), p1.getY(), currentPill.getColor1());
        if (isInside(p2)) matrix[p2.getX()][p2.getY()] = new Block(p2.getX(), p2.getY(), currentPill.getColor2());

        checkAndClearLines();

        currentPill = null;
    }


    public void movePillLeft() {
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.moveLeft();
        if(canMove(nextState)) currentPill.moveLeft();
    }

    public void movePillRight() {
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.moveRight();
        if(canMove(nextState)) currentPill.moveRight();
    }

    public void rotatePill() {
        if (currentPill == null) return;
        Pill nextState = copyPill(currentPill);
        nextState.rotate();
        // TODO: Implement wall kick
        if(canMove(nextState)) currentPill.rotate();
    }

    private boolean canMove(Pill pill) {
        Position p1 = pill.getPosition();
        Position p2 = pill.getOtherHalf();
        return isValidPosition(p1) && isValidPosition(p2);
    }

    private boolean isValidPosition(Position p) {
        if (!isInside(p)) return false;

        for (Wall w : walls) if (w.getPosition().equals(p)) return false;

        if (matrix[p.getX()][p.getY()] != null) return false;
        return true;
    }

    private boolean isInside(Position p) {
        return p.getX() >= 0 && p.getX() < width && p.getY() >= 0 && p.getY() < height;
    }

    private Pill copyPill(Pill original) {

        Pill copy = new Pill(original.getPosition().getX(), original.getPosition().getY(), original.getColor1(), original.getColor2());


        Position originalOther = original.getOtherHalf();
        while(!copy.getOtherHalf().equals(originalOther)) {
            copy.rotate();
        }
        return copy;
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

    private void checkAndClearLines() {
        Set<Position> toRemove = new HashSet<>();

        // Check for blocks in matrix
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                Block current = matrix[x][y];
                if (current == null) continue;

                checkDirection(current, toRemove, x, y, 1, 0);  // Horizontal
                checkDirection(current, toRemove, x, y, 0, 1);  // Vertical
            }
        }

        // Remove marked blocks
        removeMarkedBlocks(toRemove);
    }

    private void checkDirection(Block startBlock, Set<Position> toRemove, int x, int y, int dx, int dy) {
        String targetColor = startBlock.getColor();
        List<Block> line = new ArrayList<>();
        line.add(startBlock);

        // Check up to 3 more blocks in the given direction
        for (int i = 1; i <= 3; i++) {
            int nextX = x + i * dx;
            int nextY = y + i * dy;

            // Check bounds
            if (!isInside(new Position(nextX, nextY))) {
                break;
            }

            Block next = matrix[nextX][nextY];

            // Break if line stops or color doesn't match
            if (next == null || !next.getColor().equals(targetColor)) {
                break;
            }

            line.add(next);
        }

        // Mark for removal if line is bigger than 4
        if (line.size() >= 4) {
            for (Block block : line) {
                toRemove.add(block.getPosition());
            }
        }
    }

    private void removeMarkedBlocks(Set<Position> toRemove) {
        if (toRemove.isEmpty()) return;

        for (Position p : toRemove) {
            // Set the matrix cell to null
            matrix[p.getX()][p.getY()] = null;
        }

        // Apply gravity column by column, from left to right
        for (int x = 1; x < width - 1; x++) {
            // Iterate from the bottom, ignoring the bottom wall at height-1
            for (int y = height - 2; y >= 1; y--) {

                Block currentBlock = matrix[x][y];

                // If the current cell has a block, check if it needs to fall
                if (currentBlock != null) {
                    int dropDistance = 0;


                    for (int checkY = y + 1; checkY < height - 1; checkY++) {
                        if (matrix[x][checkY] == null) {
                            dropDistance++;
                        } else {
                            break;
                        }
                    }

                    if (dropDistance > 0) {

                        int newY = y + dropDistance;

                        // Move the block in the matrix
                        matrix[x][newY] = currentBlock;

                        // Update the block object's internal position
                        currentBlock.getPosition().setY(newY);

                        // Clear the old position
                        matrix[x][y] = null;
                    }
                }
            }
        }

    }
}