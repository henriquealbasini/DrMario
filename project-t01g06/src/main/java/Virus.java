public class Virus extends Block{
    private static final int VIRUS_TEXTURE_ID = 8; // Texture "X"

    public Virus(int x, int y, String color) {
        super(x, y, color, VIRUS_TEXTURE_ID, true);

    }
}
