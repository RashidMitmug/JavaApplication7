package javaapplication7;
public class Controls {
    private int playerX;
    private int playerY;
    private int playerSpeed = 5;
    private boolean[] keys = new boolean[256];
    private final int MAP_WIDTH = 1000;
    private final int MAP_HEIGHT = 1000;
    private final int WALL_THICKNESS = 50;
    
    public Controls() {
        playerX = MAP_WIDTH/2 - 35;
        playerY = MAP_HEIGHT/2 - 50;
    }
    
    public boolean updateMovement() {
        boolean moved = false;
        int oldX = playerX;
        int oldY = playerY;
        
        if (keys[87]) { playerY -= playerSpeed; moved = true; } // W
        if (keys[83]) { playerY += playerSpeed; moved = true; } // S
        if (keys[65]) { playerX -= playerSpeed; moved = true; } // A
        if (keys[68]) { playerX += playerSpeed; moved = true; } // D
        
        playerX = Math.max(WALL_THICKNESS, Math.min(playerX, MAP_WIDTH - WALL_THICKNESS - 70));
        playerY = Math.max(WALL_THICKNESS, Math.min(playerY, MAP_HEIGHT - WALL_THICKNESS - 100));
        
        return moved && (oldX != playerX || oldY != playerY);
    }
    
    public void setKey(int keyCode, boolean pressed) {
        keys[keyCode] = pressed;
    }
    
    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public void setPlayerX(int x) { playerX = x; }
    public void setPlayerY(int y) { playerY = y; }
}