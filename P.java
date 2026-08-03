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
    private static int helpUsesLeft = 10;
    private static int helpActiveTurns = 0;

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
                g2d.drawString("Help Uses Left: " + helpUsesLeft, 20, 105);

                if (helpActiveTurns > 0) {
                    g2d.setColor(new Color(0, 150, 0));
                    g2d.drawString("FREE SHOP! " + helpActiveTurns + " turns left", 20, 130);
                }

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
        JButton helpBtn = new JButton("Help - Free Shop 5 Turns (Left: " + helpUsesLeft + ")");

        buyStepsBtn.addActionListener(e -> {
            int cost = helpActiveTurns > 0 ? 0 : stepCost;
            if (money >= cost) {
                money -= cost;
                steps += 10;
                stepCost += 5;
                buyStepsBtn.setText("Buy 10 Steps ($" + stepCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        buyMaxMoneyBtn.addActionListener(e -> {
            int cost = helpActiveTurns > 0 ? 0 : maxMoneyCost;
            if (money >= cost) {
                money -= cost;
                maxMoneyCap += 1000;
                maxMoneyCost *= 2;
                buyMaxMoneyBtn.setText("Upgrade Max Money ($" + maxMoneyCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        buyMultiplierBtn.addActionListener(e -> {
            int cost = helpActiveTurns > 0 ? 0 : multiplierCost;
            if (money >= cost) {
                money -= cost;
                moneyMultiplier += 10;
                multiplierCost *= 2;
                buyMultiplierBtn.setText("Buy +10 Money/Step ($" + multiplierCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });

        helpBtn.addActionListener(e -> {
            if (helpUsesLeft > 0) {
                helpUsesLeft--;
                helpActiveTurns = 5;
                helpBtn.setText("Help - Free Shop 5 Turns (Left: " + helpUsesLeft + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "No more help left!");
            }
        });

        shopPanel.add(buyStepsBtn);
        shopPanel.add(buyMaxMoneyBtn);
        shopPanel.add(buyMultiplierBtn);
        shopPanel.add(helpBtn);

        InputMap im = paint.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = paint.getActionMap();

        im.put(KeyStroke.getKeyStroke('W'), "up");
        im.put(KeyStroke.getKeyStroke('S'), "down");
        im.put(KeyStroke.getKeyStroke('A'), "left");
        im.put(KeyStroke.getKeyStroke('D'), "right");

        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(0, -15, paint);
            }
        });

        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(0, 15, paint);
            }
        });

        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(-15, 0, paint);
            }
        });

        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(15, 0, paint);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "right");
        frame.add(paint, BorderLayout.CENTER);
        frame.add(shopPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void move(int dx, int dy, JPanel panel) {
        if (steps <= 0) return;

        circleX = Math.max(0, Math.min(circleX + dx, panel.getWidth() - 50));
        circleY = Math.max(0, Math.min(circleY + dy, panel.getHeight() - 90));

        steps--;

        if (helpActiveTurns > 0) {
            helpActiveTurns--;
        }

        if (money < maxMoneyCap) {
            money += moneyMultiplier;
            if (money > maxMoneyCap) {
                money = maxMoneyCap;
            }
        }

        panel.repaint();
    }
}