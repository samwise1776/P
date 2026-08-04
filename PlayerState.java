import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

class PlayerState {
    String name = "";
    int x = 200, y = 0;
    int steps = 30, money = 0, jumpsLeft = 50;
    int moneyMultiplier = 10, maxMoneyCap = 10000;
    int stepCost = 10, maxMoneyCost = 25, multiplierCost = 25;
    double velY = 0;
    boolean onGround = false;
    boolean[] areaOwned = new boolean[P.areaX.length];

    void reset() {
        boolean vip = (this == P.local) && P.isVip();
        x = 200;
        y = 0;
        steps = vip ? Integer.MAX_VALUE : 30;
        money = vip ? Integer.MAX_VALUE : 30;
        jumpsLeft = 50;
        moneyMultiplier = vip ? 500 : 10;
        maxMoneyCap = vip ? Integer.MAX_VALUE : 10000;
        stepCost = 10;
        maxMoneyCost = 25;
        multiplierCost = 25;
        velY = 0;
        onGround = false;
        areaOwned = new boolean[P.areaX.length];
    }
}
