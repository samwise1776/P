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

    static boolean isVip() {
        return playerName != null && vipUsers.containsKey(playerName.toLowerCase());
    }

    static boolean hasPerk(String perk) {
        java.util.List<String> perks = playerName == null ? null : vipUsers.get(playerName.toLowerCase());
        return perks != null && perks.contains(perk);
    }

    // ================= DEVELOPER MODE DATA =================
    static final String DEV_PASSWORD = "12409";
    static LinkedHashMap<String, java.util.List<String>> vipUsers = new LinkedHashMap<>();
    static LinkedHashMap<String, String> vipProfiles = new LinkedHashMap<>();
    static LinkedHashMap<String, String> emblems = new LinkedHashMap<>();
    static HashMap<String, String> userEmblems = new HashMap<>();
    static LinkedHashMap<String, String[]> playerRecords = new LinkedHashMap<>();

    // editable game settings (Developer Mode)
    static double GRAVITY = 0.8;
    static int JUMP_VELOCITY = -13;
    static int PLAYER_SPEED = 15;
    static double playerScale = 1.0;
    static Color SKY_TOP = new Color(135, 206, 250);
    static Color SKY_BOTTOM = new Color(224, 244, 255);
    static Color GROUND_TOP = new Color(110, 200, 90);
    static Color GROUND_STRIPE = new Color(90, 170, 70);
    static int START_MONEY = 0;
    static int START_STEPS = 10;
    static int DEFAULT_MONEY_MULTIPLIER = 10;
    static int DEFAULT_MAX_MONEY_CAP = 1000;
    static int SPAWN_X = 400;
    static int SPAWN_Y = 300;
    static long ENEMY_SPAWN_INTERVAL = 3000;
    static int BOSS_WAVE_KILLS = 25;
    static int CHEST_EVERY = 5;
    static JPanel vipListPanel;

    static int playerWidth() { return (int) Math.round(50 * playerScale); }
    static int playerHeight() { return (int) Math.round(88 * playerScale); }

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
    static int PLAYER_MAX_HEALTH = 5000;
    static int playerMaxHealth = PLAYER_MAX_HEALTH;
    static int playerHealth = PLAYER_MAX_HEALTH;
    static int playerLevel = 1;
    static int levelKills = 0;
    static int totalKills = 0;
    static int kills = 0;
    static Weapon equippedWeapon = new Weapon("Sword", 35, 85, false, 500, 0, 0, "A balanced starting blade. 35 dmg.");
    static Weapon currentWeapon = equippedWeapon;
    static int facing = 1;
    static long lastAttackMs = 0;
    static long swingEffectUntil = 0;
    static ArrayList<Enemy> enemies = new ArrayList<>();
    static ArrayList<Projectile> projectiles = new ArrayList<>();
    static ArrayList<Chest> chests = new ArrayList<>();
    static ArrayList<Block> blocks = new ArrayList<>();
    static long lastSpawnMs = 0;
    static boolean bossAlive = false;
    static int bossWaves = 0;
    static long bossMessageUntil = 0;
    static boolean gameOver = false;
    static ArrayList<Weapon> ownedWeapons = new ArrayList<>();
    static JPanel editorPaint;

    // ---- shared constants ----
    static int HELP_DISCOUNT = 10000;
    static int AREA_COST = 2000;
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
        try {
            frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage("friendrun.png"));
        } catch (Exception ignored) {
        }
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
        buildEditor(frame);
        buildDevMode(frame);

        frame.add(rootCards);
        frame.setVisible(true);
        layoutCards.show(rootCards, "launch");
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
        startBtn.addActionListener(e -> startSingleGame());

        JButton weaponShopBtn = new JButton("Weapon Shop");
        weaponShopBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        weaponShopBtn.addActionListener(e -> openWeaponShop(frame));

        JButton makeLevelBtn = new JButton("Make Level");
        makeLevelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        makeLevelBtn.addActionListener(e -> {
            loadLevel();
            if (editorPaint != null) editorPaint.repaint();
            layoutCards.show(rootCards, "editor");
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

        JButton launcherBtn = new JButton("Launcher");
        launcherBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        launcherBtn.addActionListener(e -> layoutCards.show(rootCards, "launch"));

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
        center.add(launcherBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(weaponShopBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(makeLevelBtn);
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

        // ================= LAUNCH SCREEN =================
        JPanel launch = new JPanel(new BorderLayout());
        launch.setBackground(new Color(20, 30, 55));
        JPanel lc = new JPanel();
        lc.setOpaque(false);
        lc.setLayout(new BoxLayout(lc, BoxLayout.Y_AXIS));

        JLabel ltitle = new JLabel("FRIENDRUN");
        ltitle.setFont(new Font("Arial", Font.BOLD, 46));
        ltitle.setForeground(Color.WHITE);
        ltitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lsub = new JLabel("Choose a version of the game to launch:");
        lsub.setFont(new Font("Arial", Font.PLAIN, 16));
        lsub.setForeground(new Color(200, 210, 235));
        lsub.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] versions = {
            "Current - Single Player Adventure",
            "Current - Multiplayer vs Bots",
            "Current - Public Multiplayer",
            "Current - Leaderboard",
            "Current - Level Editor",
            "Current - Weapon Shop",
            "v1.0.7",
            "v1.0.6",
            "v1.0.5",
            "v1.0.4",
            "v1.0.3",
            "v1.0.2",
            "v1.0.1",
            "v1.0.0",
            "v0.1.0"
        };
        JComboBox<String> versionBox = new JComboBox<>(versions);
        versionBox.setFont(new Font("Arial", Font.PLAIN, 15));
        versionBox.setMaximumSize(new Dimension(340, 32));
        versionBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton launchBtn = new JButton("Launch");
        launchBtn.setFont(new Font("Arial", Font.BOLD, 16));
        launchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        launchBtn.addActionListener(e -> {
            int sel = versionBox.getSelectedIndex();
            switch (sel) {
                case 0:
                    startSingleGame();
                    break;
                case 1:
                    pickRival();
                    startMultiplayer(opponentName, opponentPoints);
                    break;
                case 2:
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
                    break;
                case 3:
                    refreshLeaderboard();
                    layoutCards.show(rootCards, "leaderboard");
                    break;
                case 4:
                    loadLevel();
                    if (editorPaint != null) editorPaint.repaint();
                    layoutCards.show(rootCards, "editor");
                    break;
                case 5:
                    openWeaponShop(frame);
                    break;
                case 6:
                    launchVersion(frame, "v1.0.7");
                    break;
                case 7:
                    launchVersion(frame, "v1.0.6");
                    break;
                case 8:
                    launchVersion(frame, "v1.0.5");
                    break;
                case 9:
                    launchVersion(frame, "v1.0.4");
                    break;
                case 10:
                    launchVersion(frame, "v1.0.3");
                    break;
                case 11:
                    launchVersion(frame, "v1.0.2");
                    break;
                case 12:
                    launchVersion(frame, "v1.0.1");
                    break;
                case 13:
                    launchVersion(frame, "v1.0.0");
                    break;
                case 14:
                    launchVersion(frame, "v0.1.0");
                    break;
                default:
                    break;
            }
        });

        JButton launchBack = new JButton("Back to Main Menu");
        launchBack.setAlignmentX(Component.CENTER_ALIGNMENT);
        launchBack.addActionListener(e -> layoutCards.show(rootCards, "start"));

        JButton docsBtn = new JButton("Documents");
        docsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        docsBtn.addActionListener(e -> layoutCards.show(rootCards, "docs"));

        lc.add(Box.createVerticalStrut(50));
        lc.add(ltitle);
        lc.add(Box.createVerticalStrut(12));
        lc.add(lsub);
        lc.add(Box.createVerticalStrut(28));
        lc.add(versionBox);
        lc.add(Box.createVerticalStrut(16));
        lc.add(launchBtn);
        lc.add(Box.createVerticalStrut(10));
        lc.add(docsBtn);
        lc.add(Box.createVerticalStrut(14));
        lc.add(launchBack);
        launch.add(lc, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel vipPanel = new JPanel(new BorderLayout());
        vipPanel.setOpaque(false);
        JLabel vipHeader = new JLabel("VIP MEMBERS");
        vipHeader.setForeground(new Color(255, 215, 0));
        vipHeader.setFont(new Font("Arial", Font.BOLD, 14));
        vipHeader.setBorder(BorderFactory.createEmptyBorder(0, 14, 4, 0));
        vipListPanel = new JPanel();
        vipListPanel.setOpaque(false);
        vipListPanel.setLayout(new BoxLayout(vipListPanel, BoxLayout.Y_AXIS));
        JScrollPane vipScroll = new JScrollPane(vipListPanel);
        vipScroll.setOpaque(false);
        vipScroll.setBorder(null);
        vipScroll.setPreferredSize(new Dimension(380, 110));
        vipScroll.setMaximumSize(new Dimension(380, 110));
        vipPanel.add(vipHeader, BorderLayout.NORTH);
        vipPanel.add(vipScroll, BorderLayout.CENTER);
        bottom.add(vipPanel, BorderLayout.CENTER);

        JButton devBtn = new JButton("Dev");
        devBtn.setToolTipText("Developer Mode");
        devBtn.setFont(new Font("Arial", Font.BOLD, 10));
        devBtn.setBackground(new Color(60, 60, 80));
        devBtn.setForeground(new Color(180, 180, 200));
        devBtn.setFocusPainted(false);
        devBtn.setBorderPainted(true);
        devBtn.addActionListener(e -> {
            String pw = JOptionPane.showInputDialog(frame, "Enter Developer Password:");
            if (pw == null) return;
            if (DEV_PASSWORD.equals(pw.trim())) {
                layoutCards.show(rootCards, "dev");
            } else {
                JOptionPane.showMessageDialog(frame, "Incorrect developer password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel devWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        devWrap.setOpaque(false);
        devWrap.add(devBtn);
        bottom.add(devWrap, BorderLayout.EAST);

        launch.add(bottom, BorderLayout.SOUTH);
        rootCards.add(launch, "launch");

        refreshVipList();
        buildDocuments(frame);
    }

    static void launchVersion(JFrame frame, String tag) {
        try {
            File dir = new File("versions/" + tag);
            new ProcessBuilder("java", "P.java")
                    .directory(dir)
                    .inheritIO()
                    .start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Could not launch " + tag + ":\n" + ex.getMessage());
        }
    }

    // ================= DOCUMENTS =================
    static void buildDocuments(JFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 246, 252));

        JEditorPane docs = new JEditorPane();
        docs.setContentType("text/html");
        docs.setEditable(false);
        docs.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        docs.setText(DOCS_HTML);

        JScrollPane sp = new JScrollPane(docs);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        p.add(sp, BorderLayout.CENTER);

        JButton back = new JButton("Back to Launcher");
        back.addActionListener(e -> layoutCards.show(rootCards, "launch"));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(new Color(210, 225, 240));
        bar.add(back);
        p.add(bar, BorderLayout.SOUTH);
        rootCards.add(p, "docs");
    }

    static final String DOCS_HTML =
        "<html><body bgcolor='#F0F6FC' text='#222222'>"
        + "<div style='background-color:#0F3A5F; padding:18px;'>"
        + "<h1 style='color:#FFFFFF; font-family:Arial, sans-serif; font-size:34px; margin:0;'>FRIENDRUN</h1>"
        + "<p style='color:#BBD4EE; font-family:Arial, sans-serif; font-size:15px; margin:4px 0 0 0;'>The Documents &mdash; everything you need to know about the game</p>"
        + "</div>"

        + "<h2 style='color:#0F3A5F; font-family:Arial, sans-serif; font-size:24px;'>About the Game</h2>"
        + "<p style='font-family:Georgia, serif; font-size:15px; line-height:1.5;'>"
        + "FriendRun is a platformer and money-collecting game written in a single file of Java. You run, jump, buy, "
        + "and fight your way to a better score. It started as nothing and grew into the game you can launch today &mdash; "
        + "with monsters, weapons, bosses, treasure chests, levels, a level editor, and even multiplayer."
        + "</p>"

        + "<h2 style='color:#0F3A5F; font-family:Arial, sans-serif; font-size:24px;'>Author</h2>"
        + "<p style='font-family:Georgia, serif; font-size:15px; line-height:1.5;'>"
        + "Created by <b>Raymond. K.</b> &mdash; written entirely from scratch, one piece at a time."
        + "</p>"

        + "<h2 style='color:#0F3A5F; font-family:Arial, sans-serif; font-size:24px;'>How the Game Was Made</h2>"
        + "<p style='font-family:Georgia, serif; font-size:15px; line-height:1.5;'>"
        + "\u201CThis game was made not with intent &mdash; it used to be practice until my brother said it was addicting.\u201D"
        + "<br><br>"
        + "What began as practice code became a real game when people started playing it. Every version below is still "
        + "launchable from the launcher, so you can watch it grow from the very beginning to today."
        + "</p>"

        + "<h2 style='color:#0F3A5F; font-family:Arial, sans-serif; font-size:24px;'>Versions &amp; How to Play</h2>"

        + "<table width='100%' cellspacing='0' cellpadding='8' style='font-family:Arial, sans-serif; font-size:14px;'>"
        + "<tr bgcolor='#0F3A5F'><td><b style='color:#FFFFFF;'>Version</b></td><td><b style='color:#FFFFFF;'>What It Is</b></td><td><b style='color:#FFFFFF;'>How to Play</b></td></tr>"
        + "<tr bgcolor='#FFFFFF'><td><b>v0.1.0</b></td><td>The very first build. The earliest FriendRun ever made.</td><td>Run with the arrow keys / A and D, jump with W or Space, and grab money.</td></tr>"
        + "<tr bgcolor='#E8F1F9'><td><b>v1.0.0 &ndash; v1.0.4</b></td><td>The classic money-walker. Walk, jump, and buy your way to fortune.</td><td>Move with A/D, jump with W/Space, buy steps and multipliers in the shop, own the boost areas, and fight friends.</td></tr>"
        + "<tr bgcolor='#FFFFFF'><td><b>v1.0.5</b></td><td>Added public multiplayer to the classic game.</td><td>Same as the classic, plus Public Multiplayer to battle strangers on a server.</td></tr>"
        + "<tr bgcolor='#E8F1F9'><td><b>v1.0.6 &ndash; v1.0.7</b></td><td>Combat arrives. Monsters, weapons, and bosses join the world.</td><td>Fight with F or by clicking, dodge projectiles, spend points on weapons, and beat bosses every 25 kills.</td></tr>"
        + "<tr bgcolor='#FFFFFF'><td><b>v1.0.8 (Current)</b></td><td>The full game. 107 weapons, levels, chests, a level editor, and a launcher.</td><td>A/D move, Space/W jump, F attack, click to attack or open chests, 1-9 or Q/E/N to switch weapons. Kill monsters for points and level up &mdash; every level makes you tougher and stronger.</td></tr>"
        + "</table>"

        + "<h2 style='color:#0F3A5F; font-family:Arial, sans-serif; font-size:24px;'>Controls Cheat Sheet</h2>"
        + "<p style='font-family:Georgia, serif; font-size:15px; line-height:1.7;'>"
        + "<b>A / D</b> &mdash; move left / right<br>"
        + "<b>Space / W</b> &mdash; jump<br>"
        + "<b>F</b> &mdash; attack<br>"
        + "<b>Mouse click</b> &mdash; attack toward the cursor, or open a chest<br>"
        + "<b>1-9</b> &mdash; select a weapon<br>"
        + "<b>Q / E / N</b> &mdash; cycle weapons"
        + "</p>"

        + "<div style='background-color:#0F3A5F; padding:10px;'>"
        + "<p style='color:#FFFFFF; font-family:Arial, sans-serif; font-size:13px; margin:0;'>&copy; Raymond. K. &mdash; thanks for playing FriendRun!</p>"
        + "</div>"
        + "</body></html>";

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

                for (Block b : blocks) drawBlock(g2d, b);

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

                for (Chest c : chests) c.draw(g2d);

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
                if (gameOver) return;
                for (Chest c : chests) {
                    if (c.contains(e.getX(), e.getY())) {
                        openChest(c);
                        return;
                    }
                }
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

        JButton menuBtn = new JButton("Back to Menu");
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
        im.put(KeyStroke.getKeyStroke('N'), "nextWeapon");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "nextWeapon");

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
                    velY = JUMP_VELOCITY;
                    onGround = false;
                    jumpsLeft--;
                    steps += 2;
                }
            }
        });

        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(-PLAYER_SPEED, paint);
            }
        });

        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                move(PLAYER_SPEED, paint);
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
            velY += GRAVITY;
            circleY += (int) velY;
            boolean onBlock = false;
            if (velY >= 0) {
                for (Block b : blocks) {
                    if (horizOverlap(b) && circleY + playerHeight() >= b.y && circleY + playerHeight() <= b.y + (int) velY + 2) {
                        circleY = b.y - playerHeight();
                        velY = 0;
                        onBlock = true;
                        break;
                    }
                }
            } else {
                for (Block b : blocks) {
                    if (horizOverlap(b) && circleY <= b.y + b.h && circleY >= b.y + b.h + (int) velY - 2) {
                        circleY = b.y + b.h;
                        velY = 0;
                        break;
                    }
                }
            }
            int gY = paint.getHeight() - 185;
            if (circleY >= gY) {
                circleY = gY;
                velY = 0;
                onGround = true;
            } else if (onBlock) {
                onGround = true;
            }
            circleX = clamp(circleX, 0, paint.getWidth() - playerWidth());

            // ---- combat update ----
            long now = System.currentTimeMillis();
            if (!gameOver) {
                if (!bossAlive && kills >= BOSS_WAVE_KILLS * (bossWaves + 1)) {
                    bossAlive = true;
                    bossWaves++;
                    Boss b = new Boss(paint.getWidth() / 2);
                    b.y = gY;
                    b.onGround = true;
                    enemies.add(b);
                    bossMessageUntil = now + 2500;
                }
                if (!bossAlive && enemies.size() < 6 && now - lastSpawnMs > ENEMY_SPAWN_INTERVAL) {
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
        circleX = SPAWN_X;
        circleY = SPAWN_Y;
        steps = (isVip() || hasPerk("Infinite Steps")) ? Integer.MAX_VALUE : START_STEPS;
        money = START_MONEY;
        stepCost = 10;
        maxMoneyCost = 25;
        multiplierCost = 25;
        moneyMultiplier = (isVip() || hasPerk("Double Money")) ? Integer.MAX_VALUE - 10 : DEFAULT_MONEY_MULTIPLIER;
        maxMoneyCap = isVip() ? Integer.MAX_VALUE : DEFAULT_MAX_MONEY_CAP;
        velY = 0;
        onGround = false;
        jumpsLeft = 50;
        areaBought = new boolean[areaX.length];
        playerLevel = 1;
        levelKills = 0;
        totalKills = 0;
        playerMaxHealth = PLAYER_MAX_HEALTH;
        playerHealth = playerMaxHealth;
        kills = 0;
        currentWeapon = equippedWeapon;
        facing = 1;
        lastAttackMs = 0;
        swingEffectUntil = 0;
        enemies.clear();
        projectiles.clear();
        chests.clear();
        lastSpawnMs = 0;
        bossAlive = false;
        bossWaves = 0;
        bossMessageUntil = 0;
        gameOver = false;
    }

    private static void move(int dx, JPanel panel) {
        if (steps <= 0) return;

        circleX = clamp(circleX + dx, 0, panel.getWidth() - playerWidth());
        if (dx != 0) facing = dx > 0 ? 1 : -1;

        for (Block b : blocks) {
            if (!vertOverlap(b)) continue;
            if (dx > 0 && circleX + playerWidth() > b.x && circleX + playerWidth() < b.x + b.w + Math.abs(dx) + 2) {
                circleX = b.x - playerWidth();
                break;
            }
            if (dx < 0 && circleX < b.x + b.w && circleX > b.x - Math.abs(dx) - 2) {
                circleX = b.x + b.w;
                break;
            }
        }

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

        JButton endBtn = new JButton("Back to Menu");
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
            local.x = clamp(local.x, 0, multiPaint.getWidth() - playerWidth());
            for (PlayerState b : bots) {
                applyGravity(b, multiPaint.getHeight());
                b.x = clamp(b.x, 0, multiPaint.getWidth() - playerWidth());
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

        JButton leaveBtn = new JButton("Back to Menu");
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
            local.x = clamp(local.x, 0, netPaint.getWidth() - playerWidth());

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
            String sym = "";
            String ue = userEmblems.get(e[0].toLowerCase());
            if (ue != null && emblems.containsKey(ue)) sym = emblems.get(ue) + " ";
            sb.append(String.format("%-5d %s%-20s %d%n", rank, sym, e[0], Integer.parseInt(e[1])));
            rank++;
        }
        leaderboardArea.setText(sb.toString());
    }

    // ================= DRAWING HELPERS =================
    static void drawBackground(Graphics2D g2d, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, SKY_TOP, 0, h, SKY_BOTTOM);
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

        g2d.setColor(GROUND_TOP);
        g2d.fillRect(0, h - 100, w, 100);
        g2d.setColor(GROUND_STRIPE);
        g2d.fillRect(0, h - 12, w, 12);
    }

    static void drawCharacter(Graphics2D g2d, int x, int y, Color shirtColor) {
        java.awt.geom.AffineTransform old = g2d.getTransform();
        if (playerScale != 1.0) {
            g2d.translate(x, y);
            g2d.scale(playerScale, playerScale);
            g2d.translate(-x, -y);
        }
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
        g2d.setTransform(old);
    }

    // ================= GAME LOGIC HELPERS =================
    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    static boolean inArea(int i) {
        return circleX + playerWidth() > areaX[i] && circleX < areaX[i] + areaWidth;
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
            if (x + playerWidth() > areaX[i] && x < areaX[i] + areaWidth) {
                return true;
            }
        }
        return false;
    }

    // ================= PLAYER STATE & MULTIPLAYER HELPERS =================

    static void applyGravity(PlayerState ps, int panelHeight) {
        ps.velY += GRAVITY;
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
        ps.x = clamp(ps.x + dx, 0, panelWidth - playerWidth());
        ps.steps--;
        if (ps.money < ps.maxMoneyCap) {
            int gain = ps.moneyMultiplier * (ownsZone(ps, ps.x) ? 2 : 1);
            ps.money = Math.min(ps.maxMoneyCap, ps.money + gain);
        }
    }

    static void jumpPlayer(PlayerState ps) {
        if (ps.onGround && ps.jumpsLeft > 0) {
            ps.velY = JUMP_VELOCITY;
            ps.onGround = false;
            ps.jumpsLeft--;
            ps.steps += 2;
        }
    }

    static boolean inZoneAt(int x, int i) {
        return x + playerWidth() > areaX[i] && x < areaX[i] + areaWidth;
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

    static Weapon mk(String name, int dmg, int range, boolean ranged, long cd, int price, int proj, String desc, int style) {
        Weapon nw = new Weapon(name, dmg, range, ranged, cd, price, proj, desc);
        nw.style = style;
        return nw;
    }

    static Weapon defaultWeapon() {
        return ALL_WEAPONS[0].copy();
    }

    static final Weapon[] ALL_WEAPONS = {
        // ----- originals -----
        mk("Sword", 35, 85, false, 500, 0, 0, "A balanced starting blade. 35 dmg.", 0),
        mk("Spear", 45, 130, false, 550, 80, 0, "Long reach melee. 45 dmg.", 0),
        mk("Axe", 60, 85, false, 750, 50, 0, "Heavy melee. 60 dmg.", 0),
        mk("Hammer", 95, 95, false, 1200, 200, 0, "Massive slow smash. 95 dmg.", 0),
        mk("Bow", 30, 650, true, 450, 60, 12, "Quick long-range arrows. 30 dmg.", 0),
        mk("Crossbow", 50, 700, true, 900, 150, 14, "Powerful ranged bolts. 50 dmg.", 1),
        mk("Magic Staff", 80, 550, true, 1100, 300, 9, "Slow magic bolts. 80 dmg.", 2),
        // ----- blades -----
        mk("Dagger", 20, 70, false, 350, 30, 0, "Rapid stabs. 20 dmg.", 0),
        mk("Dirk", 26, 70, false, 400, 35, 0, "A quick little blade. 26 dmg.", 0),
        mk("Stiletto", 24, 75, false, 380, 30, 0, "Sneaky and fast. 24 dmg.", 0),
        mk("Tanto", 28, 68, false, 420, 40, 0, "A nimble dagger. 28 dmg.", 0),
        mk("Katar", 30, 70, false, 450, 55, 0, "Punching blade. 30 dmg.", 0),
        mk("Sai", 34, 75, false, 480, 60, 0, "Deflecting trident-dagger. 34 dmg.", 0),
        mk("Kukri", 40, 75, false, 550, 90, 0, "Curved and deadly. 40 dmg.", 0),
        mk("Machete", 44, 80, false, 580, 100, 0, "Chopping workhorse. 44 dmg.", 0),
        mk("Kris", 45, 85, false, 620, 105, 0, "Wavy flame blade. 45 dmg.", 0),
        mk("Jian", 47, 90, false, 640, 110, 0, "The sword of scholars. 47 dmg.", 0),
        mk("Dao", 49, 85, false, 660, 115, 0, "A broad curved blade. 49 dmg.", 0),
        mk("Katana", 50, 90, false, 650, 120, 0, "Samurai steel. 50 dmg.", 0),
        mk("Cutlass", 52, 90, false, 700, 140, 0, "Pirate's choice. 52 dmg.", 0),
        mk("Chokuto", 55, 92, false, 700, 135, 0, "Straight single-edge. 55 dmg.", 0),
        mk("Wakisashi", 43, 80, false, 560, 95, 0, "Short companion blade. 43 dmg.", 0),
        mk("Rapier", 42, 100, false, 600, 110, 0, "Precise thrusts. 42 dmg.", 0),
        mk("Scimitar", 48, 90, false, 620, 115, 0, "Swift curve. 48 dmg.", 0),
        mk("Saber", 60, 95, false, 780, 170, 0, "Cavalry blade. 60 dmg.", 0),
        mk("Cleaver", 58, 85, false, 750, 150, 0, "Butcher's favorite. 58 dmg.", 0),
        mk("Falchion", 72, 95, false, 900, 185, 0, "Single-edged brute. 72 dmg.", 0),
        mk("Bastard Sword", 68, 100, false, 850, 175, 0, "Hand-and-a-half sword. 68 dmg.", 0),
        mk("Estoc", 66, 115, false, 820, 180, 0, "Armor-piercing point. 66 dmg.", 0),
        mk("Claymore", 90, 110, false, 1150, 250, 0, "Scottish greatsword. 90 dmg.", 0),
        mk("Zweihander", 110, 120, false, 1400, 330, 0, "Two-handed monster. 110 dmg.", 0),
        mk("Tonfa", 36, 80, false, 500, 70, 0, "Fast blunt sticks. 36 dmg.", 0),
        mk("Nunchaku", 33, 85, false, 450, 65, 0, "Whirlwind of strikes. 33 dmg.", 0),
        mk("Assassin's Blade", 46, 72, false, 380, 108, 0, "Silent and cruel. 46 dmg.", 0),
        // ----- axes & hammers -----
        mk("Hatchet", 38, 75, false, 480, 55, 0, "Small but sturdy. 38 dmg.", 0),
        mk("Battle Axe", 70, 95, false, 900, 180, 0, "Classic war axe. 70 dmg.", 0),
        mk("War Axe", 78, 95, false, 950, 200, 0, "Cleaves armor. 78 dmg.", 0),
        mk("Double Axe", 85, 90, false, 1000, 220, 0, "Two blades, no mercy. 85 dmg.", 0),
        mk("Greataxe", 95, 100, false, 1100, 260, 0, "A wall of iron. 95 dmg.", 0),
        mk("Doom Axe", 135, 100, false, 1400, 350, 0, "Whispers of ruin. 135 dmg.", 0),
        mk("War Hammer", 105, 100, false, 1250, 280, 0, "Crushes shields. 105 dmg.", 0),
        mk("Sledgehammer", 115, 105, false, 1350, 310, 0, "Demolition tool. 115 dmg.", 0),
        mk("Maul", 120, 100, false, 1400, 330, 0, "Overwhelming weight. 120 dmg.", 0),
        mk("Great Hammer", 130, 110, false, 1500, 360, 0, "The ground shakes. 130 dmg.", 0),
        mk("Morning Star", 88, 90, false, 1000, 230, 0, "Spiked ball of pain. 88 dmg.", 0),
        mk("Flail", 92, 95, false, 1050, 240, 0, "Unpredictable swing. 92 dmg.", 0),
        // ----- polearms -----
        mk("Glaive", 55, 140, false, 700, 140, 0, "Sweeping reach. 55 dmg.", 0),
        mk("Halberd", 62, 150, false, 800, 160, 0, "Axe on a pole. 62 dmg.", 0),
        mk("Trident", 58, 145, false, 750, 150, 0, "Neptune's fork. 58 dmg.", 0),
        mk("Pike", 52, 170, false, 780, 145, 0, "Poke from afar. 52 dmg.", 0),
        mk("Naginata", 60, 155, false, 820, 165, 0, "Curved polearm. 60 dmg.", 0),
        mk("Blessed Spear", 90, 150, false, 950, 230, 0, "A holy point. 90 dmg.", 0),
        mk("Kusarigama", 55, 160, false, 800, 170, 0, "Sickle and chain. 55 dmg.", 0),
        mk("Whip", 40, 180, false, 550, 130, 0, "Crack! 40 dmg.", 0),
        mk("War Scythe", 84, 160, false, 1050, 225, 0, "Reaps a grim harvest. 84 dmg.", 0),
        // ----- thrown -----
        mk("Dart", 22, 300, true, 300, 16, 45, "Pinprick from afar. 22 dmg.", 0),
        mk("Kunai", 35, 300, true, 400, 12, 80, "Ninja's friend. 35 dmg.", 0),
        mk("Shuriken", 30, 280, true, 350, 14, 75, "Spinning stars. 30 dmg.", 0),
        mk("Sling", 34, 340, true, 400, 13, 70, "Old reliable. 34 dmg.", 0),
        mk("Throwing Axe", 55, 380, true, 750, 11, 140, "Hurl and split. 55 dmg.", 0),
        mk("Throwing Knives", 34, 320, true, 320, 15, 85, "A fistful of steel. 34 dmg.", 0),
        mk("Chakram", 42, 350, true, 550, 15, 110, "Rings of death. 42 dmg.", 0),
        mk("Boomerang", 38, 500, true, 900, 16, 125, "It comes back... mostly. 38 dmg.", 0),
        mk("Javelin", 50, 420, true, 700, 13, 130, "Olympic armor shot. 50 dmg.", 0),
        mk("Pilum", 46, 400, true, 650, 12, 120, "Roman javelin. 46 dmg.", 0),
        mk("Atlatl", 60, 560, true, 900, 13, 185, "Spear launcher. 60 dmg.", 0),
        mk("Spear Thrower", 64, 580, true, 950, 13, 195, "Extra hurl. 64 dmg.", 0),
        // ----- bows -----
        mk("Shortbow", 40, 500, true, 600, 12, 110, "Starter archer bow. 40 dmg.", 0),
        mk("Recurve Bow", 48, 620, true, 700, 12, 150, "Curved for power. 48 dmg.", 0),
        mk("Longbow", 52, 650, true, 800, 13, 160, "English classic. 52 dmg.", 0),
        mk("Compound Bow", 66, 680, true, 900, 13, 200, "Modern mechanics. 66 dmg.", 0),
        mk("Warbow", 74, 700, true, 950, 13, 230, "Battle-ready draw. 74 dmg.", 0),
        mk("Greatbow", 88, 750, true, 1100, 12, 280, "Tower of arrows. 88 dmg.", 0),
        mk("Sniper Bow", 120, 900, true, 1500, 16, 400, "Aim far, hit hard. 120 dmg.", 0),
        mk("Phoenix Bow", 130, 720, true, 1400, 14, 420, "Feathers ablaze. 130 dmg.", 0),
        mk("Infinity Bow", 260, 1000, true, 2000, 17, 750, "Endless arrows. 260 dmg.", 0),
        // ----- crossbows -----
        mk("Pistol Crossbow", 55, 400, true, 600, 14, 160, "One-handed bolt. 55 dmg.", 1),
        mk("Hand Crossbow", 60, 450, true, 700, 14, 180, "Reliable sidearm. 60 dmg.", 1),
        mk("Repeating Crossbow", 72, 480, true, 650, 14, 220, "Chu-ko-nu fury. 72 dmg.", 1),
        mk("Heavy Crossbow", 95, 550, true, 1100, 14, 290, "Crank that bolt. 95 dmg.", 1),
        mk("Arbalest", 110, 600, true, 1300, 15, 340, "Siege-grade bolt. 110 dmg.", 1),
        mk("Ballista Bow", 130, 650, true, 1500, 15, 390, "Almost artillery. 130 dmg.", 1),
        mk("Eclipse Crossbow", 150, 700, true, 1500, 15, 470, "Darkened bolts. 150 dmg.", 1),
        // ----- magic -----
        mk("Wand", 55, 480, true, 700, 13, 170, "Beginner's magic. 55 dmg.", 2),
        mk("Staff", 65, 520, true, 850, 12, 190, "Reliable focus. 65 dmg.", 2),
        mk("Fire Wand", 75, 500, true, 900, 12, 210, "Burns bright. 75 dmg.", 2),
        mk("Frost Wand", 80, 520, true, 950, 11, 225, "Chills the air. 80 dmg.", 2),
        mk("Crystal Wand", 90, 540, true, 980, 12, 240, "Refracts pure light. 90 dmg.", 2),
        mk("Lightning Rod", 95, 540, true, 1000, 13, 260, "Crackling power. 95 dmg.", 2),
        mk("Flame Staff", 110, 560, true, 1150, 11, 300, "A blaze of glory. 110 dmg.", 2),
        mk("Ice Staff", 120, 580, true, 1200, 10, 330, "Frozen verdict. 120 dmg.", 2),
        mk("Shadow Wand", 130, 620, true, 1350, 11, 360, "Dark whispers. 130 dmg.", 2),
        mk("Storm Staff", 140, 600, true, 1300, 12, 380, "Tempest in hand. 140 dmg.", 2),
        mk("Archmage Staff", 165, 640, true, 1400, 12, 490, "Mastery incarnate. 165 dmg.", 2),
        // ----- legendary -----
        mk("Holy Mace", 100, 90, false, 1000, 250, 0, "Blessed bashing. 100 dmg.", 0),
        mk("Frostfang", 145, 105, false, 1450, 400, 0, "Icy edge. 145 dmg.", 0),
        mk("Emberbrand", 140, 108, false, 1420, 390, 0, "Burning brand. 140 dmg.", 0),
        mk("Thunderbringer", 150, 110, false, 1500, 430, 0, "Storms in steel. 150 dmg.", 0),
        mk("Chaos Blade", 125, 105, false, 1300, 340, 0, "Unstable fury. 125 dmg.", 0),
        mk("Dragon Tooth", 160, 110, false, 1600, 450, 0, "A dragon's fang. 160 dmg.", 0),
        mk("Soulrender", 170, 115, false, 1650, 480, 0, "Tears the soul. 170 dmg.", 0),
        mk("Void Edge", 190, 120, false, 1750, 520, 0, "Reality's cut. 190 dmg.", 0),
        mk("Titan's Maul", 210, 115, false, 2100, 580, 0, "Shatters the earth. 210 dmg.", 0),
        mk("Dragon Slayer", 200, 120, false, 1800, 550, 0, "The slayer of legends. 200 dmg.", 0),
        mk("Celestial Blade", 220, 135, false, 1900, 600, 0, "Star-forged. 220 dmg.", 0),
        mk("Excalibur", 250, 130, false, 2000, 700, 0, "The king's blade. 250 dmg.", 0),
        mk("Godslayer", 300, 150, false, 2200, 800, 0, "Even gods fear it. 300 dmg.", 0),
    };

    static Weapon weaponByName(String name, int bonus) {
        for (Weapon w : ALL_WEAPONS) {
            if (w.name.equalsIgnoreCase(name)) {
                Weapon nw = w.copy();
                nw.bonus = bonus;
                return nw;
            }
        }
        return null;
    }

    static int requiredKills(int level) {
        return (int) Math.ceil(5 * Math.pow(1.5, level - 1));
    }

    static double attackMultiplier() {
        return 1.0 + 0.15 * (playerLevel - 1);
    }

    static int attackDamage() {
        return currentWeapon == null ? 0 : (int) Math.round(currentWeapon.damageValue() * attackMultiplier());
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
            projectiles.add(new Projectile(sx, sy, facing * currentWeapon.projSpeed, 0, attackDamage(), false, projectileColor()));
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
                if (ddx * ddx + ddy * ddy < reach * reach) e.hurt(attackDamage());
            }
        }
    }

    static Color projectileColor() {
        if (currentWeapon == null) return new Color(120, 80, 220);
        if (currentWeapon.style == 1) return new Color(90, 70, 55);
        if (currentWeapon.style == 2) return new Color(120, 80, 220);
        return new Color(150, 100, 60);
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
            projectiles.add(new Projectile(circleX + 22 + facing * 24, circleY + 32, vx, vy, attackDamage(), false, projectileColor()));
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
                if (ddx * ddx + ddy * ddy < reach * reach) e.hurt(attackDamage());
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

    // ================= CHESTS =================

    static void openChest(Chest c) {
        chests.remove(c);
        int level = c.level;
        int ri = 1 + (int) (Math.random() * (ALL_WEAPONS.length - 1));
        Weapon base = ALL_WEAPONS[ri];
        Weapon w = base.copy();
        w.bonus = (level - 1) * 12;
        boolean replaced = false;
        for (int i = 0; i < ownedWeapons.size(); i++) {
            Weapon ow = ownedWeapons.get(i);
            if (ow.name.equalsIgnoreCase(w.name)) {
                if (ow.bonus < w.bonus) ownedWeapons.set(i, w);
                replaced = true;
                break;
            }
        }
        if (!replaced) ownedWeapons.add(w);
        currentWeapon = w;
        equippedWeapon = w;
        lastAttackMs = 0;
        saveData();
        JOptionPane.showMessageDialog(paint, "Chest (Lv " + level + ") opened!\nGot " + w.name + " (+" + w.bonus + " bonus dmg, total " + w.damageValue() + ")!");
        paint.repaint();
    }

    // ================= LEVEL BLOCKS =================

    static void drawBlock(Graphics2D g2d, Block b) {
        g2d.setColor(new Color(139, 90, 43));
        g2d.fillRect(b.x, b.y, b.w, b.h);
        g2d.setColor(new Color(90, 160, 60));
        g2d.fillRect(b.x, b.y, b.w, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(b.x, b.y, b.w, b.h);
    }

    static boolean horizOverlap(Block b) {
        return circleX + playerWidth() > b.x && circleX < b.x + b.w;
    }

    static boolean vertOverlap(Block b) {
        return circleY + playerHeight() > b.y && circleY < b.y + b.h;
    }

    // ================= LEVEL EDITOR =================
    static void buildEditor(JFrame frame) {
        editorPaint = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawBackground(g2d, getWidth(), getHeight());
                g2d.setColor(new Color(0, 0, 0, 50));
                for (int x = 0; x <= getWidth(); x += 40) g2d.drawLine(x, 0, x, getHeight());
                for (int y = 0; y <= getHeight(); y += 40) g2d.drawLine(0, y, getWidth(), y);
                for (Block b : blocks) drawBlock(g2d, b);
            }
        };
        editorPaint.setPreferredSize(new Dimension(800, 560));
        editorPaint.setBackground(Color.WHITE);
        editorPaint.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int gx = e.getX() / 40 * 40, gy = e.getY() / 40 * 40;
                boolean found = false;
                for (java.util.Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
                    Block b = it.next();
                    if (b.at(gx, gy)) {
                        it.remove();
                        found = true;
                        break;
                    }
                }
                if (!found) blocks.add(new Block(gx, gy, 40, 40));
                editorPaint.repaint();
            }
        });

        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> {
            blocks.clear();
            editorPaint.repaint();
        });
        JButton saveBtn = new JButton("Save Level");
        saveBtn.addActionListener(e -> {
            saveLevel();
            JOptionPane.showMessageDialog(frame, "Level saved!");
        });
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> layoutCards.show(rootCards, "start"));

        JPanel bar = new JPanel(new FlowLayout());
        bar.add(new JLabel("Click to place/remove blocks (40px grid)"));
        bar.add(clearBtn);
        bar.add(saveBtn);
        bar.add(backBtn);
        JPanel p = new JPanel(new BorderLayout());
        p.add(editorPaint, BorderLayout.CENTER);
        p.add(bar, BorderLayout.SOUTH);
        rootCards.add(p, "editor");
    }

    static void saveLevel() {
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_level.txt"));
            for (Block b : blocks) w.println(b.x + "|" + b.y);
            w.close();
        } catch (Exception ignored) {
        }
    }

    static void loadLevel() {
        blocks.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_level.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length == 2) {
                    int bx = Integer.parseInt(p[0].trim()), by = Integer.parseInt(p[1].trim());
                    blocks.add(new Block(bx, by, 40, 40));
                }
            }
            br.close();
        } catch (Exception ignored) {
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
        g2d.fillRoundRect(bx, by, (int) (bw * Math.max(0, playerHealth) / (double) playerMaxHealth), bh, 4, 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("HP " + Math.max(0, playerHealth) + "/" + playerMaxHealth, bx + 5, by + 13);

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
            boolean eq = currentWeapon != null && currentWeapon.name.equals(w.name);
            g2d.setColor(eq ? new Color(0, 120, 220) : Color.BLACK);
            g2d.drawString((i + 1) + ": " + w.name + (eq ? "  <--" : ""), 20, wy);
            wy += 18;
        }
        g2d.setColor(Color.BLACK);
        g2d.drawString("Level " + playerLevel + " | Next lvl in " + (requiredKills(playerLevel) - levelKills) + " kills | Atk x" + String.format("%.2f", attackMultiplier()), 20, wy + 4);
        g2d.drawString("Kills: " + kills + " | Boss in " + (BOSS_WAVE_KILLS * (bossWaves + 1) - kills) + " | Chest in " + (CHEST_EVERY - totalKills % CHEST_EVERY) + " | Q/E to switch", 20, wy + 22);
        if (steps <= 0) {
            g2d.setColor(Color.RED);
            g2d.drawString("OUT OF STEPS! Buy more below.", 20, wy + 44);
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
            boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.name.equalsIgnoreCase(w.name));
            if (owned) {
                JOptionPane.showMessageDialog(frame, "You already own the " + w.name + "!");
                return;
            }
            if (playerPoints < w.price) {
                JOptionPane.showMessageDialog(frame, "Not enough points! Need " + w.price + " (get points by killing enemies and bosses).");
                return;
            }
            playerPoints -= w.price;
            Weapon ownedW = w.copy();
            ownedWeapons.add(ownedW);
            equippedWeapon = ownedW;
            currentWeapon = ownedW;
            setPoints(playerName, playerPoints);
            saveData();
            weaponShopPoints.setText("Points: " + playerPoints);
            if (startPointsLabel != null) startPointsLabel.setText("Points: " + playerPoints);
            updateWeaponShopInfo();
            JOptionPane.showMessageDialog(frame, "Bought " + w.name + "! Damage: " + w.damageValue());
        });
        equipBtn.addActionListener(e -> {
            int idx = weaponShopList.getSelectedIndex();
            if (idx < 0) return;
            Weapon w = ALL_WEAPONS[idx];
            boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.name.equalsIgnoreCase(w.name));
            if (!owned) {
                JOptionPane.showMessageDialog(frame, "Buy the " + w.name + " first!");
                return;
            }
            equippedWeapon = w.copy();
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
        boolean owned = ownedWeapons.stream().anyMatch(ow -> ow.name.equalsIgnoreCase(w.name));
        StringBuilder sb = new StringBuilder();
        sb.append(w.name).append("  |  ").append(w.ranged ? "Long-Ranged" : "Melee").append("\n");
        sb.append("Damage: ").append(w.damageValue()).append("\n");
        sb.append(w.ranged ? "Range: " : "Reach: ").append(w.range).append("px\n");
        sb.append("Cooldown: ").append(w.cooldownMs).append("ms\n");
        sb.append("Price: ").append(w.price).append(" points\n\n");
        sb.append(w.desc).append("\n");
        if (owned) {
            sb.append(equippedWeapon != null && equippedWeapon.name.equalsIgnoreCase(w.name) ? "[OWNED] [EQUIPPED]" : "[OWNED]");
        } else {
            sb.append("[NOT OWNED]");
        }
        weaponShopInfo.setText(sb.toString());
    }

    // ================= PERSISTENCE =================
    static void loadData() {
        loadDevData();
        leaderboard.clear();
        friends.clear();
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
                    String[] wp = line.split("\\|");
                    String nm = wp[0].trim();
                    int bonus = wp.length > 1 ? Integer.parseInt(wp[1].trim()) : 0;
                    Weapon w = weaponByName(nm, bonus);
                    if (w != null && !ownedWeapons.stream().anyMatch(ow -> ow.name.equalsIgnoreCase(w.name))) {
                        ownedWeapons.add(w);
                    }
                }
            }
            br.close();
            if (ownedWeapons.isEmpty()) ownedWeapons.add(defaultWeapon());
            String[] ep = (first == null ? "Sword|0" : first).split("\\|");
            int ebonus = ep.length > 1 ? Integer.parseInt(ep[1].trim()) : 0;
            Weapon eq = weaponByName(ep[0].trim(), ebonus);
            equippedWeapon = eq != null ? eq : ownedWeapons.get(0);
            currentWeapon = equippedWeapon;
        } catch (Exception ignored) {
            if (ownedWeapons.isEmpty()) ownedWeapons.add(defaultWeapon());
            equippedWeapon = ownedWeapons.get(0);
            currentWeapon = equippedWeapon;
        }
        loadLevel();
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
            w.println(equippedWeapon != null ? equippedWeapon.name + "|" + equippedWeapon.bonus : "Sword|0");
            for (Weapon ow : ownedWeapons) {
                w.println(ow.name + "|" + ow.bonus);
            }
            w.close();
        } catch (Exception ignored) {
        }
        saveDevData();
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
        if (name == null) return 0;
        for (String[] e : leaderboard) {
            if (e[0].equalsIgnoreCase(name)) {
                return Integer.parseInt(e[1]);
            }
        }
        return 0;
    }

    // ================= DEVELOPER MODE =================
    static int parseInt(String s, int dflt) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return dflt;
        }
    }

    static double parseDouble(String s, double dflt) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return dflt;
        }
    }

    static Color parseColor(String s, Color dflt) {
        try {
            String[] p = s.split(",");
            return new Color(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
        } catch (Exception e) {
            return dflt;
        }
    }

    static java.util.List<String> parsePerks(String s) {
        java.util.List<String> out = new ArrayList<>();
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    static void loadDevData() {
        vipUsers.clear();
        emblems.clear();
        userEmblems.clear();
        vipProfiles.clear();
        playerRecords.clear();
        vipUsers.put("th3greatplayer", new ArrayList<>(Arrays.asList("Infinite Steps", "Double Money")));
        emblems.put("Gold", "★");
        userEmblems.put("th3greatplayer", "Gold");
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_vips.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                java.util.List<String> perks = new ArrayList<>();
                if (p.length > 1 && !p[1].trim().isEmpty()) perks.addAll(parsePerks(p[1]));
                vipUsers.put(p[0].trim().toLowerCase(), perks);
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_vip_profiles.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length == 2) vipProfiles.put(p[0].trim(), p[1].trim());
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_emblems.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length == 2) emblems.put(p[0].trim(), p[1].trim());
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_user_emblems.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length == 2) userEmblems.put(p[0].trim().toLowerCase(), p[1].trim());
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_player_records.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 14) {
                    playerRecords.put(p[0].trim().toLowerCase(), Arrays.copyOf(p, 14));
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader("friendrun_settings.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] kv = line.split("=", 2);
                if (kv.length != 2) continue;
                String k = kv[0].trim();
                String v = kv[1].trim();
                switch (k) {
                    case "gravity": GRAVITY = parseDouble(v, GRAVITY); break;
                    case "jump": JUMP_VELOCITY = parseInt(v, JUMP_VELOCITY); break;
                    case "speed": PLAYER_SPEED = parseInt(v, PLAYER_SPEED); break;
                    case "scale": playerScale = parseDouble(v, playerScale); break;
                    case "skyTop": SKY_TOP = parseColor(v, SKY_TOP); break;
                    case "skyBottom": SKY_BOTTOM = parseColor(v, SKY_BOTTOM); break;
                    case "groundTop": GROUND_TOP = parseColor(v, GROUND_TOP); break;
                    case "groundStripe": GROUND_STRIPE = parseColor(v, GROUND_STRIPE); break;
                    case "maxHealth": PLAYER_MAX_HEALTH = parseInt(v, PLAYER_MAX_HEALTH); break;
                    case "startMoney": START_MONEY = parseInt(v, START_MONEY); break;
                    case "startSteps": START_STEPS = parseInt(v, START_STEPS); break;
                    case "areaCost": AREA_COST = parseInt(v, AREA_COST); break;
                    case "helpDiscount": HELP_DISCOUNT = parseInt(v, HELP_DISCOUNT); break;
                    case "moneyMultiplier": DEFAULT_MONEY_MULTIPLIER = parseInt(v, DEFAULT_MONEY_MULTIPLIER); break;
                    case "moneyCap": DEFAULT_MAX_MONEY_CAP = parseInt(v, DEFAULT_MAX_MONEY_CAP); break;
                    case "spawnX": SPAWN_X = parseInt(v, SPAWN_X); break;
                    case "spawnY": SPAWN_Y = parseInt(v, SPAWN_Y); break;
                    case "spawnInterval": ENEMY_SPAWN_INTERVAL = parseInt(v, (int) ENEMY_SPAWN_INTERVAL); break;
                    case "bossWave": BOSS_WAVE_KILLS = parseInt(v, BOSS_WAVE_KILLS); break;
                    case "chestEvery": CHEST_EVERY = parseInt(v, CHEST_EVERY); break;
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
    }

    static void saveDevData() {
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_vips.txt"));
            for (Map.Entry<String, java.util.List<String>> e : vipUsers.entrySet()) {
                w.println(e.getKey() + "|" + String.join(",", e.getValue()));
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_vip_profiles.txt"));
            for (Map.Entry<String, String> e : vipProfiles.entrySet()) {
                w.println(e.getKey() + "|" + e.getValue());
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_emblems.txt"));
            for (Map.Entry<String, String> e : emblems.entrySet()) {
                w.println(e.getKey() + "|" + e.getValue());
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_user_emblems.txt"));
            for (Map.Entry<String, String> e : userEmblems.entrySet()) {
                w.println(e.getKey() + "|" + e.getValue());
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_player_records.txt"));
            for (Map.Entry<String, String[]> e : playerRecords.entrySet()) {
                w.println(String.join("|", e.getValue()));
            }
            w.close();
        } catch (Exception ignored) {
        }
        try {
            PrintWriter w = new PrintWriter(new FileWriter("friendrun_settings.txt"));
            w.println("gravity=" + GRAVITY);
            w.println("jump=" + JUMP_VELOCITY);
            w.println("speed=" + PLAYER_SPEED);
            w.println("scale=" + playerScale);
            w.println("skyTop=" + SKY_TOP.getRed() + "," + SKY_TOP.getGreen() + "," + SKY_TOP.getBlue());
            w.println("skyBottom=" + SKY_BOTTOM.getRed() + "," + SKY_BOTTOM.getGreen() + "," + SKY_BOTTOM.getBlue());
            w.println("groundTop=" + GROUND_TOP.getRed() + "," + GROUND_TOP.getGreen() + "," + GROUND_TOP.getBlue());
            w.println("groundStripe=" + GROUND_STRIPE.getRed() + "," + GROUND_STRIPE.getGreen() + "," + GROUND_STRIPE.getBlue());
            w.println("maxHealth=" + PLAYER_MAX_HEALTH);
            w.println("startMoney=" + START_MONEY);
            w.println("startSteps=" + START_STEPS);
            w.println("areaCost=" + AREA_COST);
            w.println("helpDiscount=" + HELP_DISCOUNT);
            w.println("moneyMultiplier=" + DEFAULT_MONEY_MULTIPLIER);
            w.println("moneyCap=" + DEFAULT_MAX_MONEY_CAP);
            w.println("spawnX=" + SPAWN_X);
            w.println("spawnY=" + SPAWN_Y);
            w.println("spawnInterval=" + ENEMY_SPAWN_INTERVAL);
            w.println("bossWave=" + BOSS_WAVE_KILLS);
            w.println("chestEvery=" + CHEST_EVERY);
            w.close();
        } catch (Exception ignored) {
        }
    }

    static void applyPlayerRecord(String name) {
        if (name == null) return;
        String[] r = playerRecords.get(name.toLowerCase());
        if (r == null) return;
        playerPoints = parseInt(r[1], playerPoints);
        money = parseInt(r[2], money);
        int st = parseInt(r[3], -1);
        steps = st < 0 ? Integer.MAX_VALUE : st;
        playerLevel = parseInt(r[4], playerLevel);
        levelKills = parseInt(r[5], levelKills);
        totalKills = parseInt(r[6], totalKills);
        playerMaxHealth = parseInt(r[7], PLAYER_MAX_HEALTH);
        playerHealth = parseInt(r[8], playerMaxHealth);
        moneyMultiplier = parseInt(r[9], moneyMultiplier);
        maxMoneyCap = parseInt(r[10], maxMoneyCap);
        kills = parseInt(r[11], kills);
        String inv = r[12] == null ? "" : r[12].trim();
        if (!inv.isEmpty()) {
            ArrayList<Weapon> list = new ArrayList<>();
            for (String wn : inv.split(",")) {
                Weapon w = weaponByName(wn.trim(), 0);
                if (w != null && !list.stream().anyMatch(ow -> ow.name.equalsIgnoreCase(w.name))) list.add(w);
            }
            if (!list.isEmpty()) {
                ownedWeapons = list;
                currentWeapon = ownedWeapons.get(0);
                equippedWeapon = currentWeapon;
            }
        }
    }

    static void startSingleGame() {
        resetSinglePlayer();
        applyPlayerRecord(playerName);
        layoutCards.show(rootCards, "game");
        paint.revalidate();
        singlePlaced = false;
        singleTimer.start();
    }

    static void startGameAs(String name) {
        playerName = name;
        startSingleGame();
        startPointsLabel.setText("Points: " + playerPoints);
    }

    static void refreshVipList() {
        if (vipListPanel == null) return;
        vipListPanel.removeAll();
        for (Map.Entry<String, java.util.List<String>> e : vipUsers.entrySet()) {
            String nm = e.getKey();
            String display = nm;
            String ue = userEmblems.get(nm);
            if (ue != null && emblems.containsKey(ue)) display = emblems.get(ue) + " " + nm;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            row.setOpaque(false);
            JLabel lbl = new JLabel("⭐ " + display + "   [" + String.join(", ", e.getValue()) + "]");
            lbl.setForeground(new Color(255, 225, 120));
            lbl.setFont(new Font("Arial", Font.BOLD, 13));
            JButton start = new JButton("Start");
            start.addActionListener(ev -> startGameAs(nm));
            row.add(lbl);
            row.add(start);
            vipListPanel.add(row);
        }
        vipListPanel.revalidate();
        vipListPanel.repaint();
    }

    static JButton colorButton(Color c) {
        JButton b = new JButton("Pick Color");
        b.setBackground(c);
        b.setOpaque(true);
        b.addActionListener(e -> {
            Color nc = JColorChooser.showDialog(b, "Choose Color", b.getBackground());
            if (nc != null) b.setBackground(nc);
        });
        return b;
    }

    static void buildDevMode(JFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(28, 30, 42));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(34, 37, 52));
        JLabel h = new JLabel("  DEVELOPER MODE");
        h.setFont(new Font("Arial", Font.BOLD, 22));
        h.setForeground(new Color(120, 220, 160));
        JButton back = new JButton("Back");
        back.addActionListener(e -> layoutCards.show(rootCards, "launch"));
        header.add(h, BorderLayout.CENTER);
        header.add(back, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Player Editor", buildPlayerEditor(frame));
        tabs.addTab("Game Settings", buildGameSettings(frame));
        tabs.addTab("VIP Management", buildVipManagement(frame));
        tabs.addTab("Emblems", buildEmblems(frame));
        tabs.addTab("Save System", buildSaveSystem(frame));

        p.add(header, BorderLayout.NORTH);
        p.add(tabs, BorderLayout.CENTER);
        rootCards.add(p, "dev");
    }

    static JPanel buildPlayerEditor(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(38, 41, 58));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        JTextField search = new JTextField(16);
        JButton searchBtn = new JButton("Search / New");
        top.add(new JLabel("Username:"));
        top.add(search);
        top.add(searchBtn);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setOpaque(false);
        JTextField fName = new JTextField();
        JTextField fPoints = new JTextField();
        JTextField fMoney = new JTextField();
        JTextField fSteps = new JTextField();
        JTextField fLevel = new JTextField();
        JTextField fLevelKills = new JTextField();
        JTextField fTotalKills = new JTextField();
        JTextField fMaxHealth = new JTextField();
        JTextField fHealth = new JTextField();
        JTextField fMultiplier = new JTextField();
        JTextField fCap = new JTextField();
        JTextField fKills = new JTextField();
        JTextField fInventory = new JTextField();
        JCheckBox fVip = new JCheckBox("VIP Member");
        JTextField fPerks = new JTextField();
        JComboBox<String> fEmblem = new JComboBox<>();
        fVip.setOpaque(false);
        form.add(new JLabel("Name")); form.add(fName);
        form.add(new JLabel("Score / Points")); form.add(fPoints);
        form.add(new JLabel("Money")); form.add(fMoney);
        form.add(new JLabel("Steps (-1 = infinite)")); form.add(fSteps);
        form.add(new JLabel("Level")); form.add(fLevel);
        form.add(new JLabel("Level Kills")); form.add(fLevelKills);
        form.add(new JLabel("Total Kills")); form.add(fTotalKills);
        form.add(new JLabel("Max Health")); form.add(fMaxHealth);
        form.add(new JLabel("Health")); form.add(fHealth);
        form.add(new JLabel("Money Multiplier")); form.add(fMultiplier);
        form.add(new JLabel("Money Cap")); form.add(fCap);
        form.add(new JLabel("Kills This Run")); form.add(fKills);
        form.add(new JLabel("Inventory (weapon names, comma)")); form.add(fInventory);
        form.add(fVip); form.add(new JLabel(""));
        form.add(new JLabel("Perks (comma)")); form.add(fPerks);
        form.add(new JLabel("Emblem")); form.add(fEmblem);

        JButton applyBtn = new JButton("Apply Changes");
        JButton saveBtn = new JButton("Save Changes");
        JButton resetBtn = new JButton("Reset Player");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(applyBtn);
        buttons.add(saveBtn);
        buttons.add(resetBtn);

        searchBtn.addActionListener(e -> {
            String nm = search.getText().trim();
            if (nm.isEmpty()) return;
            String key = nm.toLowerCase();
            String[] r = playerRecords.get(key);
            if (r == null) {
                r = new String[]{nm, "0", "0", "-1", "1", "0", "0", "" + PLAYER_MAX_HEALTH, "" + PLAYER_MAX_HEALTH,
                        "" + DEFAULT_MONEY_MULTIPLIER, "" + DEFAULT_MAX_MONEY_CAP, "0", "", ""};
            }
            fName.setText(r[0]);
            fPoints.setText(r[1]);
            fMoney.setText(r[2]);
            fSteps.setText(r[3]);
            fLevel.setText(r[4]);
            fLevelKills.setText(r[5]);
            fTotalKills.setText(r[6]);
            fMaxHealth.setText(r[7]);
            fHealth.setText(r[8]);
            fMultiplier.setText(r[9]);
            fCap.setText(r[10]);
            fKills.setText(r[11]);
            fInventory.setText(r[12]);
            java.util.List<String> perks = vipUsers.get(key);
            fVip.setSelected(perks != null);
            fPerks.setText(perks == null ? "" : String.join(",", perks));
            fEmblem.removeAllItems();
            fEmblem.addItem("(none)");
            for (String em : emblems.keySet()) fEmblem.addItem(em);
            String assigned = userEmblems.get(key);
            if (assigned != null && emblems.containsKey(assigned)) fEmblem.setSelectedItem(assigned);
        });

        applyBtn.addActionListener(e -> {
            String nm = fName.getText().trim();
            if (nm.isEmpty()) return;
            String key = nm.toLowerCase();
            String[] r = new String[]{nm, fPoints.getText().trim(), fMoney.getText().trim(), fSteps.getText().trim(),
                    fLevel.getText().trim(), fLevelKills.getText().trim(), fTotalKills.getText().trim(),
                    fMaxHealth.getText().trim(), fHealth.getText().trim(), fMultiplier.getText().trim(),
                    fCap.getText().trim(), fKills.getText().trim(), fInventory.getText().trim(), ""};
            playerRecords.put(key, r);
            java.util.List<String> perks = parsePerks(fPerks.getText());
            if (fVip.isSelected()) vipUsers.put(key, perks);
            else vipUsers.remove(key);
            String sel = (String) fEmblem.getSelectedItem();
            if (sel == null || sel.equals("(none)")) userEmblems.remove(key);
            else userEmblems.put(key, sel);
            saveDevData();
            if (playerName != null && playerName.equalsIgnoreCase(nm)) {
                applyPlayerRecord(playerName);
                if (paint != null) paint.repaint();
                startPointsLabel.setText("Points: " + playerPoints);
            }
            refreshVipList();
            JOptionPane.showMessageDialog(frame, "Applied changes for " + nm + ".");
        });

        saveBtn.addActionListener(e -> {
            String nm = fName.getText().trim();
            if (nm.isEmpty()) return;
            String key = nm.toLowerCase();
            String[] r = new String[]{nm, fPoints.getText().trim(), fMoney.getText().trim(), fSteps.getText().trim(),
                    fLevel.getText().trim(), fLevelKills.getText().trim(), fTotalKills.getText().trim(),
                    fMaxHealth.getText().trim(), fHealth.getText().trim(), fMultiplier.getText().trim(),
                    fCap.getText().trim(), fKills.getText().trim(), fInventory.getText().trim(), ""};
            playerRecords.put(key, r);
            java.util.List<String> perks = parsePerks(fPerks.getText());
            if (fVip.isSelected()) vipUsers.put(key, perks);
            else vipUsers.remove(key);
            String sel = (String) fEmblem.getSelectedItem();
            if (sel == null || sel.equals("(none)")) userEmblems.remove(key);
            else userEmblems.put(key, sel);
            saveDevData();
            refreshVipList();
            JOptionPane.showMessageDialog(frame, "Saved " + nm + ".");
        });

        resetBtn.addActionListener(e -> {
            String nm = fName.getText().trim();
            if (nm.isEmpty()) return;
            playerRecords.remove(nm.toLowerCase());
            if (playerName != null && playerName.equalsIgnoreCase(nm)) {
                resetSinglePlayer();
                startPointsLabel.setText("Points: " + playerPoints);
                if (paint != null) paint.repaint();
            }
            saveDevData();
            JOptionPane.showMessageDialog(frame, nm + " reset to defaults.");
        });

        root.add(top, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    static JPanel buildGameSettings(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(38, 41, 58));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setOpaque(false);

        JTextField fGravity = new JTextField("" + GRAVITY);
        JTextField fJump = new JTextField("" + JUMP_VELOCITY);
        JTextField fSpeed = new JTextField("" + PLAYER_SPEED);
        JTextField fScale = new JTextField("" + playerScale);
        JTextField fMaxHealth = new JTextField("" + PLAYER_MAX_HEALTH);
        JTextField fAreaCost = new JTextField("" + AREA_COST);
        JTextField fHelpDiscount = new JTextField("" + HELP_DISCOUNT);
        JTextField fStartMoney = new JTextField("" + START_MONEY);
        JTextField fStartSteps = new JTextField("" + START_STEPS);
        JTextField fMultiplier = new JTextField("" + DEFAULT_MONEY_MULTIPLIER);
        JTextField fCap = new JTextField("" + DEFAULT_MAX_MONEY_CAP);
        JTextField fSpawnX = new JTextField("" + SPAWN_X);
        JTextField fSpawnY = new JTextField("" + SPAWN_Y);
        JTextField fSpawnInterval = new JTextField("" + ENEMY_SPAWN_INTERVAL);
        JTextField fBossWave = new JTextField("" + BOSS_WAVE_KILLS);
        JTextField fChestEvery = new JTextField("" + CHEST_EVERY);
        JButton skyTopBtn = colorButton(SKY_TOP);
        JButton skyBottomBtn = colorButton(SKY_BOTTOM);
        JButton groundTopBtn = colorButton(GROUND_TOP);
        JButton groundStripeBtn = colorButton(GROUND_STRIPE);

        form.add(new JLabel("Gravity")); form.add(fGravity);
        form.add(new JLabel("Jump Height (negative = up)")); form.add(fJump);
        form.add(new JLabel("Player Speed")); form.add(fSpeed);
        form.add(new JLabel("Player Size %")); form.add(fScale);
        form.add(new JLabel("Max Health")); form.add(fMaxHealth);
        form.add(new JLabel("Area Cost")); form.add(fAreaCost);
        form.add(new JLabel("Help / Shield Discount")); form.add(fHelpDiscount);
        form.add(new JLabel("Start Money")); form.add(fStartMoney);
        form.add(new JLabel("Start Steps")); form.add(fStartSteps);
        form.add(new JLabel("Money Multiplier")); form.add(fMultiplier);
        form.add(new JLabel("Money Cap")); form.add(fCap);
        form.add(new JLabel("Spawn X")); form.add(fSpawnX);
        form.add(new JLabel("Spawn Y")); form.add(fSpawnY);
        form.add(new JLabel("Enemy Spawn Interval (ms)")); form.add(fSpawnInterval);
        form.add(new JLabel("Boss Every (kills)")); form.add(fBossWave);
        form.add(new JLabel("Chest Every (kills)")); form.add(fChestEvery);
        form.add(new JLabel("Sky Top Color")); form.add(skyTopBtn);
        form.add(new JLabel("Sky Bottom Color")); form.add(skyBottomBtn);
        form.add(new JLabel("Ground Color")); form.add(groundTopBtn);
        form.add(new JLabel("Ground Stripe Color")); form.add(groundStripeBtn);

        JButton save = new JButton("Save Settings");
        save.addActionListener(e -> {
            GRAVITY = parseDouble(fGravity.getText(), GRAVITY);
            JUMP_VELOCITY = parseInt(fJump.getText(), JUMP_VELOCITY);
            PLAYER_SPEED = parseInt(fSpeed.getText(), PLAYER_SPEED);
            playerScale = parseDouble(fScale.getText(), playerScale);
            PLAYER_MAX_HEALTH = parseInt(fMaxHealth.getText(), PLAYER_MAX_HEALTH);
            AREA_COST = parseInt(fAreaCost.getText(), AREA_COST);
            HELP_DISCOUNT = parseInt(fHelpDiscount.getText(), HELP_DISCOUNT);
            START_MONEY = parseInt(fStartMoney.getText(), START_MONEY);
            START_STEPS = parseInt(fStartSteps.getText(), START_STEPS);
            DEFAULT_MONEY_MULTIPLIER = parseInt(fMultiplier.getText(), DEFAULT_MONEY_MULTIPLIER);
            DEFAULT_MAX_MONEY_CAP = parseInt(fCap.getText(), DEFAULT_MAX_MONEY_CAP);
            SPAWN_X = parseInt(fSpawnX.getText(), SPAWN_X);
            SPAWN_Y = parseInt(fSpawnY.getText(), SPAWN_Y);
            ENEMY_SPAWN_INTERVAL = parseInt(fSpawnInterval.getText(), (int) ENEMY_SPAWN_INTERVAL);
            BOSS_WAVE_KILLS = parseInt(fBossWave.getText(), BOSS_WAVE_KILLS);
            CHEST_EVERY = parseInt(fChestEvery.getText(), CHEST_EVERY);
            SKY_TOP = skyTopBtn.getBackground();
            SKY_BOTTOM = skyBottomBtn.getBackground();
            GROUND_TOP = groundTopBtn.getBackground();
            GROUND_STRIPE = groundStripeBtn.getBackground();
            if (playerHealth > PLAYER_MAX_HEALTH) playerHealth = PLAYER_MAX_HEALTH;
            if (playerMaxHealth > PLAYER_MAX_HEALTH) playerMaxHealth = PLAYER_MAX_HEALTH;
            saveDevData();
            if (paint != null) paint.repaint();
            JOptionPane.showMessageDialog(frame, "Game settings saved. Start a new run for full effect.");
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bottom.setOpaque(false);
        bottom.add(save);
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    static JPanel buildVipManagement(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(38, 41, 58));

        JTextArea list = new JTextArea(8, 40);
        list.setEditable(false);
        list.setBackground(new Color(50, 54, 75));
        list.setForeground(new Color(220, 225, 240));
        list.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setOpaque(false);
        JTextField fUser = new JTextField();
        JCheckBox fEnabled = new JCheckBox("VIP Enabled");
        fEnabled.setOpaque(false);
        JTextField fPerks = new JTextField();
        form.add(new JLabel("Username")); form.add(fUser);
        form.add(fEnabled); form.add(new JLabel(""));
        form.add(new JLabel("Perks (comma)")); form.add(fPerks);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        JButton addBtn = new JButton("Add / Update VIP");
        JButton removeBtn = new JButton("Remove VIP");
        JButton refreshBtn = new JButton("Refresh List");
        buttons.add(addBtn);
        buttons.add(removeBtn);
        buttons.add(refreshBtn);

        Runnable updateList = () -> {
            StringBuilder sb = new StringBuilder("VIP USERS\n---------\n");
            for (Map.Entry<String, java.util.List<String>> e : vipUsers.entrySet()) {
                sb.append(e.getKey()).append("  |  ").append(String.join(", ", e.getValue())).append("\n");
            }
            list.setText(sb.toString());
        };

        addBtn.addActionListener(e -> {
            String nm = fUser.getText().trim();
            if (nm.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter a username.");
                return;
            }
            String key = nm.toLowerCase();
            if (fEnabled.isSelected()) {
                java.util.List<String> perks = parsePerks(fPerks.getText());
                vipUsers.put(key, perks);
            } else {
                vipUsers.remove(key);
            }
            saveDevData();
            refreshVipList();
            updateList.run();
        });
        removeBtn.addActionListener(e -> {
            String key = fUser.getText().trim().toLowerCase();
            if (vipUsers.remove(key) != null) {
                saveDevData();
                refreshVipList();
                updateList.run();
            }
        });
        refreshBtn.addActionListener(e -> updateList.run());

        JPanel profForm = new JPanel(new GridLayout(0, 2, 8, 6));
        profForm.setOpaque(false);
        JTextField fProfName = new JTextField();
        JTextField fProfPerks = new JTextField();
        profForm.add(new JLabel("Profile Name")); profForm.add(fProfName);
        profForm.add(new JLabel("Profile Perks (comma)")); profForm.add(fProfPerks);
        JButton saveProfBtn = new JButton("Save Profile");
        JButton applyProfBtn = new JButton("Apply Profile to User");
        JPanel profButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        profButtons.setOpaque(false);
        profButtons.add(saveProfBtn);
        profButtons.add(applyProfBtn);

        saveProfBtn.addActionListener(e -> {
            String nm = fProfName.getText().trim();
            if (nm.isEmpty()) return;
            vipProfiles.put(nm, fProfPerks.getText().trim());
            saveDevData();
            JOptionPane.showMessageDialog(frame, "Profile \"" + nm + "\" saved.");
        });
        applyProfBtn.addActionListener(e -> {
            String nm = fProfName.getText().trim();
            String user = fUser.getText().trim();
            String perksStr = vipProfiles.get(nm);
            if (user.isEmpty() || perksStr == null) {
                JOptionPane.showMessageDialog(frame, "Enter a user and an existing profile name.");
                return;
            }
            java.util.List<String> perks = parsePerks(perksStr);
            vipUsers.put(user.toLowerCase(), perks);
            fEnabled.setSelected(true);
            fPerks.setText(perksStr);
            saveDevData();
            refreshVipList();
            updateList.run();
        });

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(form, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(buttons, BorderLayout.NORTH);
        center.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(profForm, BorderLayout.NORTH);
        south.add(profButtons, BorderLayout.SOUTH);

        root.add(north, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    static JPanel buildEmblems(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(38, 41, 58));

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> emblemList = new JList<>(model);
        Runnable refresh = () -> {
            model.clear();
            for (Map.Entry<String, String> e : emblems.entrySet()) {
                model.addElement(e.getValue() + "  " + e.getKey());
            }
        };

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setOpaque(false);
        JTextField fEmblemName = new JTextField();
        JTextField fEmblemSymbol = new JTextField();
        JTextField fUser = new JTextField();
        form.add(new JLabel("Emblem Name")); form.add(fEmblemName);
        form.add(new JLabel("Symbol (1-2 chars)")); form.add(fEmblemSymbol);
        form.add(new JLabel("Assign To User")); form.add(fUser);

        JButton createBtn = new JButton("Create Emblem");
        JButton assignBtn = new JButton("Assign Emblem");
        JButton renameBtn = new JButton("Rename Selected");
        JButton removeBtn = new JButton("Remove Selected");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(createBtn);
        buttons.add(assignBtn);
        buttons.add(renameBtn);
        buttons.add(removeBtn);

        createBtn.addActionListener(e -> {
            String nm = fEmblemName.getText().trim();
            String sym = fEmblemSymbol.getText().trim();
            if (nm.isEmpty() || sym.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter a name and a symbol.");
                return;
            }
            emblems.put(nm, sym);
            saveDevData();
            refresh.run();
        });
        assignBtn.addActionListener(e -> {
            String user = fUser.getText().trim();
            String em = fEmblemName.getText().trim();
            if (user.isEmpty() || !emblems.containsKey(em)) {
                JOptionPane.showMessageDialog(frame, "Enter a user and an existing emblem name.");
                return;
            }
            userEmblems.put(user.toLowerCase(), em);
            saveDevData();
            refreshVipList();
            JOptionPane.showMessageDialog(frame, "Assigned " + em + " to " + user + ".");
        });
        renameBtn.addActionListener(e -> {
            int idx = emblemList.getSelectedIndex();
            if (idx < 0) return;
            String oldName = new ArrayList<>(emblems.keySet()).get(idx);
            String newName = JOptionPane.showInputDialog(frame, "New name for \"" + oldName + "\":", oldName);
            if (newName == null || newName.trim().isEmpty()) return;
            String sym = emblems.remove(oldName);
            emblems.put(newName.trim(), sym);
            for (Map.Entry<String, String> ue : new HashMap<>(userEmblems).entrySet()) {
                if (ue.getValue().equals(oldName)) userEmblems.put(ue.getKey(), newName.trim());
            }
            saveDevData();
            refresh.run();
        });
        removeBtn.addActionListener(e -> {
            int idx = emblemList.getSelectedIndex();
            if (idx < 0) return;
            String nm = new ArrayList<>(emblems.keySet()).get(idx);
            emblems.remove(nm);
            userEmblems.values().removeIf(v -> v.equals(nm));
            saveDevData();
            refresh.run();
            refreshVipList();
        });

        root.add(new JScrollPane(emblemList), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(form, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
        refresh.run();
        return root;
    }

    static void copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception ignored) {
        }
    }

    static JPanel buildSaveSystem(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(38, 41, 58));

        JLabel status = new JLabel(" ");
        status.setForeground(new Color(140, 220, 170));
        status.setFont(new Font("Arial", Font.PLAIN, 13));

        JButton saveBtn = new JButton("Save Game Data");
        JButton backupBtn = new JButton("Backup Save");
        JButton restoreBtn = new JButton("Restore Save");
        JButton exportBtn = new JButton("Export Settings");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(saveBtn);
        buttons.add(backupBtn);
        buttons.add(restoreBtn);
        buttons.add(exportBtn);

        String[] files = {
            "friendrun_player.txt", "friendrun_leaderboard.txt", "friendrun_friends.txt",
            "friendrun_weapons.txt", "friendrun_level.txt", "friendrun_vips.txt",
            "friendrun_vip_profiles.txt", "friendrun_emblems.txt", "friendrun_user_emblems.txt",
            "friendrun_player_records.txt", "friendrun_settings.txt"
        };

        saveBtn.addActionListener(e -> {
            saveData();
            saveDevData();
            status.setText("Game data saved.");
        });
        backupBtn.addActionListener(e -> {
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File bdir = new File("backups");
            bdir.mkdirs();
            int n = 0;
            for (String f : files) {
                File src = new File(f);
                if (src.exists()) {
                    copyFile(src, new File(bdir, stamp + "_" + f));
                    n++;
                }
            }
            status.setText("Backed up " + n + " files to backups/");
        });
        restoreBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File("backups"));
            fc.setDialogTitle("Choose a backup file to restore");
            if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            File f = fc.getSelectedFile();
            String nm = f.getName();
            int underscore = nm.indexOf('_');
            String target = underscore >= 0 ? nm.substring(underscore + 1) : nm;
            copyFile(f, new File(target));
            loadData();
            refreshVipList();
            refreshLeaderboard();
            startPointsLabel.setText("Points: " + playerPoints);
            status.setText("Restored " + target + " from backup.");
        });
        exportBtn.addActionListener(e -> {
            try {
                PrintWriter w = new PrintWriter(new FileWriter("friendrun_dev_export.txt"));
                w.println("== FRIENDRUN DEVELOPER EXPORT ==");
                w.println();
                w.println("--- GAME SETTINGS ---");
                w.println("Gravity = " + GRAVITY);
                w.println("Jump = " + JUMP_VELOCITY);
                w.println("Speed = " + PLAYER_SPEED);
                w.println("Size % = " + playerScale);
                w.println("Max Health = " + PLAYER_MAX_HEALTH);
                w.println("Area Cost = " + AREA_COST);
                w.println("Help Discount = " + HELP_DISCOUNT);
                w.println("Start Money = " + START_MONEY);
                w.println("Start Steps = " + START_STEPS);
                w.println("Money Multiplier = " + DEFAULT_MONEY_MULTIPLIER);
                w.println("Money Cap = " + DEFAULT_MAX_MONEY_CAP);
                w.println("Spawn = (" + SPAWN_X + ", " + SPAWN_Y + ")");
                w.println("Spawn Interval = " + ENEMY_SPAWN_INTERVAL);
                w.println("Boss Every = " + BOSS_WAVE_KILLS);
                w.println("Chest Every = " + CHEST_EVERY);
                w.println();
                w.println("--- VIP USERS ---");
                for (Map.Entry<String, java.util.List<String>> e2 : vipUsers.entrySet()) {
                    w.println(e2.getKey() + " | " + String.join(", ", e2.getValue()));
                }
                w.println();
                w.println("--- EMBLEMS ---");
                for (Map.Entry<String, String> e2 : emblems.entrySet()) {
                    w.println(e2.getKey() + " = " + e2.getValue());
                }
                w.println();
                w.println("--- ASSIGNMENTS ---");
                for (Map.Entry<String, String> e2 : userEmblems.entrySet()) {
                    w.println(e2.getKey() + " = " + e2.getValue());
                }
                w.close();
                status.setText("Exported to friendrun_dev_export.txt");
            } catch (Exception ex) {
                status.setText("Export failed: " + ex.getMessage());
            }
        });

        root.add(buttons, BorderLayout.NORTH);
        root.add(status, BorderLayout.CENTER);
        return root;
    }
}
