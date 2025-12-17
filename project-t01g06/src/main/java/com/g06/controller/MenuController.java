package com.g06.controller;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Very small controller for menu screens. It only listens for Enter, 'r' (restart) and 'q' (quit).
 * Extended to allow difficulty selection via Left/Right arrows or A/D keys.
 */
public class MenuController implements Controller {

    public enum MenuAction { NONE, START, INSTRUCTIONS, RESTART, QUIT }

    private MenuAction lastAction = MenuAction.NONE;
    private Difficulty difficulty = Difficulty.NORMAL;

    public MenuController() {}

    public MenuAction consumeAction() {
        MenuAction a = lastAction;
        lastAction = MenuAction.NONE;
        return a;
    }

    public Difficulty getDifficulty() { return difficulty; }

    @Override
    public void processKey(KeyStroke key) {
        if (key == null) return;

        // Difficulty selection: Arrows or A/D
        if (key.getKeyType() == KeyType.ArrowLeft) {
            difficulty = difficulty.previous();
            return;
        }
        if (key.getKeyType() == KeyType.ArrowRight) {
            difficulty = difficulty.next();
            return;
        }

        if (key.getKeyType() == KeyType.Character) {
            Character c = key.getCharacter();
            if (c != null) {
                switch (Character.toLowerCase(c)) {
                    case 'q':
                        lastAction = MenuAction.QUIT;
                        break;
                    case 'r':
                        lastAction = MenuAction.RESTART;
                        break;
                    case 'i':
                        lastAction = MenuAction.INSTRUCTIONS;
                        break;
                    case 'a':
                        difficulty = difficulty.previous();
                        break;
                    case 'd':
                        difficulty = difficulty.next();
                        break;
                    default:
                        break;
                }
            }
        } else if (key.getKeyType() == KeyType.Enter) {
            lastAction = MenuAction.START;
        } else if (key.getKeyType() == KeyType.Escape) {
            lastAction = MenuAction.RESTART; // Reutilizamos RESTART para "Voltar ao Início"
        }
    }
}
