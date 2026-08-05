import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

abstract class Enemy {
    int x, y, health, maxHealth, damage, speed, width, height, range;
    boolean ranged;
    double velY = 0;
    boolean onGround = false, dead = false;
    long lastAttackMs = 0;
    Color color;

    Enemy(int health, int damage, int speed, int range, boolean ranged, Color color, int w, int h) {
        this.maxHealth = health;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.range = range;
        this.ranged = ranged;
        this.color = color;
        this.width = w;
        this.height = h;
    }

    int faceDir() {
        return P.circleX - x >= 0 ? 1 : -1;
    }

    void gravity(int panelHeight) {
        velY += P.GRAVITY;
        y += (int) velY;
        int gY = panelHeight - 185;
        if (y >= gY) {
            y = gY;
            velY = 0;
            onGround = true;
        }
    }

    void hurt(int dmg) {
        if (dead) return;
        health -= dmg;
        if (health <= 0) {
            health = 0;
            dead = true;
            onKilled();
        }
    }

    void onKilled() {
        if (maxHealth >= 500) {
            P.money = (int) Math.min((long) P.maxMoneyCap, (long) P.money + 500);
            P.playerPoints += 25;
            P.setPoints(P.playerName, P.playerPoints);
            P.saveData();
            P.startPointsLabel.setText("Points: " + P.playerPoints);
            P.bossAlive = false;
            JOptionPane.showMessageDialog(P.paint, "BOSS DEFEATED!\n+$500 and +25 points!");
        } else {
            P.money = (int) Math.min((long) P.maxMoneyCap, (long) P.money + 50);
            P.kills++;
            P.playerPoints++;
            P.setPoints(P.playerName, P.playerPoints);
            P.saveData();
            P.startPointsLabel.setText("Points: " + P.playerPoints);
        }
        P.totalKills++;
        P.levelKills++;
        P.playerHealth = Math.min(P.playerMaxHealth, P.playerHealth + 100);
        if (P.levelKills >= P.requiredKills(P.playerLevel)) {
            P.playerLevel++;
            P.levelKills = 0;
            P.playerMaxHealth = (int) (P.PLAYER_MAX_HEALTH * Math.pow(1.75, P.playerLevel - 1));
            P.playerHealth = P.playerMaxHealth;
            JOptionPane.showMessageDialog(P.paint, "LEVEL UP! Level " + P.playerLevel
                    + "\nMax HP " + P.playerMaxHealth + "\nAttack x" + String.format("%.2f", P.attackMultiplier()));
        }
        if (P.totalKills % P.CHEST_EVERY == 0 && P.chests.size() < 3) {
            int chestLevel = P.totalKills / 5;
            int cx = 60 + (int) (Math.random() * Math.max(1, P.paint.getWidth() - 160));
            int cy = P.paint.getHeight() - 185 - 30;
            P.chests.add(new Chest(cx, cy, chestLevel));
        }
    }

    void draw(Graphics2D g2d) {
        // legs
        g2d.setColor(new Color(60, 40, 30));
        g2d.fillRect(x + width / 2 - 8, y - 14, 7, 14);
        g2d.fillRect(x + width / 2 + 1, y - 14, 7, 14);
        // body
        g2d.setColor(color);
        g2d.fillOval(x, y - height, width, height);
        // head
        g2d.setColor(color.darker());
        g2d.fillOval(x + width / 4, y - height - 16, width / 2, 16);
        // eyes
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + width / 2 - 10, y - height - 12, 7, 7);
        g2d.fillOval(x + width / 2 + 3, y - height - 12, 7, 7);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + width / 2 - 8, y - height - 10, 3, 3);
        g2d.fillOval(x + width / 2 + 5, y - height - 10, 3, 3);
        // bow for archers
        if (ranged) {
            g2d.setColor(new Color(150, 105, 60));
            int dir = faceDir();
            int cx = x + width / 2;
            g2d.drawArc(cx + (dir > 0 ? 8 : -26), y - height - 4, 18, 24, dir > 0 ? -90 : 90, 180);
            g2d.drawLine(cx + (dir > 0 ? 17 : -9), y - height, cx + (dir > 0 ? 17 : -9), y - height + 24);
        }
        drawHealthBar(g2d);
    }

    void drawHealthBar(Graphics2D g2d) {
        int barW = Math.max(34, width + 6);
        int barH = 6;
        int bx = x - 3, by = y - height - 20;
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(bx, by, barW, barH);
        g2d.setColor(health > maxHealth / 3 ? new Color(70, 210, 70) : new Color(220, 70, 60));
        g2d.fillRect(bx, by, (int) (barW * Math.max(0, health) / (double) maxHealth), barH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(bx, by, barW, barH);
    }

    abstract void act(int panelWidth, int panelHeight);
}


class MeleeEnemy extends Enemy {
    MeleeEnemy(int x) {
        super(100, 10, 2, 55, false, new Color(210, 70, 60), 40, 46);
        this.x = x;
    }

    void act(int panelWidth, int panelHeight) {
        gravity(panelHeight);
        int dx = P.circleX - this.x;
        if (Math.abs(dx) > range) {
            x = P.clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
        }
        long now = System.currentTimeMillis();
        if (Math.abs(dx) <= range + 34 && Math.abs(P.circleY - (y - height)) < 90) {
            if (now - lastAttackMs > 800) {
                lastAttackMs = now;
                P.damagePlayer(damage);
            }
        }
    }
}


class RangedEnemy extends Enemy {
    RangedEnemy(int x) {
        super(100, 25, 1, 320, true, new Color(150, 90, 210), 36, 44);
        this.x = x;
    }

    void act(int panelWidth, int panelHeight) {
        gravity(panelHeight);
        int dx = P.circleX - this.x;
        if (Math.abs(dx) > range) {
            x = P.clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
        } else if (Math.abs(dx) < 150) {
            x = P.clamp(x - (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
        }
        long now = System.currentTimeMillis();
        if (now - lastAttackMs > 1200) {
            lastAttackMs = now;
            double sx = x + width / 2.0, sy = y - height / 2.0;
            double tx = P.circleX + 22, ty = P.circleY + 30;
            double nx = tx - sx, ny = ty - sy;
            double len = Math.hypot(nx, ny);
            if (len > 0) {
                double sp = 9;
                P.projectiles.add(new Projectile((int) sx, (int) sy, nx / len * sp, ny / len * sp, 25, true, new Color(140, 90, 50)));
            }
        }
    }
}


class Boss extends Enemy {
    Boss(int x) {
        super(500, 200, 1, 100, false, new Color(110, 20, 160), 92, 110);
        this.x = x;
    }

    void act(int panelWidth, int panelHeight) {
        gravity(panelHeight);
        int dx = P.circleX - this.x;
        if (Math.abs(dx) > range) {
            x = P.clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
        }
        long now = System.currentTimeMillis();
        if (now - lastAttackMs > 1500) {
            lastAttackMs = now;
            if (Math.abs(dx) > 220) {
                double sx = x + width / 2.0, sy = y - height / 2.0;
                double tx = P.circleX + 22, ty = P.circleY + 30;
                double nx = tx - sx, ny = ty - sy;
                double len = Math.hypot(nx, ny);
                if (len > 0) {
                    double sp = 7;
                    P.projectiles.add(new Projectile((int) sx, (int) sy, nx / len * sp, ny / len * sp, 200, true, new Color(200, 60, 220)));
                }
            } else if (Math.abs(P.circleY - (y - height)) < 120) {
                P.damagePlayer(200);
            }
        }
    }
}
