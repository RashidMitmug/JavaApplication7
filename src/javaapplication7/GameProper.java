package javaapplication7;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class GameProper implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    private JFrame frame;
    private GamePanel gamePanel;
    private static boolean useWASD = true;
    private PlaySound gameMusic;
    private Point mousePosition = new Point(0, 0);
    private boolean mousePressed = false;
    
    public GameProper() {
        frame = new JFrame("Game");
        gamePanel = new GamePanel();
        gameMusic = new PlaySound();
        gameMusic.playLoop("Audio/gameMusic.wav");
        
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(gamePanel);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.addKeyListener(this);
        frame.addMouseMotionListener(this);
        frame.addMouseListener(this);
        frame.setFocusable(true);
    }
    
    private class Bullet {
        double x, y;
        double dx, dy;
        int speed = 10;
        
        public Bullet(int startX, int startY, double targetX, double targetY) {
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
        
        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillOval((int)x, (int)y, 10, 10);
        }
    }
    
    private class GamePanel extends JPanel {
        private int playerX = 400;
        private int playerY = 300;
        private int playerSpeed = 5;
        private Timer timer;
        private boolean[] keys;
        private ArrayList<Bullet> bullets;
        private long lastShotTime = 0;
        private static final long SHOT_COOLDOWN = 200;
        
        public GamePanel() {
            setBackground(Color.BLACK);
            keys = new boolean[256];
            bullets = new ArrayList<>();
            timer = new Timer(16, e -> {
                movePlayer();
                long currentTime = System.currentTimeMillis();
                if (mousePressed && currentTime - lastShotTime >= SHOT_COOLDOWN) {
                    shoot();
                    lastShotTime = currentTime;
                }
                updateBullets();
                repaint();
            });
            timer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.RED);
            g.fillRect(playerX, playerY, 50, 50);
            
            for(Bullet bullet : bullets) {
                bullet.draw(g);
            }
            
            g.setColor(Color.GREEN);
            g.drawLine(mousePosition.x - 10, mousePosition.y, mousePosition.x + 10, mousePosition.y);
            g.drawLine(mousePosition.x, mousePosition.y - 10, mousePosition.x, mousePosition.y + 10);
        }
        
        private void movePlayer() {
            if (useWASD) {
                if (keys[KeyEvent.VK_W]) playerY -= playerSpeed;
                if (keys[KeyEvent.VK_S]) playerY += playerSpeed;
                if (keys[KeyEvent.VK_A]) playerX -= playerSpeed;
                if (keys[KeyEvent.VK_D]) playerX += playerSpeed;
            } else {
                if (keys[KeyEvent.VK_UP]) playerY -= playerSpeed;
                if (keys[KeyEvent.VK_DOWN]) playerY += playerSpeed;
                if (keys[KeyEvent.VK_LEFT]) playerX -= playerSpeed;
                if (keys[KeyEvent.VK_RIGHT]) playerX += playerSpeed;
            }
            
            playerX = Math.max(0, Math.min(playerX, getWidth() - 50));
            playerY = Math.max(0, Math.min(playerY, getHeight() - 50));
        }
        
        private void shoot() {
            PlaySound shootSound = new PlaySound();
            shootSound.playEffect("Audio/shoot.wav");
            
            int bulletX = playerX + 25;
            int bulletY = playerY + 25;
            bullets.add(new Bullet(bulletX, bulletY, mousePosition.x, mousePosition.y));
        }
        
        private void updateBullets() {
            ArrayList<Bullet> bulletsToRemove = new ArrayList<>();
            
            for(Bullet bullet : bullets) {
                bullet.move();
                
                if(bullet.x < 0 || bullet.x > getWidth() || 
                   bullet.y < 0 || bullet.y > getHeight()) {
                    bulletsToRemove.add(bullet);
                }
            }
            
            bullets.removeAll(bulletsToRemove);
        }
        
        public void setKeys(int keyCode, boolean pressed) {
            keys[keyCode] = pressed;
        }
    }
    
    public void setframe() {
        frame.setVisible(true);
    }
    
    public static void setControlScheme(boolean wasd) {
        useWASD = wasd;
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
        gamePanel.setKeys(e.getKeyCode(), true);
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        gamePanel.setKeys(e.getKeyCode(), false);
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void actionPerformed(ActionEvent e) {}
}
