import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class P {
    // ---- profile & persistence ----
    static String playerName = "Player";
    static int playerPoints = 0;
    static ArrayList<String> friends = new ArrayList<>();
    static ArrayList<String[]> leaderboard = new ArrayList<>();

    // ---- navigation ----
    static CardLayout layoutCards;
    static JPanel rootCards;
    static JLabel startPointsLabel;
    static JTextArea leaderboardArea;

    // ---- single player state ----
    static int circleX = 400;
    static int circleY = 300;
    static int steps = 10;
    static int money = 0;
    static int stepCost = 10;
    static int maxMoneyCost = 25;
    static int multiplierCost = 25;
    static int moneyMultiplier = 10;
    static int maxMoneyCap = 1000;
    static double velY = 0;
    static boolean onGround = false;
    static int jumpsLeft = 50;
    static boolean singlePlaced = false;
    static JPanel paint;
    static javax.swing.Timer singleTimer;

    // ---- shared constants ----
    static final int HELP_DISCOUNT = 10000;
    static final int AREA_COST = 2000;
    static final int[] areaX = {120, 350, 580};
    static final int areaWidth = 90;
    static boolean[] areaBought = new boolean[areaX.length];

    // ---- multiplayer state ----
    static PlayerState local = new PlayerState();
    static PlayerState rival = new PlayerState();
    static volatile PlayerState remote = new PlayerState();
    static int timeLimit, timeLeft;
    static long matchStart;
    static int botCounter;
    static boolean matchOver, playersPlaced;
    static String opponentName = "Rival";
    static int opponentPoints = 0;
    static javax.swing.Timer matchTimer;
    static JPanel multiPaint;

    // ---- public multiplayer (net) state ----
    static Socket netSocket;
    static BufferedReader netIn;
    static PrintWriter netOut;
    static volatile boolean netRunning;
    static volatile boolean netMatchActive;
    static volatile boolean netRemoteSeen;
    static volatile boolean netPlaced;
    static volatile String remoteName = "Rival";
    static volatile int remotePoints = 0;
    static volatile int netTimeLeft = 60;
    static int netSendCounter = 0;
    static javax.swing.Timer netTimer;
    static JPanel netPaint;

    public static void main(String[] args) {
        loadData();

        JFrame frame = new JFrame("FriendRun");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        layoutCards = new CardLayout();
        rootCards = new JPanel(layoutCards);

        buildStartScreen(frame);
        buildSinglePlayer(frame);
        buildMultiplayer(frame);
        buildNetPanel(frame);
        buildLeaderboard(frame);

        frame.add(rootCards);
        frame.setVisible(true);
        layoutCards.show(rootCards, "start");
    }

    // ================= START SCREEN =================
    static void buildStartScreen(JFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(135, 206, 250));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("FRIENDRUN");
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel("Your name:");
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextField nameField = new JTextField(playerName, 15);
        nameField.setMaximumSize(new Dimension(250, 30));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton saveName = new JButton("Save Name");
        saveName.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveName.addActionListener(e -> {
            String n = nameField.getText().trim();
            if (!n.isEmpty()) playerName = n;
            saveData();
            startPointsLabel.setText("Points: " + playerPoints);
        });

        startPointsLabel = new JLabel("Points: " + playerPoints);
        startPointsLabel.setFont(new Font("Arial", Font.BOLD, 20));
        startPointsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = new JButton("Start");
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> {
            resetSinglePlayer();
            layoutCards.show(rootCards, "game");
            paint.revalidate();
            singlePlaced = false;
            singleTimer.start();
        });

        JButton mpBtn = new JButton("Start Multiplayer");
        mpBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        mpBtn.addActionListener(e -> {
            pickRival();
            startMultiplayer(opponentName, opponentPoints);
        });

        JButton pubBtn = new JButton("Public Multiplayer");
        pubBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pubBtn.addActionListener(e -> {
            String host = JOptionPane.showInputDialog(frame, "Server address:", "localhost");
            if (host == null) return;
            String portStr = JOptionPane.showInputDialog(frame, "Server port:", "4444");
            if (portStr == null) return;
            int port = 4444;
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (Exception ignored) {
            }
            connectPublic(host.trim(), port);
        });

        JButton lbBtn = new JButton("Leaderboard");
        lbBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbBtn.addActionListener(e -> {
            refreshLeaderboard();
            layoutCards.show(rootCards, "leaderboard");
        });

        JPanel friendPanel = new JPanel(new FlowLayout());
        friendPanel.setOpaque(false);
        JTextField friendField = new JTextField(10);
        JButton addFriendBtn = new JButton("Add Friend");
        addFriendBtn.addActionListener(e -> {
            String f = friendField.getText().trim();
            if (!f.isEmpty() && !friends.contains(f)) {
                friends.add(f);
                saveData();
                JOptionPane.showMessageDialog(frame, "Added " + f + " to friends!");
            }
        });
        JButton fightFriendBtn = new JButton("Fight Friend");
        fightFriendBtn.addActionListener(e -> {
            if (friends.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Add a friend first!");
                return;
            }
            String[] opts = friends.toArray(new String[0]);
            String choice = (String) JOptionPane.showInputDialog(frame, "Choose a friend to fight:", "Fight Friend",
                    JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
            if (choice != null) {
                startMultiplayer(choice, getPointsOf(choice));
            }
        });
        friendPanel.add(new JLabel("Friends:"));
        friendPanel.add(friendField);
        friendPanel.add(addFriendBtn);
        friendPanel.add(fightFriendBtn);

        center.add(Box.createVerticalStrut(30));
        center.add(title);
        center.add(Box.createVerticalStrut(20));
        center.add(nameLbl);
        center.add(nameField);
        center.add(saveName);
        center.add(Box.createVerticalStrut(10));
        center.add(startPointsLabel);
        center.add(Box.createVerticalStrut(20));
        center.add(startBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(mpBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(pubBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(lbBtn);
        center.add(Box.createVerticalStrut(20));
        center.add(friendPanel);

        p.add(center, BorderLayout.CENTER);
        rootCards.add(p, "start");
    }

    // ================= SINGLE PLAYER =================
    static void buildSinglePlayer(JFrame frame) {
        paint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight();
                int w = getWidth();
                drawBackground(g2d, w, h);

                // Boost area boxes with cost
                int groundTop = h - 100;
                for (int i = 0; i < areaX.length; i++) {
                    if (areaBought[i]) {
                        g2d.setColor(new Color(255, 215, 0, 170));
                        g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                        g2d.setColor(new Color(180, 140, 0));
                        g2d.drawString("x2", areaX[i] + 35, groundTop - 30);
                    } else {
                        g2d.setColor(new Color(255, 255, 255, 190));
                        g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                        g2d.setColor(Color.BLACK);
                        g2d.drawRect(areaX[i], groundTop - 70, areaWidth, 70);
                        g2d.drawString("$" + AREA_COST, areaX[i] + 22, groundTop - 30);
                    }
                }

                drawCharacter(g2d, circleX, circleY, new Color(40, 100, 200));

                // UI
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.setColor(Color.BLACK);
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

        JButton[] areaBtns = new JButton[areaX.length];
        for (int i = 0; i < areaX.length; i++) {
            final int idx = i;
            areaBtns[i] = new JButton("Buy Area " + (idx + 1) + " ($" + AREA_COST + ")");
            areaBtns[i].addActionListener(e -> {
                if (areaBought[idx]) {
                    return;
                }
                if (!inArea(idx)) {
                    JOptionPane.showMessageDialog(frame, "Stand inside area " + (idx + 1) + " to buy it!");
                    return;
                }
                if (money >= AREA_COST) {
                    money -= AREA_COST;
                    areaBought[idx] = true;
                    areaBtns[idx].setText("Area " + (idx + 1) + " Owned");
                    paint.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, "Not enough money!");
                }
            });
        }

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
                buyMultiplierBtn.setText("Buy +10 Money/Step ($" + multiplierCost + ")");
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

        JButton menuBtn = new JButton("Menu");
        menuBtn.addActionListener(e -> {
            singleTimer.stop();
            layoutCards.show(rootCards, "start");
        });

        shopPanel.add(buyStepsBtn);
        shopPanel.add(buyMaxMoneyBtn);
        shopPanel.add(buyMultiplierBtn);
        for (int i = 0; i < areaBtns.length; i++) {
            shopPanel.add(areaBtns[i]);
        }
        shopPanel.add(helpBtn);
        shopPanel.add(menuBtn);

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

        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(paint, BorderLayout.CENTER);
        gamePanel.add(shopPanel, BorderLayout.SOUTH);
        rootCards.add(gamePanel, "game");

        singleTimer = new javax.swing.Timer(16, e -> {
            if (!singlePlaced) {
                int g = paint.getHeight() - 185;
                if (g > 0) {
                    circleY = g;
                    onGround = true;
                    singlePlaced = true;
                }
            }
            velY += 0.8;
            circleY += (int) velY;
            int gY = paint.getHeight() - 185;
            if (circleY >= gY) {
                circleY = gY;
                velY = 0;
                onGround = true;
            }
            circleX = clamp(circleX, 0, paint.getWidth() - 50);
            paint.repaint();
        });
    }

    static void resetSinglePlayer() {
        circleX = 400;
        circleY = 300;
        steps = 10;
        money = 0;
        stepCost = 10;
        maxMoneyCost = 25;
        multiplierCost = 25;
        moneyMultiplier = 10;
        maxMoneyCap = 1000;
        velY = 0;
        onGround = false;
        jumpsLeft = 50;
        areaBought = new boolean[areaX.length];
    }

    private static void move(int dx, JPanel panel) {
        if (steps <= 0) return;

        circleX = clamp(circleX + dx, 0, panel.getWidth() - 50);

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

    // ================= MULTIPLAYER =================
    static void buildMultiplayer(JFrame frame) {
        multiPaint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight();
                int w = getWidth();
                drawBackground(g2d, w, h);
                drawZones(g2d, h, local, rival);

                drawCharacter(g2d, local.x, local.y, new Color(40, 100, 200));
                drawCharacter(g2d, rival.x, rival.y, new Color(200, 60, 60));

                // UI
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.setColor(Color.BLACK);
                g2d.drawString("Time left: " + Math.max(0, timeLeft) + "s", 20, 30);
                g2d.drawString(playerName + " (You): $" + local.money + " | Steps: " + local.steps + " | +$" + local.moneyMultiplier + "/step", 20, 55);
                g2d.drawString(opponentName + ": $" + rival.money, 20, 80);
                g2d.drawString("Move with A/D + jump with SPACE/W. Use the shop below!", 20, 105);
            }
        };
        multiPaint.setBackground(Color.WHITE);

        JButton endBtn = new JButton("End Match");
        endBtn.addActionListener(e -> {
            if (matchOver) return;
            matchOver = true;
            matchTimer.stop();
            layoutCards.show(rootCards, "start");
        });

        JPanel matchPanel = new JPanel(new BorderLayout());
        matchPanel.add(multiPaint, BorderLayout.CENTER);
        matchPanel.add(buildPlayerShop(frame, local, multiPaint, "Your Shop", endBtn), BorderLayout.SOUTH);
        rootCards.add(matchPanel, "multi");

        InputMap im = multiPaint.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = multiPaint.getActionMap();

        im.put(KeyStroke.getKeyStroke('W'), "p1jump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "p1jump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "p1jump");
        im.put(KeyStroke.getKeyStroke("SPACE"), "p1jump");
        im.put(KeyStroke.getKeyStroke('A'), "p1left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "p1left");
        im.put(KeyStroke.getKeyStroke('D'), "p1right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "p1right");

        am.put("p1jump", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (matchOver) return;
                jumpPlayer(local);
            }
        });
        am.put("p1left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (matchOver) return;
                movePlayer(local, -15, multiPaint.getWidth());
            }
        });
        am.put("p1right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (matchOver) return;
                movePlayer(local, 15, multiPaint.getWidth());
            }
        });

        matchTimer = new javax.swing.Timer(16, e -> {
            if (matchOver) return;

            long elapsed = (System.currentTimeMillis() - matchStart) / 1000;
            timeLeft = timeLimit - (int) elapsed;
            if (timeLeft <= 0) {
                endMatch();
                return;
            }

            if (!playersPlaced) {
                int g = multiPaint.getHeight() - 185;
                if (g > 0) {
                    local.y = g;
                    rival.y = g;
                    local.onGround = true;
                    rival.onGround = true;
                    playersPlaced = true;
                }
            }

            applyGravity(local, multiPaint.getHeight());
            applyGravity(rival, multiPaint.getHeight());
            local.x = clamp(local.x, 0, multiPaint.getWidth() - 50);
            rival.x = clamp(rival.x, 0, multiPaint.getWidth() - 50);

            // bot AI
            botCounter++;
            if (botCounter % 8 == 0) {
                int dir = (int) (Math.random() * 3) - 1;
                if (dir != 0 && rival.steps > 0) {
                    movePlayer(rival, dir * 15, multiPaint.getWidth());
                }
                if (Math.random() < 0.2 && rival.onGround && rival.jumpsLeft > 0) {
                    jumpPlayer(rival);
                }
            }
            botShop(rival);

            multiPaint.repaint();
        });
    }

    static void startMultiplayer(String oName, int oPoints) {
        String t = JOptionPane.showInputDialog(rootCards, "Enter time limit (seconds):", "60");
        if (t == null) return;
        int lim = 60;
        try {
            lim = Integer.parseInt(t.trim());
        } catch (Exception ignored) {
        }
        if (lim < 5) lim = 5;

        opponentName = oName;
        opponentPoints = oPoints;
        timeLimit = lim;

        local.reset();
        rival.reset();
        local.x = 200;
        rival.x = 500;
        rival.moneyMultiplier = 10 + opponentPoints / 5;
        rival.steps = 30 + opponentPoints / 10;
        timeLeft = timeLimit;
        matchOver = false;
        botCounter = 0;
        playersPlaced = false;

        layoutCards.show(rootCards, "multi");
        matchStart = System.currentTimeMillis();
        matchTimer.start();
    }

    static void pickRival() {
        ArrayList<String[]> sorted = new ArrayList<>(leaderboard);
        sorted.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1]));
        for (String[] e : sorted) {
            if (!e[0].equals(playerName) && Integer.parseInt(e[1]) >= playerPoints) {
                opponentName = e[0];
                opponentPoints = Integer.parseInt(e[1]);
                return;
            }
        }
        opponentPoints = playerPoints + 10;
        opponentName = "Rival " + (playerPoints / 10 + 1);
    }

    static void endMatch() {
        matchOver = true;
        matchTimer.stop();

        boolean win = local.money > rival.money;
        String msg;
        if (win) {
            int gain = 1 + Math.max(0, (opponentPoints - playerPoints) / 5);
            playerPoints += gain;
            setPoints(playerName, playerPoints);
            saveData();
            msg = "You win! $" + local.money + " vs $" + rival.money + "\nYou earned " + gain + " point(s)!";
            startPointsLabel.setText("Points: " + playerPoints);
        } else {
            msg = "You lose! $" + local.money + " vs $" + rival.money + "\n" + opponentName + " wins.";
        }
        JOptionPane.showMessageDialog(multiPaint, msg);
        layoutCards.show(rootCards, "start");
    }

    // ================= PUBLIC MULTIPLAYER (NET) =================
    static void buildNetPanel(JFrame frame) {
        netPaint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight();
                int w = getWidth();
                drawBackground(g2d, w, h);
                drawZones(g2d, h, local, remote);

                drawCharacter(g2d, local.x, local.y, new Color(40, 100, 200));
                drawCharacter(g2d, remote.x, remote.y, new Color(200, 60, 60));

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.setColor(Color.BLACK);
                g2d.drawString("Time left: " + Math.max(0, netTimeLeft) + "s", 20, 30);
                g2d.drawString(playerName + " (You): $" + local.money + " | Steps: " + local.steps + " | +$" + local.moneyMultiplier + "/step", 20, 55);
                g2d.drawString(remoteName + ": $" + remote.money, 20, 80);
                g2d.drawString("Move with A/D + jump with SPACE/W. Use the shop below!", 20, 105);
            }
        };
        netPaint.setBackground(Color.WHITE);

        JButton leaveBtn = new JButton("Leave Match");
        leaveBtn.addActionListener(e -> {
            if (!netMatchActive) return;
            netMatchActive = false;
            if (netTimer != null) netTimer.stop();
            closeNet();
            layoutCards.show(rootCards, "start");
        });

        JPanel netPanel = new JPanel(new BorderLayout());
        netPanel.add(netPaint, BorderLayout.CENTER);
        netPanel.add(buildPlayerShop(frame, local, netPaint, "Your Shop", leaveBtn), BorderLayout.SOUTH);
        rootCards.add(netPanel, "net");

        InputMap im = netPaint.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = netPaint.getActionMap();

        im.put(KeyStroke.getKeyStroke('W'), "netjump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "netjump");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "netjump");
        im.put(KeyStroke.getKeyStroke("SPACE"), "netjump");
        im.put(KeyStroke.getKeyStroke('A'), "netleft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "netleft");
        im.put(KeyStroke.getKeyStroke('D'), "netright");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "netright");

        am.put("netjump", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!netMatchActive) return;
                jumpPlayer(local);
            }
        });
        am.put("netleft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!netMatchActive) return;
                movePlayer(local, -15, netPaint.getWidth());
            }
        });
        am.put("netright", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!netMatchActive) return;
                movePlayer(local, 15, netPaint.getWidth());
            }
        });

        netTimer = new javax.swing.Timer(16, e -> {
            if (!netMatchActive) return;

            if (!netPlaced) {
                int g = netPaint.getHeight() - 185;
                if (g > 0) {
                    local.y = g;
                    local.onGround = true;
                    netPlaced = true;
                }
            }

            applyGravity(local, netPaint.getHeight());
            local.x = clamp(local.x, 0, netPaint.getWidth() - 50);

            if (!netRemoteSeen) {
                int g = netPaint.getHeight() - 185;
                if (g > 0) {
                    remote.y += 0.8;
                    if (remote.y >= g) remote.y = g;
                }
            }

            sendNetState();
            netPaint.repaint();
        });
    }

    static void connectPublic(String host, int port) {
        try {
            netSocket = new Socket(host, port);
            netOut = new PrintWriter(netSocket.getOutputStream(), true);
            netIn = new BufferedReader(new InputStreamReader(netSocket.getInputStream()));
            netOut.println("JOIN " + playerName.replaceAll("\\s+", "_") + " " + playerPoints);
            netOut.flush();
            netRunning = true;
            new Thread(P::netReader).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not connect to server: " + ex.getMessage());
        }
    }

    static void netReader() {
        try {
            String line;
            while (netRunning && (line = netIn.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> handleNetMessage(msg));
            }
        } catch (Exception ignored) {
        } finally {
            netRunning = false;
            if (netMatchActive) {
                netMatchActive = false;
                if (netTimer != null) netTimer.stop();
                JOptionPane.showMessageDialog(null, "Disconnected from server.");
                layoutCards.show(rootCards, "start");
            }
        }
    }

    static void handleNetMessage(String msg) {
        if (msg.startsWith("WAIT")) {
            JOptionPane.showMessageDialog(null, "Waiting for an opponent...");
            return;
        }
        if (msg.startsWith("FULL")) {
            JOptionPane.showMessageDialog(null, "Server is full.");
            return;
        }
        if (msg.startsWith("MATCH ")) {
            String[] p = msg.split("\\s+");
            remoteName = p[1];
            netTimeLeft = Integer.parseInt(p[2]);
            remotePoints = p.length > 3 ? Integer.parseInt(p[3]) : 0;

            local.reset();
            remote.reset();
            local.x = 200;
            remote.x = 500;
            netRemoteSeen = false;
            netPlaced = false;
            netSendCounter = 0;

            layoutCards.show(rootCards, "net");
            netMatchActive = true;
            netTimer.start();
            return;
        }
        if (msg.startsWith("O ")) {
            String[] p = msg.split("\\s+");
            if (p.length >= 9) {
                remote.x = Integer.parseInt(p[1]);
                remote.y = Integer.parseInt(p[2]);
                remote.steps = Integer.parseInt(p[3]);
                remote.money = Integer.parseInt(p[4]);
                remote.jumpsLeft = Integer.parseInt(p[5]);
                remote.moneyMultiplier = Integer.parseInt(p[6]);
                remote.maxMoneyCap = Integer.parseInt(p[7]);
                setAreaBits(remote, Integer.parseInt(p[8]));
                netRemoteSeen = true;
            }
            return;
        }
        if (msg.startsWith("TIME ")) {
            String[] p = msg.split("\\s+");
            netTimeLeft = Integer.parseInt(p[1]);
            return;
        }
        if (msg.startsWith("END ")) {
            String[] p = msg.split("\\s+");
            String result = p[1];
            int myMoney = Integer.parseInt(p[2]);
            int oppMoney = Integer.parseInt(p[3]);
            netMatchActive = false;
            netTimer.stop();
            if (result.equals("WIN")) {
                int gain = 1 + Math.max(0, (remotePoints - playerPoints) / 5);
                playerPoints += gain;
                setPoints(playerName, playerPoints);
                saveData();
                startPointsLabel.setText("Points: " + playerPoints);
                JOptionPane.showMessageDialog(null, "You win! $" + myMoney + " vs $" + oppMoney + "\nYou earned " + gain + " point(s)!");
            } else {
                JOptionPane.showMessageDialog(null, "You lose! $" + myMoney + " vs $" + oppMoney + "\n" + remoteName + " wins.");
            }
            closeNet();
            layoutCards.show(rootCards, "start");
        }
    }

    static void sendNetState() {
        if (!netMatchActive || netOut == null) return;
        netSendCounter++;
        if (netSendCounter % 4 != 0) return;
        netOut.println("S " + local.x + " " + local.y + " " + local.steps + " " + local.money + " " + local.jumpsLeft
                + " " + local.moneyMultiplier + " " + local.maxMoneyCap + " " + areaBitsOf(local));
        netOut.flush();
    }

    static void closeNet() {
        netRunning = false;
        netMatchActive = false;
        try {
            if (netOut != null) {
                netOut.println("LEAVE");
                netOut.flush();
            }
        } catch (Exception ignored) {
        }
        try {
            if (netSocket != null) netSocket.close();
        } catch (Exception ignored) {
        }
    }

    // ================= LEADERBOARD =================
    static void buildLeaderboard(JFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        leaderboardArea = new JTextArea(20, 30);
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        JScrollPane sp = new JScrollPane(leaderboardArea);
        p.add(sp, BorderLayout.CENTER);
        JButton back = new JButton("Back");
        back.addActionListener(e -> layoutCards.show(rootCards, "start"));
        JPanel bottom = new JPanel();
        bottom.add(back);
        p.add(bottom, BorderLayout.SOUTH);
        rootCards.add(p, "leaderboard");
    }

    static void refreshLeaderboard() {
        ArrayList<String[]> sorted = new ArrayList<>(leaderboard);
        sorted.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1]));
        StringBuilder sb = new StringBuilder();
        sb.append("Rank  Name                 Points\n");
        sb.append("----  ----                 ------\n");
        int rank = 1;
        for (String[] e : sorted) {
            sb.append(String.format("%-5d %-20s %d%n", rank, e[0], Integer.parseInt(e[1])));
            rank++;
        }
        leaderboardArea.setText(sb.toString());
    }

    // ================= DRAWING HELPERS =================
    static void drawBackground(Graphics2D g2d, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 250), 0, h, new Color(224, 244, 255));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(new Color(255, 220, 80));
        g2d.fillOval(w - 90, 25, 55, 55);
        g2d.setColor(new Color(255, 235, 140));
        g2d.fillOval(w - 80, 35, 35, 35);

        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillOval(60, 40, 70, 30);
        g2d.fillOval(100, 30, 60, 40);
        g2d.fillOval(130, 45, 55, 25);
        g2d.fillOval(320, 70, 60, 28);
        g2d.fillOval(350, 58, 55, 36);

        g2d.setColor(new Color(110, 200, 90));
        g2d.fillRect(0, h - 100, w, 100);
        g2d.setColor(new Color(90, 170, 70));
        g2d.fillRect(0, h - 12, w, 12);
    }

    static void drawCharacter(Graphics2D g2d, int x, int y, Color shirtColor) {
        Color skinColor = new Color(245, 200, 170);
        Color hairColor = new Color(70, 50, 40);
        Color pantsColor = new Color(50, 50, 60);
        Color shoeColor = new Color(80, 80, 80);

        g2d.setColor(shoeColor);
        g2d.fillOval(x + 5, y + 75, 16, 10);
        g2d.fillOval(x + 24, y + 75, 16, 10);

        g2d.setColor(pantsColor);
        g2d.fillRect(x + 8, y + 45, 12, 32);
        g2d.fillRect(x + 24, y + 45, 12, 32);

        g2d.setColor(shirtColor);
        g2d.fillRect(x + 5, y + 15, 34, 32);

        g2d.fillRect(x - 2, y + 15, 6, 22);
        g2d.fillRect(x + 40, y + 15, 6, 22);
        g2d.setColor(skinColor);
        g2d.fillOval(x - 3, y + 36, 8, 8);
        g2d.fillOval(x + 39, y + 36, 8, 8);

        g2d.fillRect(x + 17, y + 10, 10, 6);
        g2d.fillOval(x + 11, y - 12, 22, 24);

        g2d.setColor(hairColor);
        g2d.fillArc(x + 10, y - 15, 24, 18, 0, 180);
        g2d.fillRect(x + 10, y - 8, 3, 8);
        g2d.fillRect(x + 31, y - 8, 3, 8);

        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 16, y - 4, 3, 3);
        g2d.fillOval(x + 25, y - 4, 3, 3);
        g2d.drawArc(x + 18, y + 1, 8, 5, 0, -180);
    }

    // ================= GAME LOGIC HELPERS =================
    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    static boolean inArea(int i) {
        return circleX + 45 > areaX[i] && circleX < areaX[i] + areaWidth;
    }

    static boolean inBoostArea() {
        for (int i = 0; i < areaX.length; i++) {
            if (areaBought[i] && inArea(i)) {
                return true;
            }
        }
        return false;
    }

    static boolean inZone(int x) {
        for (int i = 0; i < areaX.length; i++) {
            if (x + 45 > areaX[i] && x < areaX[i] + areaWidth) {
                return true;
            }
        }
        return false;
    }

    // ================= PLAYER STATE & MULTIPLAYER HELPERS =================
    static class PlayerState {
        int x = 200, y = 0;
        int steps = 30, money = 0, jumpsLeft = 50;
        int moneyMultiplier = 10, maxMoneyCap = 10000;
        int stepCost = 10, maxMoneyCost = 25, multiplierCost = 25;
        double velY = 0;
        boolean onGround = false;
        boolean[] areaOwned = new boolean[areaX.length];

        void reset() {
            x = 200;
            y = 0;
            steps = 30;
            money = 0;
            jumpsLeft = 50;
            moneyMultiplier = 10;
            maxMoneyCap = 10000;
            stepCost = 10;
            maxMoneyCost = 25;
            multiplierCost = 25;
            velY = 0;
            onGround = false;
            areaOwned = new boolean[areaX.length];
        }
    }

    static void applyGravity(PlayerState ps, int panelHeight) {
        ps.velY += 0.8;
        ps.y += (int) ps.velY;
        int gY = panelHeight - 185;
        if (ps.y >= gY) {
            ps.y = gY;
            ps.velY = 0;
            ps.onGround = true;
        }
    }

    static void movePlayer(PlayerState ps, int dx, int panelWidth) {
        if (ps.steps <= 0) return;
        ps.x = clamp(ps.x + dx, 0, panelWidth - 50);
        ps.steps--;
        if (ps.money < ps.maxMoneyCap) {
            int gain = ps.moneyMultiplier * (ownsZone(ps, ps.x) ? 2 : 1);
            ps.money = Math.min(ps.maxMoneyCap, ps.money + gain);
        }
    }

    static void jumpPlayer(PlayerState ps) {
        if (ps.onGround && ps.jumpsLeft > 0) {
            ps.velY = -13;
            ps.onGround = false;
            ps.jumpsLeft--;
            ps.steps += 2;
        }
    }

    static boolean inZoneAt(int x, int i) {
        return x + 45 > areaX[i] && x < areaX[i] + areaWidth;
    }

    static boolean ownsZone(PlayerState ps, int x) {
        for (int i = 0; i < areaX.length; i++) {
            if (ps.areaOwned[i] && inZoneAt(x, i)) {
                return true;
            }
        }
        return false;
    }

    static int areaBitsOf(PlayerState ps) {
        int bits = 0;
        for (int i = 0; i < areaX.length; i++) {
            if (ps.areaOwned[i]) bits |= (1 << i);
        }
        return bits;
    }

    static void setAreaBits(PlayerState ps, int bits) {
        for (int i = 0; i < areaX.length; i++) {
            ps.areaOwned[i] = (bits & (1 << i)) != 0;
        }
    }

    static void drawZones(Graphics2D g2d, int h, PlayerState a, PlayerState b) {
        int groundTop = h - 100;
        for (int i = 0; i < areaX.length; i++) {
            boolean aOwn = a.areaOwned[i];
            boolean bOwn = b.areaOwned[i];
            if (aOwn && bOwn) {
                g2d.setColor(new Color(255, 215, 0, 170));
                g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                g2d.setColor(new Color(150, 110, 0));
                g2d.drawString("x2", areaX[i] + 35, groundTop - 30);
            } else if (aOwn) {
                g2d.setColor(new Color(40, 100, 200, 170));
                g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                g2d.setColor(Color.WHITE);
                g2d.drawString("x2", areaX[i] + 35, groundTop - 30);
            } else if (bOwn) {
                g2d.setColor(new Color(200, 60, 60, 170));
                g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                g2d.setColor(Color.WHITE);
                g2d.drawString("x2", areaX[i] + 35, groundTop - 30);
            } else {
                g2d.setColor(new Color(255, 255, 255, 190));
                g2d.fillRect(areaX[i], groundTop - 70, areaWidth, 70);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(areaX[i], groundTop - 70, areaWidth, 70);
                g2d.drawString("$" + AREA_COST, areaX[i] + 22, groundTop - 30);
            }
        }
    }

    static JPanel buildPlayerShop(JFrame frame, PlayerState ps, JComponent view, String ownerLabel, JButton extraButton) {
        JPanel sp = new JPanel(new FlowLayout());
        JLabel lab = new JLabel(ownerLabel + ":");
        lab.setFont(new Font("Arial", Font.BOLD, 13));
        sp.add(lab);

        JButton buySteps = new JButton("Buy 10 Steps ($" + ps.stepCost + ")");
        JButton buyMax = new JButton("Upgrade Max Money ($" + ps.maxMoneyCost + ")");
        JButton buyMult = new JButton("Buy +10 Money/Step ($" + ps.multiplierCost + ")");
        JButton helpBtn = new JButton("Help (-$" + HELP_DISCOUNT + ")");
        JButton[] areaBtns = new JButton[3];

        buySteps.addActionListener(e -> {
            if (ps.money >= ps.stepCost) {
                ps.money -= ps.stepCost;
                ps.steps += 10;
                ps.stepCost += 5;
                buySteps.setText("Buy 10 Steps ($" + ps.stepCost + ")");
                view.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });
        buyMax.addActionListener(e -> {
            if (ps.money >= ps.maxMoneyCost) {
                ps.money -= ps.maxMoneyCost;
                ps.maxMoneyCap += 1000;
                ps.maxMoneyCost *= 2;
                buyMax.setText("Upgrade Max Money ($" + ps.maxMoneyCost + ")");
                view.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });
        buyMult.addActionListener(e -> {
            if (ps.money >= ps.multiplierCost) {
                ps.money -= ps.multiplierCost;
                ps.moneyMultiplier += 10;
                ps.multiplierCost *= 2;
                buyMult.setText("Buy +10 Money/Step ($" + ps.multiplierCost + ")");
                view.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Not enough money!");
            }
        });
        helpBtn.addActionListener(e -> {
            if (ps.money > 100000) {
                ps.stepCost -= HELP_DISCOUNT;
                ps.maxMoneyCost -= HELP_DISCOUNT;
                ps.multiplierCost -= HELP_DISCOUNT;
                buySteps.setText("Buy 10 Steps ($" + ps.stepCost + ")");
                buyMax.setText("Upgrade Max Money ($" + ps.maxMoneyCost + ")");
                buyMult.setText("Buy +10 Money/Step ($" + ps.multiplierCost + ")");
                view.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Need more than $100000 to use help!");
            }
        });
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            areaBtns[i] = new JButton("Buy Area " + (idx + 1) + " ($" + AREA_COST + ")");
            areaBtns[i].addActionListener(e -> {
                if (ps.areaOwned[idx]) return;
                if (!inZoneAt(ps.x, idx)) {
                    JOptionPane.showMessageDialog(frame, "Stand inside area " + (idx + 1) + " to buy it!");
                    return;
                }
                if (ps.money >= AREA_COST) {
                    ps.money -= AREA_COST;
                    ps.areaOwned[idx] = true;
                    areaBtns[idx].setText("Area " + (idx + 1) + " Owned");
                    view.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, "Not enough money!");
                }
            });
        }

        sp.add(buySteps);
        sp.add(buyMax);
        sp.add(buyMult);
        for (int i = 0; i < 3; i++) sp.add(areaBtns[i]);
        sp.add(helpBtn);
        if (extraButton != null) sp.add(extraButton);
        return sp;
    }

    static void botShop(PlayerState b) {
        if (b.money >= b.maxMoneyCost) {
            b.money -= b.maxMoneyCost;
            b.maxMoneyCap += 1000;
            b.maxMoneyCost *= 2;
        }
        if (b.steps < 15 && b.money >= b.stepCost) {
            b.money -= b.stepCost;
            b.steps += 10;
            b.stepCost += 5;
        }
        if (b.money >= b.multiplierCost && Math.random() < 0.03) {
            b.money -= b.multiplierCost;
            b.moneyMultiplier += 10;
            b.multiplierCost *= 2;
        }
        if (b.money >= AREA_COST) {
            for (int i = 0; i < 3; i++) {
                if (!b.areaOwned[i] && inZoneAt(b.x, i)) {
                    b.money -= AREA_COST;
                    b.areaOwned[i] = true;
                    break;
                }
            }
        }
        if (b.money > 100000) {
            b.stepCost -= HELP_DISCOUNT;
            b.maxMoneyCost -= HELP_DISCOUNT;
            b.multiplierCost -= HELP_DISCOUNT;
        }
    }

    // ================= PERSISTENCE =================
    static void loadData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_player.txt"));
            String line = br.readLine();
            if (line != null && !line.trim().isEmpty()) playerName = line.trim();
            line = br.readLine();
            if (line != null) playerPoints = Integer.parseInt(line.trim());
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_leaderboard.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    leaderboard.add(new String[]{parts[0], parts[1]});
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_friends.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) friends.add(line.trim());
            }
            br.close();
        } catch (Exception ignored) {
        }
    }

    static void saveData() {
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_player.txt"));
            w.println(playerName);
            w.println(playerPoints);
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_leaderboard.txt"));
            for (String[] e : leaderboard) {
                w.println(e[0] + "|" + e[1]);
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_friends.txt"));
            for (String f : friends) {
                w.println(f);
            }
            w.close();
        } catch (Exception ignored) {
        }
    }

    static void setPoints(String name, int pts) {
        for (String[] e : leaderboard) {
            if (e[0].equals(name)) {
                e[1] = "" + pts;
                return;
            }
        }
        leaderboard.add(new String[]{name, "" + pts});
    }

    static int getPointsOf(String name) {
        for (String[] e : leaderboard) {
            if (e[0].equals(name)) {
                return Integer.parseInt(e[1]);
            }
        }
        return 0;
    }
}
