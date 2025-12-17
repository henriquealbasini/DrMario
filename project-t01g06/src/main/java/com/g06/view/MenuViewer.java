package com.g06.view;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.g06.controller.Difficulty;

public class MenuViewer {

    private final TextGraphics graphics;
    private static final String DEFAULT_BG = "#000000";
    private static final String TITLE_COLOR = "#00ff88";
    private static final String SUB_COLOR = "#bbbbbb";

    public MenuViewer(TextGraphics graphics) {
        this.graphics = graphics;
    }

    public void drawStartMenu(TerminalSize terminalSize, Difficulty difficulty) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "DR-LANTERNA";
        String subtitle = "Press Enter to Start";
        String quit = "Q: Quit | I: Instructions";
        String difficultyLine = "Difficulty: " + difficulty.name();

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 2, title, TITLE_COLOR, true);
        putCentered(centerY, subtitle, SUB_COLOR, false);
        putCentered(centerY + 2, difficultyLine, SUB_COLOR, false);
        putCentered(centerY + 4, quit, SUB_COLOR, false);
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

    private void putCentered(int row, String text, String color, boolean bold) {
        int col = Math.max(0, (graphics.getSize().getColumns() - text.length()) / 2);
        graphics.setForegroundColor(TextColor.Factory.fromString(color));
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        if (bold) graphics.enableModifiers(SGR.BOLD);
        graphics.putString(new TerminalPosition(col, row), text);
        if (bold) graphics.disableModifiers(SGR.BOLD);
    }
    public void drawVictory(TerminalSize terminalSize) {
        graphics.setBackgroundColor(TextColor.Factory.fromString(DEFAULT_BG));
        graphics.fillRectangle(new TerminalPosition(0,0), terminalSize, ' ');

        String title = "YOU WIN!";
        String subtitle = "All viruses eliminated";
        String restart = "Press R to Restart or Q to Quit";

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
        int startY = centerY - 5;

        putCentered(startY, title, TITLE_COLOR, true);

        putCentered(startY + 2, "Left/Right Arrows: Move", "#FFFFFF", false);
        putCentered(startY + 4, "Up Arrow: Rotate", "#FFFFFF", false);
        putCentered(startY + 6, "Down Arrow: Soft Drop", "#FFFFFF", false);
        putCentered(startY + 8, "Q: Quit Game", "#FFFFFF", false);

        putCentered(startY + 10, "ESC: Return to Menu", SUB_COLOR, true);
    }
}
