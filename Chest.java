import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

class Chest {
    int x, y, w, h, level;

    Chest(int x, int y, int level) {
        this.x = x;
        this.y = y;
        this.w = 44;
        this.h = 30;
        this.level = level;
    }

    boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    void draw(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(new Color(255, 255, 255));
        g2d.drawString("Lv " + level, x, y - 6);
        g2d.setColor(new Color(200, 140, 70));
        g2d.fillRect(x, y, w, 12);
        g2d.setColor(new Color(150, 100, 50));
        g2d.fillRect(x, y + 12, w, h - 12);
        g2d.setColor(new Color(255, 215, 0));
        g2d.fillRect(x + w / 2 - 4, y + 10, 8, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, w, h);
    }
}
