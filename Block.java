import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

class Block {
    int x, y, w, h;

    Block(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    boolean at(int gx, int gy) {
        return x == gx && y == gy;
    }
}
