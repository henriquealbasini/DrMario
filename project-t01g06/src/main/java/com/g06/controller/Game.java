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
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;

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

    // Level system
    private int level = 1;
    private Instant levelStartTime = null;
    private long levelElapsedBeforePause = 0; // seconds
    // cumulative power-up state carried across levels
    private int cumulativeSwordCharges = 0;
    private int cumulativeBlocksCleared = 0;
    // number of levels the player has completed so far (used to grant swords every 2 completed levels)
    private int levelsCompleted = 0;

    public Game(){
        try {
            // Increase terminal width to accommodate HUD (add HUD columns)
            int hudExtra = 24;
            TerminalSize terminalSize = new TerminalSize((RES_X * SCALE_X) + hudExtra, RES_Y * SCALE_Y);
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory().setInitialTerminalSize(terminalSize);

            terminalFactory.setForceAWTOverSwing(true); // force AWT-based terminal frame
            try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/terminus.ttf")) {
                if (fontStream != null) {
                    Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge.registerFont(baseFont);
                    Font sized = baseFont.deriveFont(Font.PLAIN, 24f);
                    AWTTerminalFontConfiguration fontConfig = AWTTerminalFontConfiguration.newInstance(sized);
                    terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);
                } else {
                    // Resource not found; use a larger default as a fallback
                    terminalFactory.setTerminalEmulatorFontConfiguration(AWTTerminalFontConfiguration.getDefaultOfSize(24));
                    System.err.println("Warning: custom font resource 'fonts/square.ttf' not found — using default font size 24.");
                }
            } catch (FontFormatException | IOException e) {
                System.err.println("Warning: failed to load custom font, using default font. Reason: " + e.getMessage());
                terminalFactory.setTerminalEmulatorFontConfiguration(AWTTerminalFontConfiguration.getDefaultOfSize(24));
            }

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

        // Initialize with level 1 (don't start timer yet)
         this.arena = new Arena(RES_X, RES_Y, computeVirusCountForLevel(1));
        // Ensure arena knows the current level so controllers can initialize powerups
        this.arena.setLevel(this.level);
         if (!headless) {
             this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
             this.menuViewer = new MenuViewer(screen.newTextGraphics());
         }

         this.menuController = new MenuController();
         this.arenaController = new ArenaController(arena, false, menuController.getDifficulty(), cumulativeSwordCharges, cumulativeBlocksCleared);
         this.controller = menuController; // start in menu

        // Initialize fall delay from menu default
        this.fallDelayMs = menuController.getDifficulty().getDelayMs();
    }

    private int computeVirusCountForLevel(int lvl) {
        // Safe scaling: base 5, +2 per level, capped by a reasonable max derived from arena size
        int base = 5;
        int incr = 2;
        int maxByArea = (RES_X * RES_Y) / 6; // heuristic cap
        int max = Math.max(10, maxByArea); // ensure at least 10 cap
        int val = base + (lvl - 1) * incr;
        if (val > max) val = max;
        return val;
    }

    private long computeFallDelayForLevel(int lvl) {
        // Endless mode: decay factor with floor
        long base = menuController.getDifficulty().getDelayMs();
        double decay = 0.95; // gentle
        long minDelay = 80;
        double v = base * Math.pow(decay, lvl - 1);
        long val = Math.max(minDelay, Math.round(v));
        return val;
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

                    // Allow returning to main menu from gameplay (press Escape)
                    if (key.getKeyType() == KeyType.Escape && controller == arenaController) {
                        // Go back to menu to allow changing settings; do not reset game automatically
                        controller = menuController;
                        gameState = GameState.READY;
                        // Clear any pending menu action
                        menuController.consumeAction();
                        continue;
                    }

                    // Route to active controller
                    controller.processKey(key);

                    // Menu actions
                    if (controller == menuController) {
                        MenuController.MenuAction action = menuController.consumeAction();
                        // update fall delay live when difficulty changes
                        this.fallDelayMs = menuController.getDifficulty().getDelayMs();

                        if (action == MenuController.MenuAction.START) {
                            if (gameState == GameState.VICTORY) {
                                // Continue to next level when ENTER pressed on victory
                                // Persist sword state from current controller so charges do not reset
                                if (arenaController != null) {
                                    cumulativeSwordCharges = ((ArenaController)arenaController).getSwordCharges();
                                    cumulativeBlocksCleared = ((ArenaController)arenaController).getBlocksCleared();
                                }
                                // Mark the completed level and grant charges per 2 completed levels
                                levelsCompleted++;
                                if (levelsCompleted % 2 == 0) {
                                    cumulativeSwordCharges++;
                                }
                                level = level + 1;
                                startLevel(level);
                                continue;
                            }

                            // Start playing from menu - ensure timer and proper setup
                            startLevel(level);
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
                        // Capture elapsed time up to victory and pause the timer display
                        if (levelStartTime != null) {
                            levelElapsedBeforePause = java.time.Duration.between(levelStartTime, Instant.now()).getSeconds();
                        } else {
                            levelElapsedBeforePause = 0;
                        }
                        controller = menuController; // Permite reiniciar ou sair; wait for Enter to continue
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
                String line = reader.readLine(); // wait for Enter or input
                if (line == null) break;
                // Start on empty line or Enter
                startLevel(level);

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
                                case 'm':
                                    menuController.processKey(new com.googlecode.lanterna.input.KeyStroke('m', false, false));
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
                                case '\n':
                                case '\r':
                                    // If victory, continue to next level
                                    if (gameState == GameState.VICTORY) {
                                        if (arenaController != null) {
                                            cumulativeSwordCharges = ((ArenaController)arenaController).getSwordCharges();
                                            cumulativeBlocksCleared = ((ArenaController)arenaController).getBlocksCleared();
                                        }
                                        level = level + 1;
                                        startLevel(level);
                                        break;
                                    }
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
                        if (menuController.getMode() == MenuController.Mode.LEVELS) {
                            System.out.println("\n===== GAME OVER - You lost on level " + level + " =====");
                        } else {
                            // ENDLESS: show score
                            int s = 0;
                            if (arenaController != null) s = ((ArenaController)arenaController).getScore();
                            System.out.println("\n===== GAME OVER - You scored " + s + " points =====");
                        }
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

                    if (arenaController.isVictory()) {
                        System.out.println("\n===== LEVEL CLEARED! Press Enter to continue or R to restart or Q to quit =====");
                        String resp = reader.readLine();
                        if (resp == null) { quit = true; break; }
                        if (resp.isEmpty()) {
                            // persist sword state and award charges per 2 completed levels
                            if (arenaController != null) {
                                cumulativeSwordCharges = ((ArenaController)arenaController).getSwordCharges();
                                cumulativeBlocksCleared = ((ArenaController)arenaController).getBlocksCleared();
                            }
                            levelsCompleted++;
                            if (levelsCompleted % 2 == 0) cumulativeSwordCharges++;
                            level = level + 1;
                            startLevel(level);
                            break; // go back to main loop to show updated state
                        }
                        char c = Character.toLowerCase(resp.charAt(0));
                        if (c == 'r') { resetGame(); break; }
                        if (c == 'q') { quit = true; break; }
                    }

                    Thread.sleep(50);
                }

                if (quit) break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    
    private void resetGame() {
        this.level = 1;
        this.cumulativeSwordCharges = 0;
        this.cumulativeBlocksCleared = 0;
        this.levelsCompleted = 0;
         if (menuController.getMode() == MenuController.Mode.ENDLESS) {
             this.arena = new Arena(RES_X, RES_Y, 0); // no viruses in endless
             this.arena.setLevel(this.level);
             this.arenaController = new ArenaController(arena, true, menuController.getDifficulty(), cumulativeSwordCharges, cumulativeBlocksCleared);
         } else {
             this.arena = new Arena(RES_X, RES_Y, computeVirusCountForLevel(1));
             this.arena.setLevel(this.level);
             this.arenaController = new ArenaController(arena, false, menuController.getDifficulty(), cumulativeSwordCharges, cumulativeBlocksCleared);
         }
         if (!headless) this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
         // Update fallDelay according to menu selection
         this.fallDelayMs = menuController.getDifficulty().getDelayMs();
         this.levelStartTime = Instant.now();
         this.levelElapsedBeforePause = 0;
     }

     private void startLevel(int lvl) {
         this.level = lvl;
         if (menuController.getMode() == MenuController.Mode.ENDLESS) {
            // Endless: no viruses, always spawn pills until game over; use endless ArenaController
            this.arena = new Arena(RES_X, RES_Y, 0);
            this.arena.setLevel(this.level);
            this.arenaController = new ArenaController(arena, true, menuController.getDifficulty(), cumulativeSwordCharges, cumulativeBlocksCleared);
         } else {
             int virusCount = computeVirusCountForLevel(lvl);
             this.arena = new Arena(RES_X, RES_Y, virusCount);
             this.arena.setLevel(this.level);
             this.arenaController = new ArenaController(arena, false, menuController.getDifficulty(), cumulativeSwordCharges, cumulativeBlocksCleared);
         }
         if (!headless) this.arenaViewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);

        // Set fall delay according to mode
        if (menuController.getMode() == MenuController.Mode.ENDLESS) {
            this.fallDelayMs = computeFallDelayForLevel(lvl);
        } else {
            // LEVELS mode: respect selected difficulty
            this.fallDelayMs = menuController.getDifficulty().getDelayMs();
        }

        this.gameState = GameState.PLAYING;
        this.controller = arenaController;
        this.levelStartTime = Instant.now();
        this.levelElapsedBeforePause = 0;
    }

    private void draw() throws IOException{
        screen.clear();
        if (gameState == GameState.READY) {
            // Determine whether to show difficulty: hide if terminal too small to fit difficulty line
            boolean showDifficulty = screen.getTerminalSize().getColumns() > (RES_X * SCALE_X + 10);
            menuViewer.drawStartMenu(screen.getTerminalSize(), menuController.getDifficulty(), menuController.getMode(), showDifficulty);
        } else if (gameState == GameState.INSTRUCTIONS) {
            menuViewer.drawInstructions(screen.getTerminalSize());

        } else if (gameState == GameState.PLAYING) {
            // draw arena with HUD; no continue hint while playing
            int score = 0;
            if (menuController.getMode() == MenuController.Mode.ENDLESS && arenaController != null) {
                score = ((ArenaController)arenaController).getScore();
            }
            int swordCharges = 0;
            if (arenaController != null) swordCharges = ((ArenaController)arenaController).getSwordCharges();
            arenaViewer.draw(arena, level, menuController.getDifficulty(), menuController.getMode(), levelStartTime, 0, false, score, swordCharges);
        } else if (gameState == GameState.GAME_OVER) {
            if (menuController.getMode() == MenuController.Mode.LEVELS) {
                menuViewer.drawGameOverWithLevel(screen.getTerminalSize(), level, true);
            } else {
                int s = 0;
                if (arenaController != null) s = ((ArenaController)arenaController).getScore();
                menuViewer.drawGameOverWithScore(screen.getTerminalSize(), s);
            }
        }
        if (gameState == GameState.VICTORY) {
            // Show full-screen victory menu (clear screen) so the game is not visible behind the text
            menuViewer.drawVictory(screen.getTerminalSize(), level);
        }
        screen.refresh();
    }
}
