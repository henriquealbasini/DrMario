package com.g06.model;

import java.util.Random;

public class Block extends Element {

    private static final String[] TEXTURE_CHARS = {"▣", "■", "▤", "▥", "▦", "▧", "▨", "▩"};
    private static final Random random = new Random();

    private String color;
    private int textureID;
    private Boolean isVirus;


    // Constructor for subclasses
    protected Block(int x, int y, String color, int textureID, boolean isVirus) {
        super(x, y);
        this.color = color;
        this.textureID = textureID;
        this.isVirus = isVirus;
    }

    // Constructor for blocks
    public Block(int x, int y, String color) {
        this(x, y, color, random.nextInt(8), false);
    }

    public String getColor() {
        return color;
    }

    public Boolean isVirus() {
        return isVirus;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getTextureID() {
        return textureID;
    }
}