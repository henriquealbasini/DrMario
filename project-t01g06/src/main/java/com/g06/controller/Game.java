package com.g06.controller;

import com.g06.model.Arena;
import com.g06.view.ArenaViewer;
import com.g06.view.MenuViewer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;

public class Game {

    private final int SCALE_X = 3;
    private final int SCALE_Y = 2;
    private final int RES_X = 14; // Ligeiramente mais largo para caber o título ASCII
    private final int RES_Y = 20;
    private final long FALL_DELAY_MS = 500;

    private Screen screen;
    private Arena arena;
    private ArenaController arenaController;
    private ArenaViewer arenaViewer;
    private MenuController menuController;
    private MenuViewer menuViewer;

    private enum GameState { READY, INSTRUCTIONS, PLAYING, GAME_OVER, VICTORY }
    private GameState gameState;

    public Game() {
        try {
            TerminalSize terminalSize = new TerminalSize(RES_X * SCALE_X, RES_Y * SCALE_Y);
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory().setInitialTerminalSize(terminalSize);
            Terminal terminal = terminalFactory.createTerminal();
            screen = new TerminalScreen(terminal);

            screen.setCursorPosition(null);
            screen.startScreen();
            screen.doResizeIfNecessary();

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.menuViewer = new MenuViewer(screen.newTextGraphics());
        this.menuController = new MenuController();
        this.gameState = GameState.READY;
    }

    public void run() {
        long lastFallTime = System.currentTimeMillis();

        try {
            while (true) {
                KeyStroke key = screen.pollInput();
                if (key != null && key.getKeyType() == KeyType.EOF) break;

                switch (gameState) {
                    case READY:
                        menuController.processKey(key);
                        if (menuController.getAction() == MenuController.MenuAction.START) {
                            resetGame();
                            gameState = GameState.PLAYING;
                            menuController.resetAction();
                        }
                        else if (menuController.getAction() == MenuController.MenuAction.INSTRUCTIONS) {
                            gameState = GameState.INSTRUCTIONS;
                            menuController.resetAction();
                        }
                        else if (menuController.getAction() == MenuController.MenuAction.QUIT) {
                            screen.close();
                            return;
                        }
                        break;

                    case INSTRUCTIONS:
                        menuController.processKey(key); // Usa o menu controller para detetar ESC
                        if (menuController.getAction() == MenuController.MenuAction.RESTART ||
                                (key != null && key.getKeyType() == KeyType.Escape)) {
                            gameState = GameState.READY;
                            menuController.resetAction();
                        }
                        break;

                    case PLAYING:
                        if (key != null) {
                            if (key.getKeyType() == KeyType.Character && key.getCharacter() == 'q') {
                                screen.close();
                                return;
                            }
                            arenaController.processKey(key);
                        }

                        // Lógica automática (Gravidade)
                        if (System.currentTimeMillis() - lastFallTime > FALL_DELAY_MS) {
                            arenaController.fallPill();
                            lastFallTime = System.currentTimeMillis();
                        }

                        // Lógica automática (Monstro)
                        arenaController.step();

                        if (arenaController.isGameOver()) gameState = GameState.GAME_OVER;
                        else if (arenaController.isVictory()) gameState = GameState.VICTORY;
                        break;

                    case GAME_OVER:
                    case VICTORY:
                        menuController.processKey(key);
                        if (menuController.getAction() == MenuController.MenuAction.RESTART) {
                            resetGame();
                            gameState = GameState.PLAYING;
                            menuController.resetAction();
                        } else if (menuController.getAction() == MenuController.MenuAction.QUIT) {
                            screen.close();
                            return;
                        }
                        break;
                }

                draw();
                Thread.sleep(20);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void resetGame() {
        this.arena = new Arena(RES_X, RES_Y);
        // Atualiza conforme o construtor do teu ArenaViewer (2 ou 3 argumentos)
        // Se der erro aqui, apaga o SCALE_X e SCALE_Y
        this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
        this.arenaController = new ArenaController(arena);
    }

    private void draw() throws IOException {
        screen.clear();

        switch (gameState) {
            case READY:
                // --- AQUI ESTÁ A CORREÇÃO DA CHAMADA ---
                menuViewer.drawStartMenu(
                        screen.getTerminalSize(),
                        menuController.getDifficulty(),
                        menuController.getMode(), // Argumento 3: Modo
                        true                      // Argumento 4: Show Difficulty
                );
                break;
            case INSTRUCTIONS:
                menuViewer.drawInstructions(screen.getTerminalSize());
                break;
            case PLAYING:
                if (arena != null) arenaViewer.draw(arena);
                break;

            case GAME_OVER:
                // --- CORREÇÃO: Passar 3 argumentos ---
                menuViewer.drawGameOver(
                        screen.getTerminalSize(),
                        menuController.getDifficulty(),
                        menuController.getMode()
                );
                break;

            case VICTORY:
                // --- CORREÇÃO: Passar 3 argumentos ---
                menuViewer.drawVictory(
                        screen.getTerminalSize(),
                        menuController.getDifficulty(),
                        menuController.getMode()
                );
                break;
        }

        screen.refresh();
    }
}