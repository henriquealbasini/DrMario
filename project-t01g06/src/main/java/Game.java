import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;

public class Game {

    private final long FALL_DELAY_MS = 500;
    private final int SCALE_X = 3;
    private final int SCALE_Y = 2;
    private final int RES_X = 10;
    private final int RES_Y = 18;

    private Screen screen;
    private Arena arena;
    private ArenaViewer viewer;

    public Game(){
        try {
            // Use SCALE_X for width and SCALE_Y for height
            TerminalSize terminalSize = new TerminalSize(RES_X * SCALE_X, RES_Y * SCALE_Y);
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory().setInitialTerminalSize(terminalSize);
            Terminal terminal = terminalFactory.createTerminal();
            screen = new TerminalScreen(terminal);

            screen.setCursorPosition(null);
            screen.startScreen();
            screen.doResizeIfNecessary();
        } catch (IOException e){
            e.printStackTrace();
        }

        this.arena = new Arena(RES_X, RES_Y);
        // Pass both scale factors to the Viewer
        this.viewer = new ArenaViewer(screen.newTextGraphics(), SCALE_X, SCALE_Y);
    }

    public void run() {
        long lastFallTime = System.currentTimeMillis();

        try {
            while(true) {
                com.googlecode.lanterna.input.KeyStroke key = screen.pollInput();
                if (key != null) {
                    processKey(key);

                    if (key.getKeyType() == KeyType.Character && key.getCharacter() == ('q'))
                        screen.close();
                    if (key.getKeyType() == KeyType.EOF)
                        break;
                }

                if (System.currentTimeMillis() - lastFallTime > FALL_DELAY_MS) {
                    boolean pillFell = arena.fallPill();

                    if (!pillFell) {
                        arena.settlePill();
                        arena.spawnNewPill();
                    }
                    lastFallTime = System.currentTimeMillis();
                }

                draw();
                Thread.sleep(20);
            }
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    private void draw() throws IOException{
        screen.clear();
        viewer.draw(arena);
        screen.refresh();
    }

    private void processKey(com.googlecode.lanterna.input.KeyStroke key){
        switch (key.getKeyType()) {
            case ArrowLeft:
                arena.movePillLeft();
                break;
            case ArrowRight:
                arena.movePillRight();
                break;
            case ArrowUp:
                arena.rotatePill();
                break;
            case ArrowDown:
                arena.fallPill();
                break;
            default:
                break;
        }
    }
}