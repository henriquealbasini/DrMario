package com.g06.view;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.g06.model.*;
import com.g06.controller.Difficulty;
import com.g06.controller.MenuController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class ArenaViewer {

    private TextGraphics graphics;
    private final int SCALE_X;
    private final int SCALE_Y;

    // HUD width in characters
    private final int HUD_WIDTH = 20;

    //Textures

    private static final String HORIZONTAL_LINE = "─";
    private static final String VERTICAL_LINE = "│";
    private static final String CORNER_TL = "┌";
    private static final String CORNER_TR = "┐";
    private static final String CORNER_BL = "└";
    private static final String CORNER_BR = "┘";


    private static final String[] TEXTURE_CHARS = {
            "█", // full block
            "▓", // dark shade
            "▒", // medium shade
            "░", // light shade
            "■", // black small square
            "▣", // square with grid (fallback)
            "▤", // shaded pattern
            "▥", // shaded pattern
            "X"  // ASCII fallback
    };




    private static final String WALL_COLOR = "#00bbf1";
    private static final String WALL_BG_COLOR = "#0a2695";

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

    // Updated draw signature to render HUD. levelStartTime may be null for safety.
    public void draw(Arena arena, int level, Difficulty difficulty, MenuController.Mode mode, Instant levelStartTime, long pausedElapsedSeconds, boolean showContinue, int score, int swordCharges) {
        // Compute arena drawing area leaving space on the right for HUD
        int arenaCols = arena.getWidth() * this.SCALE_X;
        int arenaRows = arena.getHeight() * this.SCALE_Y;
        int totalCols = Math.max(arenaCols + HUD_WIDTH, graphics.getSize().getColumns());
        int totalRows = Math.max(arenaRows, graphics.getSize().getRows());

        // Background for full terminal
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        graphics.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(totalCols, totalRows), ' ');

        // Draw Walls and board (left area)
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

        // Draw the current falling com.g06.model.Pill
        if (arena.getCurrentPill() != null) {
            drawPill(arena.getCurrentPill());
        }

        // Draw the current falling Sword if present
        if (arena.getCurrentSword() != null) {
            drawSword(arena.getCurrentSword());
        }

        // Draw HUD on the right
        drawHUD(arena, level, difficulty, mode, levelStartTime, pausedElapsedSeconds, arenaCols, showContinue, score, swordCharges);
    }

    private void drawSword(Sword sword) {
        if (sword == null) return;
        // Render body segments as '|' and the tip (bottom) as \_/
        graphics.setForegroundColor(TextColor.Factory.fromString("#FFFFFF"));
        graphics.enableModifiers(SGR.BOLD);
        java.util.List<Position> segs = sword.getSegments();
        for (int i = 0; i < segs.size(); i++) {
            Position seg = segs.get(i);
            int bx = seg.getX() * SCALE_X;
            int by = seg.getY() * SCALE_Y;

            for (int dx = 0; dx < SCALE_X; dx++) {
                for (int dy = 0; dy < SCALE_Y; dy++) {
                    String ch;
                    if (i == segs.size() - 1) {
                        // tip: render \ _ /
                        if (SCALE_X >= 3) {
                            if (dx == 0) ch = "\\";
                            else if (dx == SCALE_X - 1) ch = "/";
                            else ch = "_";
                        } else {
                            ch = "v"; // fallback for narrow scales
                        }
                    } else {
                        ch = "|"; // body
                    }
                    graphics.putString(new TerminalPosition(bx + dx, by + dy), ch);
                }
            }
        }
        graphics.disableModifiers(SGR.BOLD);
    }

    private void drawHUD(Arena arena, int level, Difficulty difficulty, MenuController.Mode mode, Instant levelStartTime, long pausedElapsedSeconds, int arenaCols, boolean showContinue, int score, int swordCharges) {
        int hudX = arenaCols + 2; // leave 2 columns gap
        int row = 1;
        graphics.setForegroundColor(TextColor.Factory.fromString("#FFFFFF"));

        // Title
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        // Draw next-pill preview (each half scaled according to SCALE_X/SCALE_Y), with a label above the box
        // Determine preview size from scale so preview matches in-game cell size
        int halfW = this.SCALE_X;
        int halfH = this.SCALE_Y;
        int interiorW = halfW * 2; // two halves side-by-side fits interior
        int interiorH = halfH * 2; // two halves stacked fits interior
        int previewW = interiorW + 2; // interior + left/right borders
        int previewH = interiorH + 2; // interior + top/bottom borders
        // only draw if there's space in the HUD area
        if (graphics.getSize().getColumns() >= hudX + previewW) {
            drawPreviewBox(hudX, row, previewW, previewH, arena.getNextPill());
            row += previewH + 2; // label + box + extra spacing
        }
        if (mode == MenuController.Mode.ENDLESS) {
            graphics.putString(new TerminalPosition(hudX, row++), "Score: " + score);
        } else {
            graphics.putString(new TerminalPosition(hudX, row++), "Level: " + level);
        }

        // Difficulty (only show when it affects game: LEVELS mode doesn't change speed, but it's still relevant; ENDLESS shows speed changes)
        graphics.putString(new TerminalPosition(hudX, row++), "Difficulty: " + difficulty.name());

        // Viruses remaining (hide in endless mode)
        if (mode != MenuController.Mode.ENDLESS) {
            graphics.putString(new TerminalPosition(hudX, row++), "Viruses: " + arena.getVirusCount());
        }

        // Elapsed time (pausedElapsedSeconds takes precedence when >0)
        if (pausedElapsedSeconds > 0) {
            long mins = pausedElapsedSeconds / 60;
            long secs = pausedElapsedSeconds % 60;
            String time = String.format("Time: %02d:%02d", mins, secs);
            graphics.putString(new TerminalPosition(hudX, row++), time);
        } else if (levelStartTime != null) {
            Duration elapsed = Duration.between(levelStartTime, Instant.now());
            long mins = elapsed.toMinutes();
            long secs = elapsed.minusMinutes(mins).getSeconds();
            String time = String.format("Time: %02d:%02d", mins, secs);
            graphics.putString(new TerminalPosition(hudX, row++), time);
        } else {
            graphics.putString(new TerminalPosition(hudX, row++), "Time: 00:00");
        }

        // Extra spacing
        row++;

        // Small instructions at top of HUD
        graphics.putString(new TerminalPosition(hudX, row++), "Left/Right: Move");
        graphics.putString(new TerminalPosition(hudX, row++), "Up: Rotate");
        graphics.putString(new TerminalPosition(hudX, row++), "Down: Drop");

        // Sword status / power-up hint (show when charge is available)
        if (swordCharges > 0 && arena.getCurrentSword() == null) {
            graphics.putString(new TerminalPosition(hudX, row++), "Sword ready — press Space to deploy");
            graphics.putString(new TerminalPosition(hudX, row++), "Sword: " + swordCharges + " charge(s)");
        } else if (arena.getCurrentSword() != null) {
            graphics.putString(new TerminalPosition(hudX, row++), "Sword: active");
        } else {
            graphics.putString(new TerminalPosition(hudX, row++), "Sword: locked");
        }

        // Place continue/quit hints near bottom to ensure visibility
        int terminalRows = graphics.getSize().getRows();
        int bottomRow = Math.max(row + 2, terminalRows - 3);
        // Provide Esc (return to main menu) above the restart/quit hints so it fits even on narrow HUDs
        graphics.putString(new TerminalPosition(hudX, bottomRow - 2), "Esc: Menu");
        if (showContinue) {
            graphics.putString(new TerminalPosition(hudX, bottomRow - 1), "Enter: Continue");
        } else {
            graphics.putString(new TerminalPosition(hudX, bottomRow - 1), "            ");
        }
        graphics.putString(new TerminalPosition(hudX, bottomRow), "R: restart   Q: quit");
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

    // Draw the "Next Pill:" label and a bordered preview box at (x,y). The label is drawn at (x,y),
    // and the box is directly below it occupying height h (so total consumed rows = 1 + h).
    private void drawPreviewBox(int x, int y, int w, int h, Pill pill) {
        // Draw label "Next Pill:" with pivot color if available
        String label = "Next Pill:";
        String labelColor = "#FFFFFF";
        if (pill != null) labelColor = mapColorForeground(pill.getColor1());
        graphics.setForegroundColor(TextColor.Factory.fromString(labelColor));
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        graphics.putString(new TerminalPosition(x, y), label);

        int boxTop = y + 1; // box sits below the label

        // Draw border using wall style
        graphics.setForegroundColor(TextColor.Factory.fromString(WALL_COLOR));
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        graphics.enableModifiers(SGR.BOLD);

        // Corners
        graphics.putString(new TerminalPosition(x, boxTop), CORNER_TL);
        graphics.putString(new TerminalPosition(x + w - 1, boxTop), CORNER_TR);
        graphics.putString(new TerminalPosition(x, boxTop + h - 1), CORNER_BL);
        graphics.putString(new TerminalPosition(x + w - 1, boxTop + h - 1), CORNER_BR);

        // Top/Bottom
        for (int cx = x + 1; cx < x + w - 1; cx++) {
            graphics.putString(new TerminalPosition(cx, boxTop), HORIZONTAL_LINE);
            graphics.putString(new TerminalPosition(cx, boxTop + h - 1), HORIZONTAL_LINE);
        }
        // Sides
        for (int ry = boxTop + 1; ry < boxTop + h - 1; ry++) {
            graphics.putString(new TerminalPosition(x, ry), VERTICAL_LINE);
            graphics.putString(new TerminalPosition(x + w - 1, ry), VERTICAL_LINE);
        }

        // Clear interior
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
        for (int ix = x + 1; ix < x + w - 1; ix++) {
            for (int iy = boxTop + 1; iy < boxTop + h - 1; iy++) {
                graphics.putString(new TerminalPosition(ix, iy), " ");
            }
        }

        // If there's a pill, render it centered. Each half is scaled according to SCALE_X/SCALE_Y.
        if (pill != null) {
            int interiorX = x + 1;
            int interiorY = boxTop + 1;
            int interiorW = w - 2;
            int interiorH = h - 2;

            // Use the same scaling as the main arena so preview matches in-game
            final int halfW = this.SCALE_X;
            final int halfH = this.SCALE_Y;

            // positions for pivot and other top-left corners
            int pivotX = interiorX;
            int pivotY = interiorY;
            int otherX = interiorX;
            int otherY = interiorY;

            switch (pill.getOrientation()) {
                case 0: // Right: pivot on left, other on right
                    pivotX = interiorX + 0;
                    pivotY = interiorY + (interiorH - halfH) / 2;
                    otherX = interiorX + halfW;
                    otherY = pivotY;
                    break;
                case 2: // Left: pivot on right, other on left
                    otherX = interiorX + 0;
                    otherY = interiorY + (interiorH - halfH) / 2;
                    pivotX = interiorX + halfW;
                    pivotY = otherY;
                    break;
                case 1: // Up: other above pivot => pivot bottom
                    pivotX = interiorX + (interiorW - halfW) / 2;
                    pivotY = interiorY + halfH;
                    otherX = pivotX;
                    otherY = interiorY + 0;
                    break;
                case 3: // Down: other below pivot => pivot top
                    pivotX = interiorX + (interiorW - halfW) / 2;
                    pivotY = interiorY + 0;
                    otherX = pivotX;
                    otherY = interiorY + halfH;
                    break;
                default:
                    pivotX = interiorX + 0;
                    pivotY = interiorY + (interiorH - halfH) / 2;
                    otherX = interiorX + halfW;
                    otherY = pivotY;
            }

            // Draw pivot half (color1)
            graphics.setForegroundColor(TextColor.Factory.fromString(mapColorForeground(pill.getColor1())));
            graphics.enableModifiers(SGR.BOLD);
            for (int dx = 0; dx < halfW; dx++) {
                for (int dy = 0; dy < halfH; dy++) {
                    graphics.putString(new TerminalPosition(pivotX + dx, pivotY + dy), "◉");
                }
            }

            // Draw other half (color2)
            graphics.setForegroundColor(TextColor.Factory.fromString(mapColorForeground(pill.getColor2())));
            for (int dx = 0; dx < halfW; dx++) {
                for (int dy = 0; dy < halfH; dy++) {
                    graphics.putString(new TerminalPosition(otherX + dx, otherY + dy), "●");
                }
            }
            graphics.disableModifiers(SGR.BOLD);
        }

        graphics.disableModifiers(SGR.BOLD);
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_COLOR));
    }
}
