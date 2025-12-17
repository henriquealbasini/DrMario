package com.g06.controller;

import com.g06.model.Arena;
import com.g06.view.ArenaViewer;
import com.g06.view.MenuViewer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Game {

    private long fallDelayMs = 500; // made instance-level and dynamic
    private final int SCALE_X = 3;
    private final int SCALE_Y = 2;
    private final int RES_X = 10;
    private final int RES_Y = 18;

    private Screen screen;
    private Arena arena;
    private ArenaViewer arenaViewer;
    private MenuViewer menuViewer;

    private Controller controller; // active controller
    private ArenaController arenaController; // concrete arena controller
    private MenuController menuController;

    private GameState gameState = GameState.READY;

    private boolean headless = false; // fallback to console if Lanterna unavailable

    private enum GameState { READY, INSTRUCTIONS, PLAYING, GAME_OVER, VICTORY }

    public Game(){
        try {
            TerminalSize terminalSize = new TerminalSize(RES_X * SCALE_X, RES_Y * SCALE_Y);
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory().setInitialTerminalSize(terminalSize);
            Terminal terminal = terminalFactory.createTerminal();
            screen = new TerminalScreen(terminal);

            screen.setCursorPosition(null);
            screen.startScreen();
            screen.doResizeIfNecessary();
        } catch (IOException e){
            // If terminal creation fails (common on some Windows setups), fallback to console mode
            System.err.println("Lanterna terminal unavailable, falling back to console mode: " + e.getMessage());
            headless = true;
        }

        this.arena = new Arena(RES_X, RES_Y);
        if (!headless) {
            this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
            this.menuViewer = new MenuViewer(screen.newTextGraphics());
        }

        this.arenaController = new ArenaController(arena);
        this.menuController = new MenuController();
        this.controller = menuController; // start in menu

        // Initialize fall delay from menu default
        this.fallDelayMs = menuController.getDifficulty().getDelayMs();
    }

    public void run() {
        if (headless) {
            runHeadless();
            return;
        }

        long lastFallTime = System.currentTimeMillis();

        try {
            while(true) {
                // INPUT
                com.googlecode.lanterna.input.KeyStroke key = screen.pollInput();
                if (key != null) {
                    // Global quit
                    if (key.getKeyType() == KeyType.Character && key.getCharacter() == ('q')) {
                        screen.close();
                        break;
                    }
                    if (key.getKeyType() == KeyType.EOF) break;

                    // Route to active controller
                    controller.processKey(key);

                    // Menu actions
                    if (controller == menuController) {
                        MenuController.MenuAction action = menuController.consumeAction();
                        // update fall delay live when difficulty changes
                        this.fallDelayMs = menuController.getDifficulty().getDelayMs();

                        if (action == MenuController.MenuAction.START) {
                            // Start playing
                            gameState = GameState.PLAYING;
                            controller = arenaController;

                        } else if (action == MenuController.MenuAction.INSTRUCTIONS) {
                            gameState = GameState.INSTRUCTIONS;

                        } else if (action == MenuController.MenuAction.RESTART) {
                            if (gameState == GameState.INSTRUCTIONS) {
                                gameState = GameState.READY;
                            }
                            else{
                                    resetGame();
                                    gameState = GameState.PLAYING;
                                    controller = arenaController;
                                }

                        } else if (action == MenuController.MenuAction.QUIT) {
                            screen.close();
                            break;
                        }
                    }
                }

                // GRAVIDADE / tick only when playing
                if (gameState == GameState.PLAYING) {
                    if (System.currentTimeMillis() - lastFallTime > fallDelayMs) {
                        arenaController.fallPill();
                        lastFallTime = System.currentTimeMillis();
                    }

                    // If arena reports game over, switch state
                    if (arenaController.isGameOver()) {
                        gameState = GameState.GAME_OVER;
                        controller = menuController; // use menu controller to accept restart/quit
                    }
                    else if (arenaController.isVictory()) {
                        gameState = GameState.VICTORY;
                        controller = menuController; // Permite reiniciar ou sair
                    }
                }

                draw();

                // Pequena pausa para não usar 100% do CPU
                Thread.sleep(20);
            }
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    private void runHeadless() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (true) {
                showStartMenuConsole();
                String line = reader.readLine(); // wait for Enter or input
                if (line == null) break;
                // Start on empty line or Enter
                gameState = GameState.PLAYING;
                controller = arenaController;

                // set fallDelay according to menu difficulty in headless mode (user may use a/d to change previous)
                this.fallDelayMs = menuController.getDifficulty().getDelayMs();

                // Play loop
                long lastFallTime = System.currentTimeMillis();
                boolean quit = false;
                while (!quit) {
                    // Non-blocking input check
                    if (reader.ready()) {
                        String input = reader.readLine();
                        if (input != null && !input.isEmpty()) {
                            char c = Character.toLowerCase(input.charAt(0));
                            switch (c) {
                                case 'q':
                                    quit = true; break;
                                case 'a':
                                    menuController.processKey(new com.googlecode.lanterna.input.KeyStroke('a', false, false));
                                    // update difficulty
                                    this.fallDelayMs = menuController.getDifficulty().getDelayMs();
                                    break;
                                case 'd':
                                    menuController.processKey(new com.googlecode.lanterna.input.KeyStroke('d', false, false));
                                    this.fallDelayMs = menuController.getDifficulty().getDelayMs();
                                    break;
                                case 'A':
                                case 'D':
                                    // handled above by lowercasing, but keep for safety
                                    break;
                                case 'r':
                                    // restart
                                    resetGame();
                                    quit = true; break;
                                case 's':
                                    arenaController.fallPill(); break;
                                case 'w':
                                    arenaController.rotatePill(); break;
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                    // optional numeric shortcuts for difficulty
                                    int idx = Character.getNumericValue(c) - 1;
                                    Difficulty[] vals = Difficulty.values();
                                    if (idx >= 0 && idx < vals.length) {
                                        // set in menu controller
                                        while (menuController.getDifficulty() != vals[idx]) {
                                            menuController.processKey(new com.googlecode.lanterna.input.KeyStroke('d', false, false));
                                        }
                                        this.fallDelayMs = menuController.getDifficulty().getDelayMs();
                                    }
                                    break;
                                default:
                                    arenaController.processKey(new com.googlecode.lanterna.input.KeyStroke(c, false, false));
                                    break;
                            }
                        }
                    }

                    if (System.currentTimeMillis() - lastFallTime > fallDelayMs) {
                        arenaController.fallPill();
                        lastFallTime = System.currentTimeMillis();
                    }

                    if (arenaController.isGameOver()) {
                        // Show game over console and wait for r or q
                        showGameOverConsole();
                        String resp = reader.readLine();
                        if (resp == null) { quit = true; break; }
                        if (!resp.isEmpty()) {
                            char c = Character.toLowerCase(resp.charAt(0));
                            if (c == 'r') {
                                resetGame();
                                break; // break to outer loop to show start menu
                            } else if (c == 'q') {
                                quit = true; break;
                            }
                        }
                    }

                    Thread.sleep(50);
                }

                if (quit) break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void showStartMenuConsole() {
        System.out.println("\n=============================\n");
        System.out.println("   DR-LIKE - JAVA EDITION\n");
        System.out.println("   Press Enter to start");
        System.out.println("   Controls: a:left  d:right  w:rotate  s:drop  q:quit");
        System.out.println("   Difficulty: " + menuController.getDifficulty().name() + " (A/D or 1-4 to change)");
        System.out.println("\n=============================\n");
        System.out.print("> ");
    }

    private void showGameOverConsole() {
        System.out.println("\n===== GAME OVER =====");
        System.out.println("Press R to restart or Q to quit");
        System.out.print("> ");
    }

    private void resetGame() {
        this.arena = new Arena(RES_X, RES_Y);
        this.arenaController = new ArenaController(arena);
        if (!headless) this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
        // Update fallDelay according to menu selection
        this.fallDelayMs = menuController.getDifficulty().getDelayMs();
    }

    private void draw() throws IOException{
        screen.clear();
        if (gameState == GameState.READY) {
            menuViewer.drawStartMenu(screen.getTerminalSize(), menuController.getDifficulty());
        } else if (gameState == GameState.INSTRUCTIONS) {
            menuViewer.drawInstructions(screen.getTerminalSize());

        } else if (gameState == GameState.PLAYING) {
            arenaViewer.draw(arena);
        } else {
            menuViewer.drawGameOver(screen.getTerminalSize());
        }
        if (gameState == GameState.VICTORY) {
            menuViewer.drawVictory(screen.getTerminalSize());
        }
        screen.refresh();
    }
}

