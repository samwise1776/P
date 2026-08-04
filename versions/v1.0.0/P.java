import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class P {
    // Game variables
    private static int circleX = 400;
    private static int circleY = 300;
    private static int steps = 10;
    private static int money = 0;
    
    // Upgrade costs
    private static int stepCost = 10;
    private static int moneyCost = 25;
    
    // Multipliers / Caps
    private static int moneyMultiplier = 10; 
    private static int maxMoneyCap = 1000;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Freindrun");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        
        // Use BorderLayout to easily split game view and shop panel
        frame.setLayout(new BorderLayout());

        // Main game screen panel
        JPanel paint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                // Turn on anti-aliasing to keep the round shapes smooth
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Base colors for the character
                Color skinColor = new Color(245, 200, 170);
                Color hairColor = new Color(70, 50, 40);
                Color shirtColor = new Color(40, 100, 200);
                Color pantsColor = new Color(50, 50, 60);
                Color shoeColor = new Color(80, 80, 80);

                // 1. FEET & SHOES
                g2d.setColor(shoeColor);
                g2d.fillOval(circleX + 5, circleY + 75, 16, 10);  // Left shoe
                g2d.fillOval(circleX + 24, circleY + 75, 16, 10); // Right shoe

                // 2. LEGS & PANTS
                g2d.setColor(pantsColor);
                g2d.fillRect(circleX + 8, circleY + 45, 12, 32);  // Left leg
                g2d.fillRect(circleX + 24, circleY + 45, 12, 32); // Right leg

                // 3. TORSO (Shirt)
                g2d.setColor(shirtColor);
                g2d.fillRect(circleX + 5, circleY + 15, 34, 32);  // Body

                // 4. ARMS
                g2d.setColor(shirtColor);
                g2d.fillRect(circleX - 2, circleY + 15, 6, 22);   // Left sleeve
                g2d.fillRect(circleX + 40, circleY + 15, 6, 22);  // Right sleeve
                g2d.setColor(skinColor);
                g2d.fillOval(circleX - 3, circleY + 36, 8, 8);    // Left hand
                g2d.fillOval(circleX + 39, circleY + 36, 8, 8);   // Right hand

                // 5. NECK & HEAD
                g2d.setColor(skinColor);
                g2d.fillRect(circleX + 17, circleY + 10, 10, 6);  // Neck
                g2d.fillOval(circleX + 11, circleY - 12, 22, 24); // Head

                // 6. HAIR
                g2d.setColor(hairColor);
                g2d.fillArc(circleX + 10, circleY - 15, 24, 18, 0, 180); // Top hair
                g2d.fillRect(circleX + 10, circleY - 8, 3, 8);    // Left sideburn
                g2d.fillRect(circleX + 31, circleY - 8, 3, 8);    // Right sideburn

                // 7. FACE DETAILS (Eyes & Smile)
                g2d.setColor(new Color(50, 50, 50));
                g2d.fillOval(circleX + 16, circleY - 4, 3, 3);    // Left eye
                g2d.fillOval(circleX + 25, circleY - 4, 3, 3);    // Right eye
                g2d.drawArc(circleX + 18, circleY + 1, 8, 5, 0, -180); // Smile
                
                // UI Overlay text
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Money: $" + money + " / $" + maxMoneyCap, 30, 40);
                g2d.drawString("Steps Left: " + steps, 30, 70);

                if (steps <= 0) {
                    g2d.setColor(Color.RED);
                    g2d.drawString("OUT OF STEPS! Use the shop below.", 30, 100);
                }
            }
        };
        paint.setBackground(Color.WHITE);

        // Shop UI Panel
        JPanel shopPanel = new JPanel();
        shopPanel.setBackground(Color.LIGHT_GRAY);
        shopPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton buyStepsBtn = new JButton("Buy 10 Steps (Cost: $" + stepCost + ")");
        JButton buyMaxMoneyBtn = new JButton("Upgrade Max Money (Cost: $" + moneyCost + ")");

        // Shop Action: Buy Steps
        buyStepsBtn.addActionListener(e -> {
            if (money >= stepCost) {
                money -= stepCost;
                steps += 10;
                stepCost += 5; 
                buyStepsBtn.setText("Buy 10 Steps (Cost: $" + stepCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
            frame.requestFocusInWindow(); 
        });

        // Shop Action: Upgrade Max Money limit
        buyMaxMoneyBtn.addActionListener(e -> {
            if (money >= moneyCost) {
                money -= moneyCost;
                maxMoneyCap += 1000;
                moneyMultiplier += 5; 
                moneyCost *= 2; 
                buyMaxMoneyBtn.setText("Upgrade Max Money (Cost: $" + moneyCost + ")");
                paint.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
            frame.requestFocusInWindow(); 
        });

        shopPanel.add(buyStepsBtn);
        shopPanel.add(buyMaxMoneyBtn);

        // Movement Actions (W / S / A / D) checking step availability
        addKey(KeyEvent.VK_W, frame, () -> moveCircle(0, -15, paint));
        addKey(KeyEvent.VK_S, frame, () -> moveCircle(0, 15, paint));
        addKey(KeyEvent.VK_A, frame, () -> moveCircle(-15, 0, paint));
        addKey(KeyEvent.VK_D, frame, () -> moveCircle(15, 0, paint));

        // Assemble frame components
        frame.add(paint, BorderLayout.CENTER);
        frame.add(shopPanel, BorderLayout.SOUTH);
        
        frame.setVisible(true);
        frame.requestFocusInWindow();
    }

    // Helper logic to execute movement
    private static void moveCircle(int dx, int dy, JPanel panel) {
        if (steps > 0) {
            circleX += dx;
            circleY += dy;
            steps--;
            
            if (money < maxMoneyCap) {
                money += moneyMultiplier;
                if (money > maxMoneyCap) {
                    money = maxMoneyCap;
                }
            }
            panel.repaint();
        }
    }

    // Key binding helper utility
    public static void addKey(int keyCode, JFrame frame, Runnable action) {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == keyCode) {
                    action.run();
                }
            }
        });
    }
}