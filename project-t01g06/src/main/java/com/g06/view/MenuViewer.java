package com.g06.view;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.g06.controller.Difficulty;
import com.g06.controller.MenuController;

public class MenuViewer {

    private final TextGraphics graphics;
    private static final String DEFAULT_BG = "#000000";
    private static final String TITLE_COLOR = "#00ff88";
    private static final String SUB_COLOR = "#bbbbbb";

    public MenuViewer(TextGraphics graphics) {
        this.graphics = graphics;
    }

    // Shorter ASCII art for title
    private static final String[] TITLE_ASCII = new String[] {
            " ___   ___       ",
            "| | \\ | |_)  __  ",
            "|_|_/ |_| \\ (_() ",
            "",
            " _      __    _     _____  ____  ___   _       __   ",
            "| |    / /\\  | |\\ |  | |  | |_  | |_) | |\\ |  / /\\  ",
            "|_|__ /_/--\\ |_| \\|  |_|  |_|__ |_| \\ |_| \\| /_/--\\"
    };

    // Compact start menu. showDifficulty controls whether the difficulty line is shown.
    public void drawStartMenu(TerminalSize terminalSize, Difficulty difficulty, MenuController.Mode mode, boolean showDifficulty) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        int maxCols = graphics.getSize().getColumns();
        int centerRow = terminalSize.getRows() / 2;

        // draw ASCII title centered, truncating lines if needed
        int titleStartRow = Math.max(0, centerRow - TITLE_ASCII.length - 2);
        for (int i = 0; i < TITLE_ASCII.length; i++) {
            String line = TITLE_ASCII[i];
            if (line.length() > maxCols - 2) line = line.substring(0, Math.max(0, maxCols - 2));
            putCentered(titleStartRow + i, line, TITLE_COLOR, true);
        }

        int row = titleStartRow + TITLE_ASCII.length + 1;
        putCentered(row++, "Press Enter to Start", SUB_COLOR, false);
        if (showDifficulty) {
            putCentered(row++, "Difficulty: " + difficulty.name() + " (D)", SUB_COLOR, false);
        }
        putCentered(row++, "Mode: " + mode.name() + " (M)", SUB_COLOR, false);
        putCentered(row, "Q: Quit | I: Instructions", SUB_COLOR, false);
    }

    public void drawGameOver(TerminalSize terminalSize) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "GAME OVER";
        String subtitle = "Press R to Restart or Q to Quit";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 1, title, TITLE_COLOR, true);
        putCentered(centerY + 1, subtitle, SUB_COLOR, false);
    }

    // Game over that can show the level number (used for LEVELS mode)
    public void drawGameOverWithLevel(TerminalSize terminalSize, int level, boolean showLevel) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "GAME OVER";
        String subtitle = showLevel ? ("You lost on level " + level) : "Press R to Restart or Q to Quit";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 1, title, TITLE_COLOR, true);
        putCentered(centerY + 1, subtitle, SUB_COLOR, false);
    }

    // Game over showing score for ENDLESS mode
    public void drawGameOverWithScore(TerminalSize terminalSize, int score) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "GAME OVER";
        String subtitle = "You scored " + score + " points";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 1, title, TITLE_COLOR, true);
        putCentered(centerY + 1, subtitle, SUB_COLOR, false);
    }

    // Draw victory overlay WITHOUT clearing the background, so HUD remains visible
    public void drawVictoryOverlay(TerminalSize terminalSize, int level) {
        String title = "LEVEL CLEARED!";
        String subtitle = "You cleared level " + level;
        String restart = "Press Enter to continue | R: Restart | Q: Quit";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 2, title, "#ffe112", true);
        putCentered(centerY, subtitle, "#ffffff", false);
        putCentered(centerY + 2, restart, SUB_COLOR, false);
    }

    private void putCentered(int row, String text, String color, boolean bold) {
        int maxCols = graphics.getSize().getColumns();
        String display = text;
        // truncate if too long for the terminal width
        if (display.length() > maxCols - 2) {
            display = display.substring(0, Math.max(0, maxCols - 2));
        }
        int col = Math.max(0, (maxCols - display.length()) / 2);
        graphics.setForegroundColor(TextColor.Factory.fromString(color));
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        if (bold) graphics.enableModifiers(SGR.BOLD);
        graphics.putString(new TerminalPosition(col, row), display);
        if (bold) graphics.disableModifiers(SGR.BOLD);
    }

    public void drawVictory(TerminalSize terminalSize, int level) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "LEVEL CLEARED!";
        String subtitle = "You cleared level " + level;
        String restart = "Press Enter to continue | R: Restart | Q: Quit";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 2, title, "#ffe112", true);
        putCentered(centerY, subtitle, "#ffffff", false);
        putCentered(centerY + 2, restart, SUB_COLOR, false);
    }
    public void drawInstructions(TerminalSize terminalSize) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "INSTRUCTIONS";

        int centerY = terminalSize.getRows() / 2;
        int startY = centerY - 15;

        putCentered(startY, title, TITLE_COLOR, true);

        putCentered(startY + 2, "A/D or Left/Right: Move", "#FFFFFF", false);
        putCentered(startY + 4, "W or Up Arrow: Rotate", "#FFFFFF", false);
        putCentered(startY + 6, "S or Down Arrow: Soft Drop", "#FFFFFF", false);
        putCentered(startY + 8, "D: Change Difficulty | M: Change Mode" , "#FFFFFF", false);
        putCentered(startY + 10, "R: Restart Game | Q: Quit Game" , "#FFFFFF", false);
        putCentered(startY + 12, "ESC: Return to Menu", SUB_COLOR, true);

        // Power-up description: Sword
        putCentered(startY + 14, "POWER-UP: SWORD", "#ffe112", true);
        putCentered(startY + 16, "Press Space to deploy a falling sword (1x4).", "#FFFFFF", false);
        putCentered(startY + 18, "Sword breaks blocks and viruses as it falls, scoring per block.", "#FFFFFF", false);
        putCentered(startY + 20, "In LEVELS: you gain 1 charge every 2 levels.", "#FFFFFF", false);
        putCentered(startY + 22, "In ENDLESS: you gain 1 charge per 30 cleared blocks.", "#FFFFFF", false);
    }
}
