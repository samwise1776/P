import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

class Weapon {
    String name;
    int damage;
    int range;
    boolean ranged;
    long cooldownMs;
    int price;
    int projSpeed;
    String desc;
    int bonus = 0;
    int style = 0;

    Weapon(String name, int damage, int range, boolean ranged, long cooldownMs, int price, int projSpeed, String desc) {
        this.name = name;
        this.damage = damage;
        this.range = range;
        this.ranged = ranged;
        this.cooldownMs = cooldownMs;
        this.price = price;
        this.projSpeed = projSpeed;
        this.desc = desc;
    }

    int damageValue() {
        return damage + bonus;
    }

    Weapon copy() {
        Weapon nw = new Weapon(name, damage, range, ranged, cooldownMs, price, projSpeed, desc);
        nw.bonus = bonus;
        nw.style = style;
        return nw;
    }
}
