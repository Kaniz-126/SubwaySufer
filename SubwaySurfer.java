import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SubwaySurfer extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 500;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 70;
    private static final int OBSTACLE_WIDTH = 50;
    private static final int OBSTACLE_HEIGHT = 70;
    private static final int COIN_SIZE = 20;
    private static final int LANE_WIDTH = 200;
    private static final int LEFT_LANE = 150;
    private static final int MIDDLE_LANE = 350;
    private static final int RIGHT_LANE = 550;

    // Game state
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private int score = 0;
    private int coins = 0;
    private int speed = 5;
    private int frameCount = 0;
    private Timer timer;
    private Random random = new Random();

    // Player
    private int playerLane = 1; // 0=left, 1=middle, 2=right
    private int playerX = MIDDLE_LANE;
    private int playerY = GROUND_Y - PLAYER_HEIGHT;
    private boolean isJumping = false;
    private boolean isRolling = false;
    private int jumpVelocity = 0;
    private int targetX = MIDDLE_LANE;
    private boolean invincible = false;
    private int invincibleTimer = 0;

    // Obstacles and coins
    private ArrayList<int[]> obstacles = new ArrayList<>(); // {x, y, lane, type}
    private ArrayList<int[]> coinList = new ArrayList<>();  // {x, y, lane}
    private ArrayList<int[]> powerUps = new ArrayList<>();  // {x, y, type}

    // Background
    private int bgOffset = 0;
    private ArrayList<int[]> buildings = new ArrayList<>();
    private ArrayList<int[]> clouds = new ArrayList<>();

    // Colors
    private Color playerColor = new Color(50, 150, 255);
    private Color obstacleColor = new Color(200, 50, 50);
    private Color coinColor = new Color(255, 215, 0);
    private Color groundColor = new Color(80, 80, 80);
    private Color skyColor = new Color(135, 206, 235);
    private Color trackColor = new Color(100, 100, 100);

    // Lives
    private int lives = 3;

    public SubwaySurfer() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(skyColor);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS

        // Initialize background elements
        initBackground();

        // Show start screen
        showStartScreen();
    }

    private void initBackground() {
        // Add some buildings
        for (int i = 0; i < 10; i++) {
            int bx = i * 150 + random.nextInt(50);
            int bHeight = 100 + random.nextInt(150);
            int bWidth = 80 + random.nextInt(60);
            buildings.add(new int[]{bx, HEIGHT - bHeight - 50, bWidth, bHeight,
                    new Color(random.nextInt(100) + 100,
                            random.nextInt(100) + 100,
                            random.nextInt(100) + 100).getRGB()});
        }

        // Add clouds
        for (int i = 0; i < 5; i++) {
            clouds.add(new int[]{i * 200 + random.nextInt(100),
                    50 + random.nextInt(100),
                    80 + random.nextInt(60),
                    30 + random.nextInt(20)});
        }
    }

    private void showStartScreen() {
        gameRunning = false;
        gameOver = false;
        repaint();
    }

    private void startGame() {
        gameRunning = true;
        gameOver = false;
        score = 0;
        coins = 0;
        speed = 5;
        frameCount = 0;
        lives = 3;
        playerLane = 1;
        playerX = MIDDLE_LANE;
        playerY = GROUND_Y - PLAYER_HEIGHT;
        isJumping = false;
        isRolling = false;
        invincible = false;
        invincibleTimer = 0;
        obstacles.clear();
        coinList.clear();
        powerUps.clear();
        timer.start();
    }

    private void resetGame() {
        timer.stop();
        gameRunning = false;
        gameOver = true;
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            update();
            repaint();
        }
    }

    private void update() {
        frameCount++;

        // Increase speed gradually
        if (frameCount % 300 == 0) {
            speed = Math.min(speed + 1, 20);
        }

        // Update score
        score += speed / 5;

        // Move player towards target lane
        if (playerX < targetX) {
            playerX = Math.min(playerX + 15, targetX);
        } else if (playerX > targetX) {
            playerX = Math.max(playerX - 15, targetX);
        }

        // Handle jumping
        if (isJumping) {
            playerY += jumpVelocity;
            jumpVelocity += 1; // gravity

            if (playerY >= GROUND_Y - PLAYER_HEIGHT) {
                playerY = GROUND_Y - PLAYER_HEIGHT;
                isJumping = false;
                jumpVelocity = 0;
            }
        }

        // Handle rolling (temporary)
        if (isRolling) {
            if (frameCount % 30 == 0) {
                isRolling = false;
            }
        }

        // Handle invincibility
        if (invincible) {
            invincibleTimer--;
            if (invincibleTimer <= 0) {
                invincible = false;
            }
        }

        // Update background
        bgOffset = (bgOffset + speed) % WIDTH;

        // Update buildings
        for (int[] building : buildings) {
            building[0] -= speed / 2;
            if (building[0] + building[2] < 0) {
                building[0] = WIDTH + random.nextInt(100);
                building[2] = 80 + random.nextInt(60);
                building[3] = 100 + random.nextInt(150);
            }
        }

        // Update clouds
        for (int[] cloud : clouds) {
            cloud[0] -= speed / 4;
            if (cloud[0] + cloud[2] < 0) {
                cloud[0] = WIDTH + random.nextInt(100);
                cloud[1] = 50 + random.nextInt(100);
            }
        }

        // Spawn obstacles
        if (frameCount % 80 == 0) {
            spawnObstacle();
        }

        // Spawn coins
        if (frameCount % 50 == 0) {
            spawnCoin();
        }

        // Spawn power-ups
        if (frameCount % 400 == 0) {
            spawnPowerUp();
        }

        // Update obstacles
        ArrayList<int[]> toRemove = new ArrayList<>();
        for (int[] obs : obstacles) {
            obs[0] -= speed;
            if (obs[0] + OBSTACLE_WIDTH < 0) {
                toRemove.add(obs);
            }
        }
        obstacles.removeAll(toRemove);

        // Update coins
        toRemove.clear();
        for (int[] coin : coinList) {
            coin[0] -= speed;
            if (coin[0] + COIN_SIZE < 0) {
                toRemove.add(coin);
            }
        }
        coinList.removeAll(toRemove);

        // Update power-ups
        toRemove.clear();
        for (int[] power : powerUps) {
            power[0] -= speed;
            if (power[0] + 30 < 0) {
                toRemove.add(power);
            }
        }
        powerUps.removeAll(toRemove);

        // Check collisions
        checkCollisions();
    }

    private void spawnObstacle() {
        int lane = random.nextInt(3);
        int laneX = getLaneX(lane);
        int type = random.nextInt(3); // 0=train, 1=barrier, 2=box
        int obsHeight = OBSTACLE_HEIGHT;
        if (type == 1) obsHeight = OBSTACLE_HEIGHT / 2; // barrier is lower

        obstacles.add(new int[]{WIDTH, GROUND_Y - obsHeight, lane, type, obsHeight});
    }

    private void spawnCoin() {
        int lane = random.nextInt(3);
        int laneX = getLaneX(lane);
        int numCoins = 1 + random.nextInt(5);
        for (int i = 0; i < numCoins; i++) {
            int coinY = GROUND_Y - COIN_SIZE - random.nextInt(100);
            coinList.add(new int[]{WIDTH + i * 40, coinY, lane});
        }
    }

    private void spawnPowerUp() {
        int lane = random.nextInt(3);
        int type = random.nextInt(2); // 0=shield, 1=magnet
        powerUps.add(new int[]{WIDTH, GROUND_Y - 80, lane, type});
    }

    private int getLaneX(int lane) {
        switch (lane) {
            case 0: return LEFT_LANE;
            case 1: return MIDDLE_LANE;
            case 2: return RIGHT_LANE;
            default: return MIDDLE_LANE;
        }
    }

    private void checkCollisions() {
        Rectangle playerRect;
        if (isRolling) {
            playerRect = new Rectangle(playerX - PLAYER_WIDTH / 2,
                    playerY + PLAYER_HEIGHT / 2,
                    PLAYER_WIDTH, PLAYER_HEIGHT / 2);
        } else {
            playerRect = new Rectangle(playerX - PLAYER_WIDTH / 2,
                    playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        // Check obstacle collisions
        if (!invincible) {
            ArrayList<int[]> toRemove = new ArrayList<>();
            for (int[] obs : obstacles) {
                int obsHeight = obs[4];
                int obsX = getLaneX(obs[2]);
                Rectangle obsRect = new Rectangle(obsX - OBSTACLE_WIDTH / 2,
                        GROUND_Y - obsHeight, OBSTACLE_WIDTH, obsHeight);

                if (playerRect.intersects(obsRect)) {
                    toRemove.add(obs);
                    lives--;
                    invincible = true;
                    invincibleTimer = 120; // 2 seconds of invincibility
                    if (lives <= 0) {
                        resetGame();
                        return;
                    }
                }
            }
            obstacles.removeAll(toRemove);
        }

        // Check coin collisions
        ArrayList<int[]> coinsToRemove = new ArrayList<>();
        for (int[] coin : coinList) {
            int coinX = getLaneX(coin[2]);
            Rectangle coinRect = new Rectangle(coinX - COIN_SIZE / 2,
                    coin[1], COIN_SIZE, COIN_SIZE);
            if (playerRect.intersects(coinRect)) {
                coinsToRemove.add(coin);
                coins++;
                score += 10;
            }
        }
        coinList.removeAll(coinsToRemove);

        // Check power-up collisions
        ArrayList<int[]> powerToRemove = new ArrayList<>();
        for (int[] power : powerUps) {
            int powerX = getLaneX(power[2]);
            Rectangle powerRect = new Rectangle(powerX - 15, power[1], 30, 30);
            if (playerRect.intersects(powerRect)) {
                powerToRemove.add(power);
                if (power[3] == 0) { // shield
                    invincible = true;
                    invincibleTimer = 300;
                } else { // magnet - collect nearby coins
                    collectNearbyCoins(powerX, power[1]);
                }
            }
        }
        powerUps.removeAll(powerToRemove);
    }

    private void collectNearbyCoins(int px, int py) {
        ArrayList<int[]> toRemove = new ArrayList<>();
        for (int[] coin : coinList) {
            int coinX = getLaneX(coin[2]);
            double dist = Math.sqrt(Math.pow(coinX - px, 2) + Math.pow(coin[1] - py, 2));
            if (dist < 200) {
                toRemove.add(coin);
                coins++;
                score += 10;
            }
        }
        coinList.removeAll(toRemove);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!gameRunning && !gameOver) {
            drawStartScreen(g2d);
        } else if (gameOver) {
            drawGameOver(g2d);
        } else {
            drawGame(g2d);
        }
    }

    private void drawGame(Graphics2D g) {
        // Draw sky
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(100, 180, 255),
                0, HEIGHT, new Color(200, 230, 255));
        g.setPaint(skyGradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw clouds
        g.setColor(Color.WHITE);
        for (int[] cloud : clouds) {
            g.fillOval(cloud[0], cloud[1], cloud[2], cloud[3]);
            g.fillOval(cloud[0] + 20, cloud[1] - 15, cloud[2] - 20, cloud[3]);
            g.fillOval(cloud[0] + cloud[2] - 40, cloud[1], cloud[2] - 30, cloud[3]);
        }

        // Draw buildings
        for (int[] building : buildings) {
            Color bColor = new Color(building[4]);
            g.setColor(bColor);
            g.fillRect(building[0], building[1], building[2], building[3]);

            // Windows
            g.setColor(new Color(255, 255, 150, 150));
            for (int wy = building[1] + 10; wy < building[1] + building[3] - 10; wy += 25) {
                for (int wx = building[0] + 10; wx < building[0] + building[2] - 10; wx += 20) {
                    g.fillRect(wx, wy, 10, 15);
                }
            }

            g.setColor(bColor.darker());
            g.drawRect(building[0], building[1], building[2], building[3]);
        }

        // Draw track/ground
        g.setColor(groundColor);
        g.fillRect(50, GROUND_Y, WIDTH - 100, HEIGHT - GROUND_Y);

        // Track lines
        g.setColor(new Color(60, 60, 60));
        g.fillRect(50, GROUND_Y, WIDTH - 100, 10);

        // Lane dividers
        g.setColor(new Color(200, 200, 200, 100));
        g.drawLine(MIDDLE_LANE - LANE_WIDTH / 2, GROUND_Y,
                MIDDLE_LANE - LANE_WIDTH / 2 + speed * 5, GROUND_Y + 10);
        g.drawLine(MIDDLE_LANE + LANE_WIDTH / 2, GROUND_Y,
                MIDDLE_LANE + LANE_WIDTH / 2 + speed * 5, GROUND_Y + 10);

        // Rail tracks
        g.setColor(new Color(150, 150, 150));
        for (int x = -(bgOffset % 60); x < WIDTH; x += 60) {
            g.fillRect(x, GROUND_Y + 5, 40, 5);
        }

        // Draw coins
        for (int[] coin : coinList) {
            int coinX = getLaneX(coin[2]);
            g.setColor(coinColor);
            g.fillOval(coinX - COIN_SIZE / 2, coin[1], COIN_SIZE, COIN_SIZE);
            g.setColor(new Color(200, 160, 0));
            g.drawOval(coinX - COIN_SIZE / 2, coin[1], COIN_SIZE, COIN_SIZE);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("$", coinX - 4, coin[1] + 14);
        }

        // Draw power-ups
        for (int[] power : powerUps) {
            int powerX = getLaneX(power[2]);
            if (power[3] == 0) { // shield
                g.setColor(new Color(0, 200, 255));
                g.fillOval(powerX - 15, power[1], 30, 30);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString("S", powerX - 6, power[1] + 22);
            } else { // magnet
                g.setColor(new Color(255, 100, 200));
                g.fillOval(powerX - 15, power[1], 30, 30);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString("M", powerX - 8, power[1] + 22);
            }
        }

        // Draw obstacles
        for (int[] obs : obstacles) {
            int obsX = getLaneX(obs[2]);
            int obsHeight = obs[4];
            drawObstacle(g, obsX, GROUND_Y - obsHeight, obs[3], obsHeight);
        }

        // Draw player
        drawPlayer(g);

        // Draw HUD
        drawHUD(g);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int type, int height) {
        switch (type) {
            case 0: // Train
                g.setColor(new Color(150, 50, 50));
                g.fillRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                g.setColor(new Color(200, 200, 255));
                g.fillRect(x - OBSTACLE_WIDTH / 2 + 5, y + 10, 15, 20);
                g.fillRect(x + 5, y + 10, 15, 20);
                g.setColor(obstacleColor);
                g.drawRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                // Wheels
                g.setColor(Color.DARK_GRAY);
                g.fillOval(x - OBSTACLE_WIDTH / 2, y + height - 15, 15, 15);
                g.fillOval(x + OBSTACLE_WIDTH / 2 - 15, y + height - 15, 15, 15);
                break;

            case 1: // Barrier
                g.setColor(new Color(255, 165, 0));
                g.fillRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                // Stripes
                g.setColor(new Color(0, 0, 0, 100));
                for (int i = 0; i < 3; i++) {
                    g.fillRect(x - OBSTACLE_WIDTH / 2 + i * 17, y, 10, height);
                }
                g.setColor(Color.BLACK);
                g.drawRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                break;

            case 2: // Box/Crate
                g.setColor(new Color(139, 90, 43));
                g.fillRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                g.setColor(new Color(100, 60, 20));
                g.drawRect(x - OBSTACLE_WIDTH / 2, y, OBSTACLE_WIDTH, height);
                // Wood grain
                g.drawLine(x - OBSTACLE_WIDTH / 2, y + height / 2,
                        x + OBSTACLE_WIDTH / 2, y + height / 2);
                g.drawLine(x, y, x, y + height);
                break;
        }
    }

    private void drawPlayer(Graphics2D g) {
        int drawY = playerY;
        int drawHeight = PLAYER_HEIGHT;

        if (isRolling) {
            drawY = playerY + PLAYER_HEIGHT / 2;
            drawHeight = PLAYER_HEIGHT / 2;
        }

        // Invincibility flash effect
        if (invincible && invincibleTimer % 6 < 3) {
            g.setColor(new Color(255, 255, 255, 150));
        }

        // Body
        Color pColor = invincible ? new Color(0, 200, 255) : playerColor;
        g.setColor(pColor);
        g.fillRect(playerX - PLAYER_WIDTH / 2, drawY, PLAYER_WIDTH, drawHeight);

        // Head
        g.setColor(new Color(255, 200, 150));
        g.fillOval(playerX - 15, drawY - 25, 30, 30);

        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(playerX - 8, drawY - 18, 6, 6);
        g.fillOval(playerX + 2, drawY - 18, 6, 6);

        // Smile
        g.drawArc(playerX - 8, drawY - 10, 16, 8, 0, -180);

        // Arms
        if (!isRolling) {
            g.setColor(pColor);
            if (isJumping) {
                // Arms up
                g.fillRect(playerX - PLAYER_WIDTH / 2 - 10, drawY - 10, 10, 20);
                g.fillRect(playerX + PLAYER_WIDTH / 2, drawY - 10, 10, 20);
            } else {
                // Arms out
                g.fillRect(playerX - PLAYER_WIDTH / 2 - 10, drawY + 10, 10, 20);
                g.fillRect(playerX + PLAYER_WIDTH / 2, drawY + 10, 10, 20);
            }

            // Legs
            g.setColor(new Color(30, 80, 150));
            g.fillRect(playerX - PLAYER_WIDTH / 2, drawY + drawHeight - 20, 20, 20);
            g.fillRect(playerX + PLAYER_WIDTH / 2 - 20, drawY + drawHeight - 20, 20, 20);
        }

        // Shield effect
        if (invincible) {
            g.setColor(new Color(0, 200, 255, 80));
            g.fillOval(playerX - 35, drawY - 30, 70, 90);
            g.setColor(new Color(0, 200, 255, 150));
            g.drawOval(playerX - 35, drawY - 30, 70, 90);
        }
    }

    private void drawHUD(Graphics2D g) {
        // Semi-transparent HUD background
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(10, 10, 200, 90, 15, 15);

        // Score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Score: " + score, 20, 35);

        // Coins
        g.setColor(coinColor);
        g.fillOval(20, 45, 15, 15);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("x " + coins, 40, 58);

        // Lives
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Lives:", 20, 80);
        for (int i = 0; i < lives; i++) {
            g.setColor(Color.RED);
            drawHeart(g, 75 + i * 25, 68);
        }
        for (int i = lives; i < 3; i++) {
            g.setColor(new Color(100, 100, 100));
            drawHeart(g, 75 + i * 25, 68);
        }

        // Speed indicator
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(WIDTH - 130, 10, 120, 40, 15, 15);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Speed: " + speed, WIDTH - 120, 35);

        // Controls hint
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(WIDTH / 2 - 150, HEIGHT - 35, 300, 25, 10, 10);
        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("← → Move | ↑ Jump | ↓ Roll | P Pause", WIDTH / 2 - 140, HEIGHT - 18);
    }

    private void drawHeart(Graphics2D g, int x, int y) {
        int[] xPoints = {x, x - 5, x - 10, x - 10, x - 5, x, x + 5, x + 10, x + 10, x + 5};
        int[] yPoints = {y + 10, y + 5, y, y - 5, y - 5, y, y - 5, y - 5, y, y + 5};
        g.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    private void drawStartScreen(Graphics2D g) {
        // Background
        GradientPaint bg = new GradientPaint(0, 0, new Color(20, 20, 80),
                0, HEIGHT, new Color(80, 20, 80));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        FontMetrics fm = g.getFontMetrics();
        String title = "SUBWAY SURFER";
        g.setColor(new Color(255, 215, 0));
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, HEIGHT / 2 - 100);

        // Subtitle
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        fm = g.getFontMetrics();
        String subtitle = "A Java Swing Adventure";
        g.drawString(subtitle, WIDTH / 2 - fm.stringWidth(subtitle) / 2, HEIGHT / 2 - 60);

        // Start prompt
        g.setFont(new Font("Arial", Font.BOLD, 28));
        fm = g.getFontMetrics();
        String start = "Press ENTER to Start";
        g.setColor(new Color(0, 255, 100));
        g.drawString(start, WIDTH / 2 - fm.stringWidth(start) / 2, HEIGHT / 2 + 20);

        // Controls
        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        String[] controls = {
                "Controls:",
                "← → Arrow Keys - Change Lane",
                "↑ Arrow Key - Jump",
                "↓ Arrow Key - Roll",
                "P - Pause/Resume"
        };
        for (int i = 0; i < controls.length; i++) {
            g.drawString(controls[i], WIDTH / 2 - fm.stringWidth(controls[i]) / 2,
                    HEIGHT / 2 + 80 + i * 30);
        }

        // Decorative elements
        g.setColor(new Color(255, 215, 0));
        for (int i = 0; i < 5; i++) {
            g.fillOval(50 + i * 60, HEIGHT / 2 - 30, 20, 20);
        }
        for (int i = 0; i < 5; i++) {
            g.fillOval(WIDTH - 350 + i * 60, HEIGHT / 2 - 30, 20, 20);
        }
    }

    private void drawGameOver(Graphics2D g) {
        // Background
        GradientPaint bg = new GradientPaint(0, 0, new Color(80, 0, 0),
                0, HEIGHT, new Color(20, 0, 0));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Game Over text
        g.setFont(new Font("Arial", Font.BOLD, 70));
        FontMetrics fm = g.getFontMetrics();
        String gameOverText = "GAME OVER";
        g.setColor(Color.RED);
        g.drawString(gameOverText, WIDTH / 2 - fm.stringWidth(gameOverText) / 2, HEIGHT / 2 - 80);

        // Score panel
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(WIDTH / 2 - 150, HEIGHT / 2 - 40, 300, 120, 20, 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        fm = g.getFontMetrics();
        String scoreText = "Score: " + score;
        g.drawString(scoreText, WIDTH / 2 - fm.stringWidth(scoreText) / 2, HEIGHT / 2);

        g.setColor(coinColor);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        fm = g.getFontMetrics();
        String coinText = "Coins: " + coins;
        g.drawString(coinText, WIDTH / 2 - fm.stringWidth(coinText) / 2, HEIGHT / 2 + 35);

        g.setColor(new Color(100, 200, 255));
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        String speedText = "Max Speed: " + speed;
        g.drawString(speedText, WIDTH / 2 - fm.stringWidth(speedText) / 2, HEIGHT / 2 + 65);

        // Restart prompt
        g.setColor(new Color(0, 255, 100));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g.getFontMetrics();
        String restart = "Press ENTER to Play Again";
        g.drawString(restart, WIDTH / 2 - fm.stringWidth(restart) / 2, HEIGHT / 2 + 120);

        // Menu prompt
        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        String menu = "Press ESC for Menu";
        g.drawString(menu, WIDTH / 2 - fm.stringWidth(menu) / 2, HEIGHT / 2 + 155);
    }

    private boolean paused = false;

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (!gameRunning && !gameOver) {
            if (key == KeyEvent.VK_ENTER) {
                startGame();
            }
            return;
        }

        if (gameOver) {
            if (key == KeyEvent.VK_ENTER) {
                startGame();
            } else if (key == KeyEvent.VK_ESCAPE) {
                gameOver = false;
                repaint();
            }
            return;
        }

        if (key == KeyEvent.VK_P) {
            paused = !paused;
            if (paused) {
                timer.stop();
                drawPauseScreen();
            } else {
                timer.start();
            }
            return;
        }

        if (paused) return;

        switch (key) {
            case KeyEvent.VK_LEFT:
                if (playerLane > 0) {
                    playerLane--;
                    targetX = getLaneX(playerLane);
                }
                break;

            case KeyEvent.VK_RIGHT:
                if (playerLane < 2) {
                    playerLane++;
                    targetX = getLaneX(playerLane);
                }
                break;

            case KeyEvent.VK_UP:
                if (!isJumping && !isRolling) {
                    isJumping = true;
                    jumpVelocity = -18;
                }
                break;

            case KeyEvent.VK_DOWN:
                if (!isJumping) {
                    isRolling = true;
                    frameCount = (frameCount / 30) * 30; // Reset rolling timer
                }
                break;

            case KeyEvent.VK_ESCAPE:
                resetGame();
                gameOver = false;
                gameRunning = false;
                repaint();
                break;
        }
    }

    private void drawPauseScreen() {
        Graphics2D g = (Graphics2D) getGraphics();
        if (g == null) return;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        FontMetrics fm = g.getFontMetrics();
        String pause = "PAUSED";
        g.drawString(pause, WIDTH / 2 - fm.stringWidth(pause) / 2, HEIGHT / 2);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        fm = g.getFontMetrics();
        String resume = "Press P to Resume";
        g.drawString(resume, WIDTH / 2 - fm.stringWidth(resume) / 2, HEIGHT / 2 + 50);
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Subway Surfer - Java Edition");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            SubwaySurfer game = new SubwaySurfer();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
