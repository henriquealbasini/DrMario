import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.util.Map;

public class ArenaViewer {

    private TextGraphics graphics;
    private final int SCALE_X;
    private final int SCALE_Y;

    //Textures

    //Wall
    private static final String HORIZONTAL_LINE = "─";
    private static final String VERTICAL_LINE = "│";
    private static final String CORNER_TL = "┌";
    private static final String CORNER_TR = "┐";
    private static final String CORNER_BL = "└";
    private static final String CORNER_BR = "┘";

    //Blocks
    private static final String[] TEXTURE_CHARS = {"▣", "■", "▤", "▥", "▦", "▧", "▨", "▩", "X"};



    //Color palette

    //Wall
    private static final String WALL_COLOR = "#00bbf1";
    private static final String WALL_BG_COLOR = "#0a2695";

    //Pills | Blocks
    private static final Map<String, String> FOREGROUND_COLORS = Map.of(
            "RED", "#ff4454",
            "YELLOW", "#ffe112",
            "BLUE", "#00b4f6"
            //"VIRUS", "#FFFFFF" //Unused
    );
    private static final Map<String, String> BACKGROUND_COLORS = Map.of(
            "RED", "#cc3744",
            "YELLOW", "#ccb40f",
            "BLUE", "#008fcc",
            "VIRUS", "#4f5376"
    );

    private static final String DEFAULT_COLOR = "#000000";


    public ArenaViewer(TextGraphics graphics, int scaleX, int scaleY) {
        this.graphics = graphics;
        this.SCALE_X = scaleX;
        this.SCALE_Y = scaleY;
    }

    public void draw(Arena arena) {
        // Background
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        graphics.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(arena.getWidth() * this.SCALE_X, arena.getHeight() * this.SCALE_Y), ' ');

        // Draw Walls
        for (Wall wall : arena.getWalls()) {
            drawWall(wall, arena.getWidth(), arena.getHeight());
        }

        // Draw settled Blocks
        Block[][] matrix = arena.getMatrix();
        for (int x = 0; x < arena.getWidth(); x++) {
            for (int y = 0; y < arena.getHeight(); y++) {
                if (matrix[x][y] != null) {
                    drawBlock(matrix[x][y]);
                }
            }
        }

        // Draw the current falling Pill
        if (arena.getCurrentPill() != null) {
            drawPill(arena.getCurrentPill());
        }
    }

    //Color Mapping
    private String mapColorForeground(String colorName) {
        return FOREGROUND_COLORS.getOrDefault(colorName, DEFAULT_COLOR);
    }

    private String mapColorBackground(String colorName) {
        return BACKGROUND_COLORS.getOrDefault(colorName, DEFAULT_COLOR);
    }


    private void drawWall(Wall wall, int arenaWidth, int arenaHeight) {
        graphics.setForegroundColor(TextColor.Factory.fromString(WALL_COLOR));
        graphics.setBackgroundColor(TextColor.Factory.fromString(WALL_BG_COLOR));

        graphics.enableModifiers(SGR.BOLD);

        Position p = wall.getPosition();
        String character;

        if (p.getX() == 0 && p.getY() == 0) {
            character = CORNER_TL;
        } else if (p.getX() == arenaWidth - 1 && p.getY() == 0) {
            character = CORNER_TR;
        } else if (p.getX() == 0 && p.getY() == arenaHeight - 1) {
            character = CORNER_BL;
        } else if (p.getX() == arenaWidth - 1 && p.getY() == arenaHeight - 1) {
            character = CORNER_BR;
        } else if (p.getY() == 0 || p.getY() == arenaHeight - 1) {
            character = HORIZONTAL_LINE;
        } else if (p.getX() == 0 || p.getX() == arenaWidth - 1) {
            character = VERTICAL_LINE;
        } else {
            // IDK just in case
            character = "✦";
        }


        for (int dx = 0; dx < SCALE_X; dx++) {
            for (int dy = 0; dy < SCALE_Y; dy++) {
                graphics.putString(
                        new TerminalPosition(wall.getPosition().getX() * SCALE_X + dx, wall.getPosition().getY() * SCALE_Y + dy),
                        character);
            }
        }
        graphics.disableModifiers(SGR.BOLD);
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));

    }

    private void drawBlock(Block block) {

        // Color
        String backgroundColor;
        String foregroundColor;

        if (block.isVirus()) {
            backgroundColor = mapColorBackground("VIRUS");
        } else {
            backgroundColor = mapColorBackground(block.getColor());
        }
        foregroundColor = mapColorForeground(block.getColor());

        graphics.setBackgroundColor(TextColor.Factory.fromString(backgroundColor));
        graphics.setForegroundColor(TextColor.Factory.fromString(foregroundColor));
        graphics.enableModifiers(SGR.BOLD);

        // Texture
        String character;
        int textureId = block.getTextureID();
        if (textureId >= 0 && textureId < TEXTURE_CHARS.length) {
            character = TEXTURE_CHARS[textureId];
        } else {
            character = "?"; // Just in Case
        }

        for (int dx = 0; dx < SCALE_X; dx++) {
            for (int dy = 0; dy < SCALE_Y; dy++) {
                graphics.putString(
                        new TerminalPosition(block.getPosition().getX() * SCALE_X + dx, block.getPosition().getY() * SCALE_Y + dy),
                        character);
            }
        }
        graphics.disableModifiers(SGR.BOLD);
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));

    }

    private void drawPill(Pill pill) {
        // Pivot
        graphics.setForegroundColor(TextColor.Factory.fromString(mapColorForeground(pill.getColor1())));
        graphics.enableModifiers(SGR.BOLD);
        for (int dx = 0; dx < SCALE_X; dx++) {
            for (int dy = 0; dy < SCALE_Y; dy++) {
                graphics.putString(
                        new TerminalPosition(pill.getPosition().getX() * SCALE_X + dx, pill.getPosition().getY() * SCALE_Y + dy),
                        "◉");
            }
        }

        // Second Half
        Position other = pill.getOtherHalf();
        graphics.setForegroundColor(TextColor.Factory.fromString(mapColorForeground(pill.getColor2())));
        for (int dx = 0; dx < SCALE_X; dx++) {
            for (int dy = 0; dy < SCALE_Y; dy++) {
                graphics.putString(
                        new TerminalPosition(other.getX() * SCALE_X + dx, other.getY() * SCALE_Y + dy),
                        "●");
            }
        }

        graphics.disableModifiers(SGR.BOLD);
    }
}