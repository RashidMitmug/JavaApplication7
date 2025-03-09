package javaapplication7;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;

public class Tutorial implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    private static final int MAP_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static final int MAP_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    private static final int WALL_THICKNESS = 50;
    private Controls controls;
    private JFrame frame;
    private TutorialPanel tutorialPanel;
    private Point mousePosition = new Point(0, 0);
    private boolean mousePressed = false;
    private int currentStage = 1;
    private Point targetArea;
    private String currentInstruction = "Use WASD to move to the red circle";

    private class Enemy {
        int x, y;
        int health = 3;
        double dx, dy;
        long lastShotTime = 0;
        static final long SHOT_COOLDOWN = 2000;
        boolean canMove = false;
        Color currentColor = Color.RED;
        int animationFrame = 0;

        public Enemy() {
            Random rand = new Random();
            this.x = rand.nextInt(MAP_WIDTH - 200) + 100;
            this.y = rand.nextInt(MAP_HEIGHT - 200) + 100;
            dx = Math.random() * 4 - 2;
            dy = Math.random() * 4 - 2;
        }

        public void move() {
            if (!canMove) return;
            x += dx;
            y += dy;
            
            if (x <= WALL_THICKNESS || x >= MAP_WIDTH - WALL_THICKNESS - 70) dx *= -1;
            if (y <= WALL_THICKNESS || y >= MAP_HEIGHT - WALL_THICKNESS - 100) dy *= -1;
            
            x = Math.max(WALL_THICKNESS, Math.min(x, MAP_WIDTH - WALL_THICKNESS - 70));
            y = Math.max(WALL_THICKNESS, Math.min(y, MAP_HEIGHT - WALL_THICKNESS - 100));
            
            animationFrame = (animationFrame + 1) % 20;
            currentColor = animationFrame < 10 ? Color.RED : Color.ORANGE;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 70, 100);
        }

        public void shoot(ArrayList<Bullet> enemyBullets, int playerX, int playerY) {
            if (!canMove) return;
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShotTime >= SHOT_COOLDOWN) {
                lastShotTime = currentTime;
                
                enemyBullets.add(new Bullet(x + 35, y + 50, playerX, playerY));
                enemyBullets.add(new Bullet(x + 35, y + 50, playerX - 100, playerY));
                enemyBullets.add(new Bullet(x + 35, y + 50, playerX + 100, playerY));
                enemyBullets.add(new Bullet(x + 35, y + 50, playerX, playerY - 100));
            }
        }
    }

    private class Bullet {
        double x, y;
        double dx, dy;
        int speed = 15;

        public Bullet(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            double angle = Math.atan2(targetY - startY, targetX - startX);
            dx = Math.cos(angle) * speed;
            dy = Math.sin(angle) * speed;
        }

        public void move() {
            x += dx;
            y += dy;
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, 10, 10);
        }
    }

    private class TutorialPanel extends JPanel {
        private Timer timer;
        private ArrayList<Bullet> bullets;
        private ArrayList<Enemy> enemies;
        private ArrayList<Bullet> enemyBullets;
        private long lastShotTime = 0;
        private static final long SHOT_COOLDOWN = 200;
        private int playerHealth = 5;
        private static final int MINIMAP_SIZE = 200;
        private int cameraX = 0;
        private int cameraY = 0;
        private Point portalLocation;
        private boolean portalSpawned = false;

        public TutorialPanel() {
            setBackground(Color.BLACK);
            setOpaque(true);
            bullets = new ArrayList<>();
            enemies = new ArrayList<>();
            enemyBullets = new ArrayList<>();
            
            Random rand = new Random();
            targetArea = new Point(
                rand.nextInt(MAP_WIDTH - 600) + 300,
                rand.nextInt(MAP_HEIGHT - 600) + 300
            );

            timer = new Timer(16, e -> {
                controls.updateMovement();
                updateCamera();
                checkStageProgress();
                moveEnemies();
                updateBullets();
                updateEnemyBullets();
                
                if (mousePressed && System.currentTimeMillis() - lastShotTime >= SHOT_COOLDOWN) {
                    shoot();
                    lastShotTime = System.currentTimeMillis();
                }
                
                checkCollisions();
                repaint();
            });
            timer.start();
        }

        private void updateCamera() {
            cameraX = Math.max(0, Math.min(controls.getPlayerX() - getWidth()/2 + 35, MAP_WIDTH - getWidth()));
            cameraY = Math.max(0, Math.min(controls.getPlayerY() - getHeight()/2 + 50, MAP_HEIGHT - getHeight()));
        }

        private void moveEnemies() {
            for (Enemy enemy : enemies) {
                enemy.move();
                if (currentStage == 3) {
                    enemy.shoot(enemyBullets, controls.getPlayerX(), controls.getPlayerY());
                }
            }
        }

        private void shoot() {
            Point worldMouse = new Point(mousePosition.x + cameraX, mousePosition.y + cameraY);
            bullets.add(new Bullet(controls.getPlayerX() + 35, controls.getPlayerY() + 50, 
                                 worldMouse.x, worldMouse.y));
        }

        private void updateBullets() {
            bullets.removeIf(bullet -> 
                bullet.x < 0 || bullet.x > MAP_WIDTH || 
                bullet.y < 0 || bullet.y > MAP_HEIGHT);
            
            for (Bullet bullet : bullets) {
                bullet.move();
            }
        }

        private void updateEnemyBullets() {
            enemyBullets.removeIf(bullet -> 
                bullet.x < 0 || bullet.x > MAP_WIDTH || 
                bullet.y < 0 || bullet.y > MAP_HEIGHT);
            
            for (Bullet bullet : enemyBullets) {
                bullet.move();
            }
        }

        private void checkStageProgress() {
            if (currentStage == 1) {
                Rectangle targetBounds = new Rectangle(targetArea.x - 50, targetArea.y - 50, 100, 100);
                if (targetBounds.contains(controls.getPlayerX() + 35, controls.getPlayerY() + 50)) {
                    currentStage = 2;
                    currentInstruction = "Shoot the enemies (Left Click)";
                    spawnEnemies(3, false);
                }
            } else if (currentStage == 2 && enemies.isEmpty()) {
                currentStage = 3;
                currentInstruction = "Survive and defeat moving enemies";
                spawnEnemies(3, true);
            } else if (currentStage == 3 && enemies.isEmpty() && !portalSpawned) {
                spawnPortal();
                currentInstruction = "Enter the portal to continue";
            }
        }

        private void spawnEnemies(int count, boolean canMove) {
            for (int i = 0; i < count; i++) {
                Enemy enemy = new Enemy();
                enemy.canMove = canMove;
                enemies.add(enemy);
            }
        }

        private void spawnPortal() {
            portalSpawned = true;
            portalLocation = new Point(MAP_WIDTH/2, MAP_HEIGHT/2);
        }

        private void checkCollisions() {
            Rectangle playerBounds = new Rectangle(controls.getPlayerX(), controls.getPlayerY(), 70, 100);
            
            Iterator<Bullet> bulletIt = bullets.iterator();
            while (bulletIt.hasNext()) {
                Bullet bullet = bulletIt.next();
                Iterator<Enemy> enemyIt = enemies.iterator();
                while (enemyIt.hasNext()) {
                    Enemy enemy = enemyIt.next();
                    if (bullet.getBounds().intersects(enemy.getBounds())) {
                        enemy.health--;
                        bulletIt.remove();
                        if (enemy.health <= 0) {
                            enemyIt.remove();
                        }
                        break;
                    }
                }
            }

            Iterator<Bullet> enemyBulletIt = enemyBullets.iterator();
            while (enemyBulletIt.hasNext()) {
                Bullet bullet = enemyBulletIt.next();
                if (bullet.getBounds().intersects(playerBounds)) {
                    playerHealth--;
                    enemyBulletIt.remove();
                    if (playerHealth <= 0) {
                        handleGameOver();
                    }
                }
            }

            if (portalSpawned) {
                Rectangle portalBounds = new Rectangle(portalLocation.x - 50, portalLocation.y - 50, 100, 100);
                if (playerBounds.intersects(portalBounds)) {
                    handleGameComplete();
                }
            }
        }

        private void handleGameOver() {
            if (currentStage == 3) {
                restartStage3();
            } else {
                timer.stop();
                JOptionPane.showMessageDialog(frame, "Game Over!");
                frame.dispose();
            }
        }

        private void handleGameComplete() {
            timer.stop();
            JOptionPane.showMessageDialog(frame, "Tutorial Complete!");
            frame.dispose();
        }

        private void restartStage3() {
            playerHealth = 5;
            controls.setPlayerX(MAP_WIDTH/2 - 35);
            controls.setPlayerY(MAP_HEIGHT/2 - 50);
            enemies.clear();
            enemyBullets.clear();
            bullets.clear();
            spawnEnemies(3, true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            g2d.translate(-cameraX, -cameraY);
            
            // Draw floor
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(WALL_THICKNESS, WALL_THICKNESS, 
                        MAP_WIDTH - 2*WALL_THICKNESS, MAP_HEIGHT - 2*WALL_THICKNESS);
            
            // Draw walls
            g2d.setColor(Color.GREEN);
            g2d.fillRect(0, 0, MAP_WIDTH, WALL_THICKNESS);
            g2d.fillRect(0, MAP_HEIGHT - WALL_THICKNESS, MAP_WIDTH, WALL_THICKNESS);
            g2d.fillRect(0, 0, WALL_THICKNESS, MAP_HEIGHT);
            g2d.fillRect(MAP_WIDTH - WALL_THICKNESS, 0, WALL_THICKNESS, MAP_HEIGHT);
            
            // Draw player
            g2d.setColor(Color.BLUE);
            g2d.fillRect(controls.getPlayerX(), controls.getPlayerY(), 70, 100);
            
            // Draw gun
            g2d.setColor(Color.GRAY);
            Point worldMouse = new Point(mousePosition.x + cameraX, mousePosition.y + cameraY);
            double angle = Math.atan2(worldMouse.y - (controls.getPlayerY() + 50),
                                    worldMouse.x - (controls.getPlayerX() + 35));
            AffineTransform old = g2d.getTransform();
            g2d.translate(controls.getPlayerX() + 35, controls.getPlayerY() + 50);
            g2d.rotate(angle);
            g2d.fillRect(0, -5, 40, 10);
            g2d.setTransform(old);
            
            // Draw enemies
            for (Enemy enemy : enemies) {
                g2d.setColor(enemy.currentColor);
                g2d.fillRect(enemy.x, enemy.y, 70, 100);
            }
            
            // Draw bullets
            g2d.setColor(Color.YELLOW);
            for (Bullet bullet : bullets) {
                g2d.fillOval((int)bullet.x, (int)bullet.y, 10, 10);
            }
            
            g2d.setColor(Color.RED);
            for (Bullet bullet : enemyBullets) {
                g2d.fillOval((int)bullet.x, (int)bullet.y, 10, 10);
            }
            
            // Draw target in stage 1
            if (currentStage == 1) {
                g2d.setColor(Color.RED);
                g2d.fillOval(targetArea.x - 50, targetArea.y - 50, 100, 100);
            }
            
            // Draw portal
            if (portalSpawned) {
                g2d.setColor(Color.MAGENTA);
                g2d.fillOval(portalLocation.x - 50, portalLocation.y - 50, 100, 100);
                g2d.setColor(Color.CYAN);
                g2d.drawOval(portalLocation.x - 40 + (int)(Math.random() * 10),
                            portalLocation.y - 40 + (int)(Math.random() * 10),
                            80, 80);
            }
            
            g2d.translate(cameraX, cameraY);
            
            // Draw health
            g2d.setColor(Color.RED);
            for (int i = 0; i < playerHealth; i++) {
                g2d.fillOval(10 + i * 40, 10, 30, 30);
            }
            
            // Draw crosshair
            g2d.setColor(Color.GREEN);
            g2d.drawLine(mousePosition.x - 10, mousePosition.y, mousePosition.x + 10, mousePosition.y);
            g2d.drawLine(mousePosition.x, mousePosition.y - 10, mousePosition.x, mousePosition.y + 10);
            
            // Draw instructions
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString(currentInstruction, 10, getHeight() - 30);
        }
    }

    public Tutorial() {
        controls = new Controls();
        frame = new JFrame("Tutorial");
        tutorialPanel = new TutorialPanel();
        
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(tutorialPanel);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.addKeyListener(this);
        frame.addMouseMotionListener(this);
        frame.addMouseListener(this);
        frame.setFocusable(true);
        frame.setUndecorated(true);
    }

    public void setframe() {
        frame.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePosition = e.getPoint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        controls.setKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        controls.setKey(e.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tutorial tutorial = new Tutorial();
            tutorial.setframe();
        });
    }
}