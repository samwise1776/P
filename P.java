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

    // Th3GreatPlayer VIP: infinite steps & storage, 500 money per step.
    static boolean isVip() {
        return playerName != null && playerName.equalsIgnoreCase("th3greatplayer");
    }

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

    // ---- single player combat ----
    static final int PLAYER_MAX_HEALTH = 5000;
    static int playerHealth = PLAYER_MAX_HEALTH;
    static int kills = 0;
    static Weapon equippedWeapon = new Sword();
    static Weapon currentWeapon = equippedWeapon;
    static int facing = 1;
    static long lastAttackMs = 0;
    static long swingEffectUntil = 0;
    static ArrayList<Enemy> enemies = new ArrayList<>();
    static ArrayList<Projectile> projectiles = new ArrayList<>();
    static long lastSpawnMs = 0;
    static boolean bossAlive = false;
    static int bossWaves = 0;
    static long bossMessageUntil = 0;
    static boolean gameOver = false;
    static ArrayList<Weapon> ownedWeapons = new ArrayList<>();

    // ---- shared constants ----
    static final int HELP_DISCOUNT = 10000;
    static final int AREA_COST = 2000;
    static final int[] areaX = {120, 350, 580};
    static final int areaWidth = 90;
    static boolean[] areaBought = new boolean[areaX.length];

    // ---- multiplayer state ----
    static PlayerState local = new PlayerState();
    static ArrayList<PlayerState> bots = new ArrayList<>();
    static int botCount = 1;
    static final Color[] BOT_COLORS = {
            new Color(200, 60, 60), new Color(230, 150, 0), new Color(160, 90, 210),
            new Color(0, 150, 150), new Color(200, 60, 180), new Color(120, 170, 40),
            new Color(90, 90, 210), new Color(210, 120, 160), new Color(140, 100, 60),
            new Color(80, 80, 80)
    };
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

        JButton weaponShopBtn = new JButton("Weapon Shop");
        weaponShopBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        weaponShopBtn.addActionListener(e -> openWeaponShop(frame));

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
        center.add(weaponShopBtn);
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

                for (Enemy e : enemies) {
                    if (!e.dead) e.draw(g2d);
                }

                drawCharacter(g2d, circleX, circleY, new Color(40, 100, 200));

                for (Projectile pr : projectiles) pr.draw(g2d);

                if (currentWeapon != null && !currentWeapon.ranged && System.currentTimeMillis() < swingEffectUntil) {
                    g2d.setColor(new Color(255, 255, 180, 130));
                    int reach = currentWeapon.range;
                    g2d.drawArc(circleX + 22 - reach, circleY + 30 - reach, reach * 2, reach * 2,
                            facing > 0 ? -70 : 110, 140);
                }

                if (System.currentTimeMillis() < bossMessageUntil) {
                    g2d.setColor(new Color(255, 40, 40));
                    g2d.setFont(new Font("Arial", Font.BOLD, 34));
                    String m = "A BOSS HAS ARRIVED!";
                    g2d.drawString(m, w / 2 - g2d.getFontMetrics().stringWidth(m) / 2, h / 2);
                }

                drawBossBar(g2d, w);
                drawPlayerHud(g2d);
            }
        };
        paint.setBackground(Color.WHITE);

        paint.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                attackAt(e.getX(), e.getY());
            }
        });

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
        im.put(KeyStroke.getKeyStroke('F'), "attack");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "attack");
        for (int i = 0; i <= 9; i++) {
            im.put(KeyStroke.getKeyStroke(Character.forDigit(i, 10)), "weapon" + i);
            im.put(KeyStroke.getKeyStroke((char) ('0' + i)), "weapon" + i);
        }
        im.put(KeyStroke.getKeyStroke('Q'), "prevWeapon");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "prevWeapon");
        im.put(KeyStroke.getKeyStroke('E'), "nextWeapon");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "nextWeapon");

        am.put("attack", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!gameOver) playerAttack();
            }
        });

        for (int i = 0; i <= 9; i++) {
            final int idx = i;
            am.put("weapon" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (!gameOver) switchWeapon(idx);
                }
            });
        }
        am.put("prevWeapon", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!gameOver) cycleWeapon(-1);
            }
        });
        am.put("nextWeapon", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!gameOver) cycleWeapon(1);
            }
        });

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

            // ---- combat update ----
            long now = System.currentTimeMillis();
            if (!gameOver) {
                if (!bossAlive && kills >= 25 * (bossWaves + 1)) {
                    bossAlive = true;
                    bossWaves++;
                    Boss b = new Boss(paint.getWidth() / 2);
                    b.y = gY;
                    b.onGround = true;
                    enemies.add(b);
                    bossMessageUntil = now + 2500;
                }
                if (!bossAlive && enemies.size() < 6 && now - lastSpawnMs > 3000) {
                    lastSpawnMs = now;
                    int ex = Math.random() < 0.5 ? -40 : paint.getWidth();
                    Enemy en = Math.random() < 0.4 ? new RangedEnemy(ex) : new MeleeEnemy(ex);
                    en.y = gY;
                    en.onGround = true;
                    enemies.add(en);
                }
                for (Enemy en : enemies) {
                    if (!en.dead) en.act(paint.getWidth(), paint.getHeight());
                }
                enemies.removeIf(en -> en.dead);
                for (Projectile pr : projectiles) pr.update();
                projectiles.removeIf(pr -> pr.dead);
            }
            paint.repaint();
        });
    }

    static void resetSinglePlayer() {
        circleX = 400;
        circleY = 300;
        steps = isVip() ? Integer.MAX_VALUE : 10;
        money = 0;
        stepCost = 10;
        maxMoneyCost = 25;
        multiplierCost = 25;
        moneyMultiplier = isVip() ? Integer.MAX_VALUE - 10 : 10;
        maxMoneyCap = isVip() ? Integer.MAX_VALUE : 1000;
        velY = 0;
        onGround = false;
        jumpsLeft = 50;
        areaBought = new boolean[areaX.length];
        playerHealth = PLAYER_MAX_HEALTH;
        kills = 0;
        currentWeapon = equippedWeapon;
        facing = 1;
        lastAttackMs = 0;
        swingEffectUntil = 0;
        enemies.clear();
        projectiles.clear();
        lastSpawnMs = 0;
        bossAlive = false;
        bossWaves = 0;
        bossMessageUntil = 0;
        gameOver = false;
    }

    private static void move(int dx, JPanel panel) {
        if (steps <= 0) return;

        circleX = clamp(circleX + dx, 0, panel.getWidth() - 50);
        if (dx != 0) facing = dx > 0 ? 1 : -1;

        if (!isVip()) steps--;

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

                PlayerState botsOwned = new PlayerState();
                for (PlayerState b : bots) {
                    for (int i = 0; i < areaX.length; i++) {
                        if (b.areaOwned[i]) botsOwned.areaOwned[i] = true;
                    }
                }
                drawZones(g2d, h, local, botsOwned);

                drawCharacter(g2d, local.x, local.y, new Color(40, 100, 200));
                for (int i = 0; i < bots.size(); i++) {
                    PlayerState b = bots.get(i);
                    drawCharacter(g2d, b.x, b.y, BOT_COLORS[i % BOT_COLORS.length]);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 11));
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(b.name, b.x + 2, b.y - 18);
                }

                // UI
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.setColor(Color.BLACK);
                g2d.drawString("Time left: " + Math.max(0, timeLeft) + "s", 20, 30);
                g2d.drawString(playerName + " (You): $" + local.money + " | Steps: " + local.steps + " | +$" + local.moneyMultiplier + "/step", 20, 55);
                g2d.drawString("Opponents (" + bots.size() + " bots): $" + botsTotalMoney() + " | Best: " + bestBot().name + " $" + bestBot().money, 20, 80);
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
                    local.onGround = true;
                    for (PlayerState b : bots) {
                        b.y = g;
                        b.onGround = true;
                    }
                    playersPlaced = true;
                }
            }

            applyGravity(local, multiPaint.getHeight());
            local.x = clamp(local.x, 0, multiPaint.getWidth() - 50);
            for (PlayerState b : bots) {
                applyGravity(b, multiPaint.getHeight());
                b.x = clamp(b.x, 0, multiPaint.getWidth() - 50);
            }

            // bot AI
            botCounter++;
            if (botCounter % 8 == 0) {
                for (PlayerState b : bots) {
                    int dir = (int) (Math.random() * 3) - 1;
                    if (dir != 0 && b.steps > 0) {
                        movePlayer(b, dir * 15, multiPaint.getWidth());
                    }
                    if (Math.random() < 0.2 && b.onGround && b.jumpsLeft > 0) {
                        jumpPlayer(b);
                    }
                }
            }
            for (PlayerState b : bots) botShop(b);

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

        String bt = JOptionPane.showInputDialog(rootCards, "How many AI opponents? (1-10, more = more lag):", "" + botCount);
        int bc = botCount;
        if (bt != null) {
            try {
                bc = Integer.parseInt(bt.trim());
            } catch (Exception ignored) {
            }
        }
        botCount = Math.max(1, Math.min(10, bc));

        opponentName = oName;
        opponentPoints = oPoints;
        timeLimit = lim;

        local.reset();
        bots.clear();
        for (int i = 0; i < botCount; i++) {
            PlayerState b = new PlayerState();
            b.reset();
            b.name = (i == 0) ? opponentName : "Rival " + (i + 1);
            b.x = clamp(120 + 55 * i, 0, 700);
            b.moneyMultiplier = Math.max(5, 10 + opponentPoints / 5 - i * 2);
            b.steps = 30 + opponentPoints / 10;
            bots.add(b);
        }
        local.x = 200;
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

        PlayerState best = bestBot();
        boolean win = local.money > best.money;
        String msg;
        if (win) {
            int gain = bots.size() + Math.max(0, (opponentPoints - playerPoints) / 5);
            playerPoints += gain;
            setPoints(playerName, playerPoints);
            saveData();
            msg = "You win! $" + local.money + " vs best " + best.name + " $" + best.money + "\nYou earned " + gain + " point(s)!";
            startPointsLabel.setText("Points: " + playerPoints);
        } else {
            msg = "You lose! $" + local.money + " vs " + best.name + " $" + best.money + "\n" + best.name + " wins.";
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
        String name = "";
        int x = 200, y = 0;
        int steps = 30, money = 0, jumpsLeft = 50;
        int moneyMultiplier = 10, maxMoneyCap = 10000;
        int stepCost = 10, maxMoneyCost = 25, multiplierCost = 25;
        double velY = 0;
        boolean onGround = false;
        boolean[] areaOwned = new boolean[areaX.length];

        void reset() {
            boolean vip = (this == local) && isVip();
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

    static PlayerState bestBot() {
        PlayerState best = bots.get(0);
        for (PlayerState b : bots) {
            if (b.money > best.money) best = b;
        }
        return best;
    }

    static long botsTotalMoney() {
        long total = 0;
        for (PlayerState b : bots) total += b.money;
        return total;
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

    // ================= WEAPONS =================
    static abstract class Weapon {
        String name;
        int damage;
        int range;
        boolean ranged;
        long cooldownMs;
        int price;
        int projSpeed;
        String desc;

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
    }

    static class Sword extends Weapon { Sword() { super("Sword", 35, 85, false, 500, 0, 0, "A balanced starting blade. 35 dmg."); } }
    static class Spear extends Weapon { Spear() { super("Spear", 45, 130, false, 550, 80, 0, "Long reach melee. 45 dmg."); } }
    static class Axe extends Weapon { Axe() { super("Axe", 60, 85, false, 750, 50, 0, "Heavy melee. 60 dmg."); } }
    static class Hammer extends Weapon { Hammer() { super("Hammer", 95, 95, false, 1200, 200, 0, "Massive slow smash. 95 dmg."); } }
    static class Bow extends Weapon { Bow() { super("Bow", 30, 650, true, 450, 60, 12, "Quick long-range arrows. 30 dmg."); } }
    static class Crossbow extends Weapon { Crossbow() { super("Crossbow", 50, 700, true, 900, 150, 14, "Powerful ranged bolts. 50 dmg."); } }
    static class MagicStaff extends Weapon { MagicStaff() { super("Magic Staff", 80, 550, true, 1100, 300, 9, "Slow magic bolts. 80 dmg."); } }

    static final Weapon[] ALL_WEAPONS = {new Sword(), new Spear(), new Axe(), new Hammer(), new Bow(), new Crossbow(), new MagicStaff()};

    static Weapon weaponOf(Class<? extends Weapon> cls) {
        if (cls.equals(Spear.class)) return new Spear();
        if (cls.equals(Axe.class)) return new Axe();
        if (cls.equals(Hammer.class)) return new Hammer();
        if (cls.equals(Bow.class)) return new Bow();
        if (cls.equals(Crossbow.class)) return new Crossbow();
        if (cls.equals(MagicStaff.class)) return new MagicStaff();
        return new Sword();
    }

    static Weapon weaponByName(String name) {
        for (Weapon w : ALL_WEAPONS) {
            if (w.name.equalsIgnoreCase(name)) return weaponOf(w.getClass());
        }
        return null;
    }

    static void damagePlayer(int dmg) {
        if (gameOver) return;
        playerHealth -= dmg;
        if (playerHealth <= 0) {
            playerHealth = 0;
            gameOver = true;
            singleTimer.stop();
            JOptionPane.showMessageDialog(paint, "You were defeated! Kills: " + kills);
            layoutCards.show(rootCards, "start");
        }
    }

    static void playerAttack() {
        if (gameOver || currentWeapon == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAttackMs < currentWeapon.cooldownMs) return;
        lastAttackMs = now;
        if (currentWeapon.ranged) {
            int sx = circleX + 22 + facing * 24;
            int sy = circleY + 32;
            projectiles.add(new Projectile(sx, sy, facing * currentWeapon.projSpeed, 0, currentWeapon.damage, false, projectileColor()));
        } else {
            swingEffectUntil = now + 120;
            int reach = currentWeapon.range;
            int pcx = circleX + 22, pcy = circleY + 30;
            for (Enemy e : enemies) {
                if (e.dead) continue;
                int cx = e.x + e.width / 2, cy = e.y - e.height / 2;
                int ddx = cx - pcx, ddy = cy - pcy;
                if (facing > 0 && ddx < -24) continue;
                if (facing < 0 && ddx > 24) continue;
                if (ddx * ddx + ddy * ddy < reach * reach) e.hurt(currentWeapon.damage);
            }
        }
    }

    static Color projectileColor() {
        if (currentWeapon instanceof Bow) return new Color(150, 100, 60);
        if (currentWeapon instanceof Crossbow) return new Color(90, 70, 55);
        return new Color(120, 80, 220);
    }

    static void attackAt(int mx, int my) {
        if (gameOver || currentWeapon == null) return;
        facing = mx >= circleX + 22 ? 1 : -1;
        long now = System.currentTimeMillis();
        if (now - lastAttackMs < currentWeapon.cooldownMs) return;
        lastAttackMs = now;
        if (currentWeapon.ranged) {
            double nx = mx - (circleX + 22), ny = my - (circleY + 30);
            double len = Math.hypot(nx, ny);
            double sp = currentWeapon.projSpeed;
            double vx = len > 0 ? nx / len * sp : facing * sp;
            double vy = len > 0 ? ny / len * sp : 0;
            projectiles.add(new Projectile(circleX + 22 + facing * 24, circleY + 32, vx, vy, currentWeapon.damage, false, projectileColor()));
        } else {
            swingEffectUntil = now + 120;
            int reach = currentWeapon.range;
            int pcx = circleX + 22, pcy = circleY + 30;
            for (Enemy e : enemies) {
                if (e.dead) continue;
                int cx = e.x + e.width / 2, cy = e.y - e.height / 2;
                int ddx = cx - pcx, ddy = cy - pcy;
                if (facing > 0 && ddx < -24) continue;
                if (facing < 0 && ddx > 24) continue;
                if (ddx * ddx + ddy * ddy < reach * reach) e.hurt(currentWeapon.damage);
            }
        }
    }

    static void switchWeapon(int index) {
        if (ownedWeapons.isEmpty()) return;
        if (index < 1 || index > ownedWeapons.size()) return;
        Weapon w = ownedWeapons.get(index - 1);
        if (w == null) return;
        currentWeapon = w;
        lastAttackMs = 0;
    }

    static void cycleWeapon(int dir) {
        if (ownedWeapons.isEmpty()) return;
        int idx = ownedWeapons.indexOf(currentWeapon);
        int n = ownedWeapons.size();
        int next = ((idx + dir) % n + n) % n;
        currentWeapon = ownedWeapons.get(next);
        lastAttackMs = 0;
    }

    // ================= ENEMIES & PROJECTILES =================
    static abstract class Enemy {
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
            return circleX - x >= 0 ? 1 : -1;
        }

        void gravity(int panelHeight) {
            velY += 0.8;
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
                money = (int) Math.min((long) maxMoneyCap, (long) money + 500);
                playerPoints += 25;
                setPoints(playerName, playerPoints);
                saveData();
                startPointsLabel.setText("Points: " + playerPoints);
                bossAlive = false;
                JOptionPane.showMessageDialog(paint, "BOSS DEFEATED!\n+$500 and +25 points!");
            } else {
                money = (int) Math.min((long) maxMoneyCap, (long) money + 50);
                kills++;
                playerPoints++;
                setPoints(playerName, playerPoints);
                saveData();
                startPointsLabel.setText("Points: " + playerPoints);
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

    static class MeleeEnemy extends Enemy {
        MeleeEnemy(int x) {
            super(100, 10, 2, 55, false, new Color(210, 70, 60), 40, 46);
            this.x = x;
        }

        void act(int panelWidth, int panelHeight) {
            gravity(panelHeight);
            int dx = circleX - this.x;
            if (Math.abs(dx) > range) {
                x = clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
            }
            long now = System.currentTimeMillis();
            if (Math.abs(dx) <= range + 34 && Math.abs(circleY - (y - height)) < 90) {
                if (now - lastAttackMs > 800) {
                    lastAttackMs = now;
                    damagePlayer(damage);
                }
            }
        }
    }

    static class RangedEnemy extends Enemy {
        RangedEnemy(int x) {
            super(100, 25, 1, 320, true, new Color(150, 90, 210), 36, 44);
            this.x = x;
        }

        void act(int panelWidth, int panelHeight) {
            gravity(panelHeight);
            int dx = circleX - this.x;
            if (Math.abs(dx) > range) {
                x = clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
            } else if (Math.abs(dx) < 150) {
                x = clamp(x - (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
            }
            long now = System.currentTimeMillis();
            if (now - lastAttackMs > 1200) {
                lastAttackMs = now;
                double sx = x + width / 2.0, sy = y - height / 2.0;
                double tx = circleX + 22, ty = circleY + 30;
                double nx = tx - sx, ny = ty - sy;
                double len = Math.hypot(nx, ny);
                if (len > 0) {
                    double sp = 9;
                    projectiles.add(new Projectile((int) sx, (int) sy, nx / len * sp, ny / len * sp, 25, true, new Color(140, 90, 50)));
                }
            }
        }
    }

    static class Boss extends Enemy {
        Boss(int x) {
            super(500, 200, 1, 100, false, new Color(110, 20, 160), 92, 110);
            this.x = x;
        }

        void act(int panelWidth, int panelHeight) {
            gravity(panelHeight);
            int dx = circleX - this.x;
            if (Math.abs(dx) > range) {
                x = clamp(x + (dx > 0 ? 1 : -1) * speed, 0, panelWidth - width);
            }
            long now = System.currentTimeMillis();
            if (now - lastAttackMs > 1500) {
                lastAttackMs = now;
                if (Math.abs(dx) > 220) {
                    double sx = x + width / 2.0, sy = y - height / 2.0;
                    double tx = circleX + 22, ty = circleY + 30;
                    double nx = tx - sx, ny = ty - sy;
                    double len = Math.hypot(nx, ny);
                    if (len > 0) {
                        double sp = 7;
                        projectiles.add(new Projectile((int) sx, (int) sy, nx / len * sp, ny / len * sp, 200, true, new Color(200, 60, 220)));
                    }
                } else if (Math.abs(circleY - (y - height)) < 120) {
                    damagePlayer(200);
                }
            }
        }
    }

    static class Projectile {
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
                if (Math.abs(x - (circleX + 22)) < 26 && Math.abs(y - (circleY + 30)) < 42) {
                    damagePlayer(damage);
                    dead = true;
                }
            } else {
                for (Enemy e : enemies) {
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

    // ================= COMBAT HUD =================
    static void drawPlayerHud(Graphics2D g2d) {
        int bw = 190, bh = 16;
        int bx = 20, by = 20;
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(bx - 2, by - 2, bw + 4, bh + 4, 6, 6);
        g2d.setColor(new Color(120, 20, 20));
        g2d.fillRoundRect(bx, by, bw, bh, 4, 4);
        g2d.setColor(new Color(60, 210, 60));
        g2d.fillRoundRect(bx, by, (int) (bw * Math.max(0, playerHealth) / (double) PLAYER_MAX_HEALTH), bh, 4, 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("HP " + Math.max(0, playerHealth) + "/" + PLAYER_MAX_HEALTH, bx + 5, by + 13);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        g2d.drawString("Money: $" + money + " / $" + maxMoneyCap, 20, 58);
        g2d.drawString("Steps: " + steps, 20, 80);
        g2d.drawString("Money/Step: $" + moneyMultiplier, 20, 102);
        g2d.drawString("Jumps Left: " + jumpsLeft, 20, 124);

        // owned weapon hotkeys
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        int wy = 150;
        for (int i = 0; i < ownedWeapons.size(); i++) {
            Weapon w = ownedWeapons.get(i);
            boolean eq = currentWeapon != null && currentWeapon.getClass().equals(w.getClass());
            g2d.setColor(eq ? new Color(0, 120, 220) : Color.BLACK);
            g2d.drawString((i + 1) + ": " + w.name + (eq ? "  <--" : ""), 20, wy);
            wy += 18;
        }
        g2d.setColor(Color.BLACK);
        g2d.drawString("Kills: " + kills + " | Boss in " + (25 * (bossWaves + 1) - kills) + " | Q/E to switch", 20, wy + 4);
        if (steps <= 0) {
            g2d.setColor(Color.RED);
            g2d.drawString("OUT OF STEPS! Buy more below.", 20, wy + 24);
        }
    }

    static void drawBossBar(Graphics2D g2d, int w) {
        Enemy boss = null;
        for (Enemy e : enemies) {
            if (e.maxHealth >= 500 && !e.dead) {
                boss = e;
                break;
            }
        }
        if (boss == null) return;
        int bw = 360, bh = 18;
        int bx = (w - bw) / 2, by = 16;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(bx - 2, by - 2, bw + 4, bh + 4, 8, 8);
        g2d.setColor(new Color(90, 20, 20));
        g2d.fillRoundRect(bx, by, bw, bh, 6, 6);
        g2d.setColor(new Color(210, 40, 40));
        g2d.fillRoundRect(bx, by, (int) (bw * Math.max(0, boss.health) / (double) boss.maxHealth), bh, 6, 6);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("BOSS  " + Math.max(0, boss.health) + "/" + boss.maxHealth, bx + 8, by + 14);
    }

    // ================= WEAPON SHOP =================
    static JList<String> weaponShopList;
    static JTextArea weaponShopInfo;
    static JLabel weaponShopPoints;

    static void openWeaponShop(JFrame frame) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Weapon w : ALL_WEAPONS) {
            model.addElement(w.name + "   $" + w.price + "   " + (w.ranged ? "Long-Ranged" : "Melee"));
        }
        weaponShopList = new JList<>(model);
        weaponShopList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponShopList.addListSelectionListener(e -> updateWeaponShopInfo());
        JScrollPane sp = new JScrollPane(weaponShopList);
        sp.setPreferredSize(new Dimension(290, 230));

        weaponShopInfo = new JTextArea(7, 34);
        weaponShopInfo.setEditable(false);
        weaponShopInfo.setFont(new Font("Monospaced", Font.PLAIN, 13));

        weaponShopPoints = new JLabel("Points: " + playerPoints);
        weaponShopPoints.setFont(new Font("Arial", Font.BOLD, 14));

        JButton buyBtn = new JButton("Buy");
        JButton equipBtn = new JButton("Equip");
        buyBtn.addActionListener(e -> {
            int idx = weaponShopList.getSelectedIndex();
            if (idx < 0) return;
            Weapon w = ALL_WEAPONS[idx];
            boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.getClass().equals(w.getClass()));
            if (owned) {
                JOptionPane.showMessageDialog(frame, "You already own the " + w.name + "!");
                return;
            }
            if (playerPoints < w.price) {
                JOptionPane.showMessageDialog(frame, "Not enough points! Need " + w.price + " (get points by killing enemies and bosses).");
                return;
            }
            playerPoints -= w.price;
            Weapon ownedW = weaponOf(w.getClass());
            ownedWeapons.add(ownedW);
            equippedWeapon = ownedW;
            currentWeapon = ownedW;
            setPoints(playerName, playerPoints);
            saveData();
            weaponShopPoints.setText("Points: " + playerPoints);
            if (startPointsLabel != null) startPointsLabel.setText("Points: " + playerPoints);
            updateWeaponShopInfo();
            JOptionPane.showMessageDialog(frame, "Bought " + w.name + "! Damage: " + w.damage);
        });
        equipBtn.addActionListener(e -> {
            int idx = weaponShopList.getSelectedIndex();
            if (idx < 0) return;
            Weapon w = ALL_WEAPONS[idx];
            boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.getClass().equals(w.getClass()));
            if (!owned) {
                JOptionPane.showMessageDialog(frame, "Buy the " + w.name + " first!");
                return;
            }
            equippedWeapon = weaponOf(w.getClass());
            currentWeapon = equippedWeapon;
            saveData();
            updateWeaponShopInfo();
            JOptionPane.showMessageDialog(frame, "Equipped " + w.name + "!");
        });

        weaponShopList.setSelectedIndex(0);
        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(buyBtn);
        bottom.add(equipBtn);
        bottom.add(weaponShopPoints);
        panel.add(sp, BorderLayout.WEST);
        panel.add(weaponShopInfo, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        JOptionPane.showConfirmDialog(frame, panel, "Weapon Shop", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    static void updateWeaponShopInfo() {
        int idx = weaponShopList.getSelectedIndex();
        if (idx < 0) {
            weaponShopInfo.setText("");
            return;
        }
        Weapon w = ALL_WEAPONS[idx];
        boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.getClass().equals(w.getClass()));
        StringBuilder sb = new StringBuilder();
        sb.append(w.name).append("  |  ").append(w.ranged ? "Long-Ranged" : "Melee").append("\n");
        sb.append("Damage: ").append(w.damage).append("\n");
        sb.append(w.ranged ? "Range: " : "Reach: ").append(w.range).append("px\n");
        sb.append("Cooldown: ").append(w.cooldownMs).append("ms\n");
        sb.append("Price: ").append(w.price).append(" points\n\n");
        sb.append(w.desc).append("\n");
        if (owned) {
            sb.append(equippedWeapon != null && equippedWeapon.getClass().equals(w.getClass()) ? "[OWNED] [EQUIPPED]" : "[OWNED]");
        } else {
            sb.append("[NOT OWNED]");
        }
        weaponShopInfo.setText(sb.toString());
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
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_weapons.txt"));
            String first = br.readLine();
            String line;
            ownedWeapons.clear();
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Weapon w = weaponByName(line.trim());
                    if (w != null && !ownedWeapons.stream().anyMatch(ow -> ow.getClass().equals(w.getClass()))) {
                        ownedWeapons.add(w);
                    }
                }
            }
            br.close();
            if (ownedWeapons.isEmpty()) ownedWeapons.add(new Sword());
            Weapon eq = weaponByName(first == null ? "Sword" : first.trim());
            equippedWeapon = eq != null ? eq : ownedWeapons.get(0);
            currentWeapon = equippedWeapon;
        } catch (Exception ignored) {
            if (ownedWeapons.isEmpty()) ownedWeapons.add(new Sword());
            equippedWeapon = ownedWeapons.get(0);
            currentWeapon = equippedWeapon;
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
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_weapons.txt"));
            w.println(equippedWeapon != null ? equippedWeapon.name : "Sword");
            for (Weapon ow : ownedWeapons) {
                w.println(ow.name);
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
