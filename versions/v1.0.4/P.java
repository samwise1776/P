import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class P {
    // Game variables
    private static int circleX = 400;
    private static int circleY = 300;
    private static int steps = 10;
    private static int money = 0;

    // Upgrade costs
    private static int stepCost = 10;
    private static int maxMoneyCost = 25;
    private static int multiplierCost = 25;

    // Multipliers / Caps
    private static int moneyMultiplier = 10; 
    private static int maxMoneyCap = 1000;
    
    // Help button state
    private static final int HELP_DISCOUNT = 10000;

    // Physics state
    private static double velY = 0;
    private static boolean onGround = false;
    private static int jumpsLeft = 50;

    // Boost areas
    private static final int MAX_AREA_COST = 10000;
    private static int areaCost = 2000;
    private static final int[] areaX = {120, 350, 580};
    private static final int areaWidth = 90;
    private static final boolean[] areaBought = new boolean[areaX.length];

    public static void main(String[] args) {
        JFrame frame = new JFrame("FriendRun");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel paint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background: sky gradient
                int h = getHeight();
                int w = getWidth();
                GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 250), 0, h, new Color(224, 244, 255));
                g2d.setPaint(sky);
                g2d.fillRect(0, 0, w, h);

                // Sun
                g2d.setColor(new Color(255, 220, 80));
                g2d.fillOval(w - 90, 25, 55, 55);
                g2d.setColor(new Color(255, 235, 140));
                g2d.fillOval(w - 80, 35, 35, 35);

                // Clouds
                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.fillOval(60, 40, 70, 30);
                g2d.fillOval(100, 30, 60, 40);
                g2d.fillOval(130, 45, 55, 25);
                g2d.fillOval(320, 70, 60, 28);
                g2d.fillOval(350, 58, 55, 36);

                // Grass at the bottom
                g2d.setColor(new Color(110, 200, 90));
                g2d.fillRect(0, h - 100, w, 100);
                g2d.setColor(new Color(90, 170, 70));
                g2d.fillRect(0, h - 12, w, 12);

                Color skinColor = new Color(245, 200, 170);
                Color hairColor = new Color(70, 50, 40);
                Color shirtColor = new Color(40, 100, 200);
                Color pantsColor = new Color(50, 50, 60);
                Color shoeColor = new Color(80, 80, 80);

                // Shoes
                g2d.setColor(shoeColor);
                g2d.fillOval(circleX + 5, circleY + 75, 16, 10);
                g2d.fillOval(circleX + 24, circleY + 75, 16, 10);

                // Legs
                g2d.setColor(pantsColor);
                g2d.fillRect(circleX + 8, circleY + 45, 12, 32);
                g2d.fillRect(circleX + 24, circleY + 45, 12, 32);

                // Shirt
                g2d.setColor(shirtColor);
                g2d.fillRect(circleX + 5, circleY + 15, 34, 32);

                // Arms
                g2d.fillRect(circleX - 2, circleY + 15, 6, 22);
                g2d.fillRect(circleX + 40, circleY + 15, 6, 22);
                g2d.setColor(skinColor);
                g2d.fillOval(circleX - 3, circleY + 36, 8, 8);
                g2d.fillOval(circleX + 39, circleY + 36, 8, 8);

                // Head
                g2d.fillRect(circleX + 17, circleY + 10, 10, 6);
                g2d.fillOval(circleX + 11, circleY - 12, 22, 24);

                // Hair
                g2d.setColor(hairColor);
                g2d.fillArc(circleX + 10, circleY - 15, 24, 18, 0, 180);
                g2d.fillRect(circleX + 10, circleY - 8, 3, 8);
                g2d.fillRect(circleX + 31, circleY - 8, 3, 8);

                // Face
                g2d.setColor(Color.BLACK);
                g2d.fillOval(circleX + 16, circleY - 4, 3, 3);
                g2d.fillOval(circleX + 25, circleY - 4, 3, 3);
                g2d.drawArc(circleX + 18, circleY + 1, 8, 5, 0, -180);

                // UI
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Money: $" + money + " / $" + maxMoneyCap, 20, 30);
                g2d.drawString("Steps: " + steps, 20, 55);
                g2d.drawString("Money/Step: $" + moneyMultiplier, 20, 80);
                g2d.drawString("SPACE/W to jump (+2 steps)", 20, 105);
                g2d.drawString("Jumps Left: " + jumpsLeft, 20, 130);

                if (steps <= 0) {
                    g2d.setColor(Color.RED);
                    g2d.drawString("OUT OF STEPS! Buy more below.", 20, 155);
                }
            }
        };
        paint.setBackground(Color.WHITE);

        JPanel shopPanel = new JPanel(new FlowLayout());

        JButton buyStepsBtn = new JButton("Buy 10 Steps ($" + stepCost + ")");
        JButton buyMaxMoneyBtn = new JButton("Upgrade Max Money ($" + maxMoneyCost + ")");
        JButton buyMultiplierBtn = new JButton("Buy +10 Money/Step ($" + multiplierCost + ")");
        JButton helpBtn = new JButton("Help (-$" + HELP_DISCOUNT + " all prices)");
        JButton buyAreaBtn = new JButton("Buy Boost Area ($" + areaCost + ")");

        buyStepsBtn.addActionListener(e -> {
            if (money >= stepCost) {
                money -= stepCost;
                steps += 10;
                stepCost += 5;
                buyStepsBtn.setText("Buy 10 Steps ($" + stepCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        buyMaxMoneyBtn.addActionListener(e -> {
            if (money >= maxMoneyCost) {
                money -= maxMoneyCost;
                maxMoneyCap += 1000;
                maxMoneyCost *= 2;
                buyMaxMoneyBtn.setText("Upgrade Max Money ($" + maxMoneyCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        buyMultiplierBtn.addActionListener(e -> {
            if (money >= multiplierCost) {
                money -= multiplierCost;
                moneyMultiplier += 10;
                multiplierCost *= 2;
                areaCost = Math.min(MAX_AREA_COST, areaCost + 1000);
                buyMultiplierBtn.setText("Buy +10 Money/Step ($" + multiplierCost + ")");
                buyAreaBtn.setText("Buy Boost Area ($" + areaCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        buyAreaBtn.addActionListener(e -> {
            int idx = -1;
            for (int i = 0; i < areaBought.length; i++) {
                if (!areaBought[i]) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) {
                JOptionPane.showMessageDialog(frame, "All areas owned!");
                return;
            }
            if (money >= areaCost) {
                money -= areaCost;
                areaBought[idx] = true;
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        helpBtn.addActionListener(e -> {
            if (money > 100000) {
                stepCost -= HELP_DISCOUNT;
                maxMoneyCost -= HELP_DISCOUNT;
                multiplierCost -= HELP_DISCOUNT;
                buyStepsBtn.setText("Buy 10 Steps ($" + stepCost + ")");
                buyMaxMoneyBtn.setText("Upgrade Max Money ($" + maxMoneyCost + ")");
                buyMultiplierBtn.setText("Buy +10 Money/Step ($" + multiplierCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Need more than $100000 to use help!");
            }
        });

        shopPanel.add(buyStepsBtn);
        shopPanel.add(buyMaxMoneyBtn);
        shopPanel.add(buyMultiplierBtn);
        shopPanel.add(buyAreaBtn);
        shopPanel.add(helpBtn);

        InputMap im = paint.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = paint.getActionMap();

        im.put(KeyStroke.getKeyStroke('W'), "jump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "jump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "jump");
        im.put(KeyStroke.getKeyStroke("SPACE"), "jump");
        im.put(KeyStroke.getKeyStroke('A'), "left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "left");
        im.put(KeyStroke.getKeyStroke('D'), "right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "right");

        am.put("jump", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (onGround && jumpsLeft > 0) {
                    velY = -13;
                    onGround = false;
                    jumpsLeft--;
                    steps += 2;
                }
            }
        });

        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(-15, paint);
            }
        });

        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(15, paint);
            }
        });
        frame.add(paint, BorderLayout.CENTER);
        frame.add(shopPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        int groundY = paint.getHeight() - 185;
        circleY = groundY;
        onGround = true;

        javax.swing.Timer gameTimer = new javax.swing.Timer(16, e -> {
            velY += 0.8;
            circleY += (int) velY;
            int gY = paint.getHeight() - 185;
            if (circleY >= gY) {
                circleY = gY;
                velY = 0;
                onGround = true;
            }
            circleX = Math.max(0, Math.min(circleX, paint.getWidth() - 50));
            paint.repaint();
        });
        gameTimer.start();
    }

    private static void move(int dx, JPanel panel) {
        if (steps <= 0) return;

        circleX = Math.max(0, Math.min(circleX + dx, panel.getWidth() - 50));

        steps--;

        if (money < maxMoneyCap) {
            int gain = moneyMultiplier * (inBoostArea() ? 2 : 1);
            money += gain;
            if (money > maxMoneyCap) {
                money = maxMoneyCap;
            }
        }

        panel.repaint();
    }

    private static boolean inBoostArea() {
        for (int i = 0; i < areaX.length; i++) {
            if (areaBought[i] && circleX + 45 > areaX[i] && circleX < areaX[i] + areaWidth) {
                return true;
            }
        }
        return false;
    }
}