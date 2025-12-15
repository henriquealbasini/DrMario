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

        String title = "DR-LIKE - JAVA EDITION";
        String subtitle = "Press Enter to Start";
        String controls = "Arrows: Move/Rotate  |  Q: Quit";
        String difficultyLine = "Difficulty: " + difficulty.name() + "  (Left/Right or A/D to change)";

        int centerY = terminalSize.getRows() / 2;

        putCentered(centerY - 2, title, TITLE_COLOR, true);
        putCentered(centerY, subtitle, SUB_COLOR, false);
        putCentered(centerY + 1, difficultyLine, SUB_COLOR, false);
        putCentered(centerY + 3, controls, SUB_COLOR, false);
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
}
