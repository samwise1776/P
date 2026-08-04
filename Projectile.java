import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

class Projectile {
    int x, y;
    double vx, vy;
    int damage;
    boolean fromEnemy;
    Color color;
    boolean dead = false;

    Projectile(int x, int y, double vx, double vy, int damage, boolean fromEnemy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.fromEnemy = fromEnemy;
        this.color = color;
    }

    void update() {
        x += (int) vx;
        y += (int) vy;
        if (x < -80 || x > 1400 || y < -80 || y > 900) {
            dead = true;
            return;
        }
        if (fromEnemy) {
            if (Math.abs(x - (P.circleX + 22)) < 26 && Math.abs(y - (P.circleY + 30)) < 42) {
                P.damagePlayer(damage);
                dead = true;
            }
        } else {
            for (Enemy e : P.enemies) {
                if (e.dead) continue;
                int cx = e.x + e.width / 2, cy = e.y - e.height / 2;
                if (Math.abs(x - cx) < e.width / 2 + 8 && Math.abs(y - cy) < e.height / 2 + 8) {
                    e.hurt(damage);
                    dead = true;
                    break;
                }
            }
        }
    }

    void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        double len = Math.hypot(vx, vy);
        if (len > 0) {
            int tipX = x + (int) (vx / len * 14), tipY = y + (int) (vy / len * 14);
            g2d.drawLine(x, y, tipX, tipY);
            double px = -vy / len, py = vx / len;
            g2d.drawLine(tipX, tipY, tipX - (int) (px * 5) - (int) (vx / len * 5), tipY - (int) (py * 5) - (int) (vy / len * 5));
            g2d.drawLine(tipX, tipY, tipX + (int) (px * 5) - (int) (vx / len * 5), tipY + (int) (py * 5) - (int) (vy / len * 5));
        } else {
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
        g2d.setStroke(new BasicStroke(1));
    }
}
