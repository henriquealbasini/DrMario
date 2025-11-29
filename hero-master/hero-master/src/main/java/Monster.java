import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.util.Random;

public class Monster extends Element{

    public Monster(int x, int y) {super(x,y);}

    @Override
    public void draw(TextGraphics screen) {
        screen.setForegroundColor(TextColor.Factory.fromString("#339933"));
        screen.enableModifiers(SGR.BOLD);
        screen.putString(new TerminalPosition(getPosition().getX(), getPosition().getY()), "m");
    }

    public Position move(Arena arena){
        Random random = new Random();
        while(true) {
            Position ret = new Position(this.getPosition().getX() + random.nextInt(3) - 1,
                    this.getPosition().getY() + random.nextInt(3) - 1);
            if(ret.getX() > 0 && ret.getX() < arena.getWidth()-1 &&
                ret.getY() > 0 && ret.getY() < arena.getHeight()-1)
                return ret;
        }
    }
}
