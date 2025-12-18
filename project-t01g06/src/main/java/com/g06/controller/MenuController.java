package com.g06.controller;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class MenuController implements Controller {

    public enum MenuAction { NONE, START, INSTRUCTIONS, RESTART, QUIT }

    // Novo Enum para o Modo de Jogo
    public enum Mode { CLASSIC, INFINITE }

    private MenuAction lastAction = MenuAction.NONE;
    private Difficulty difficulty = Difficulty.EASY;
    private Mode mode = Mode.CLASSIC; // Valor por defeito

    @Override
    public void processKey(KeyStroke key) {
        if (key == null) return;
        lastAction = MenuAction.NONE;

        if (key.getKeyType() == KeyType.Enter) {
            lastAction = MenuAction.START;
        }
        else if (key.getKeyType() == KeyType.Escape) {
            lastAction = MenuAction.RESTART;
        }
        else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
            switch (Character.toLowerCase(key.getCharacter())) {
                case 'q':
                    lastAction = MenuAction.QUIT;
                    break;
                case 'r':
                    lastAction = MenuAction.RESTART;
                    break;
                case 'i':
                    lastAction = MenuAction.INSTRUCTIONS;
                    break;
                // --- MUDAR MODO (Tecla M) ---
                case 'm':
                    if (mode == Mode.CLASSIC) mode = Mode.INFINITE;
                    else mode = Mode.CLASSIC;
                    break;
                // --- MUDAR DIFICULDADE (Exemplo com setas, opcional) ---
                // case 'd': ... logica para mudar dificuldade ...
            }
        }
    }

    public MenuAction getAction() {
        return lastAction;
    }

    public void resetAction() {
        this.lastAction = MenuAction.NONE;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Mode getMode() {
        return mode;
    }
}