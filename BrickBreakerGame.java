import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class BrickBreakerGame {
    public static void main(String[] args) {
        JFrame obj = new JFrame();
        Gameplay gamePlay = new Gameplay();
        
        // Window Setup
        obj.setBounds(10, 10, 700, 600);
        obj.setTitle("Brick Breaker - Levels Edition");
        obj.setResizable(false);
        obj.setVisible(true);
        obj.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        obj.add(gamePlay);
    }
}

class Gameplay extends JPanel implements KeyListener, ActionListener {
    
    // Game State Variables
    private boolean play = false;
    private boolean paused = false;
    private boolean levelFinished = false; // New state for level transition
    private int level = 1; // Track current level
    
    // Smooth Movement Flags
    private boolean moveLeft = false;
    private boolean moveRight = false;
    
    private int score = 0;
    private int totalBricks = 0;
    
    // Timer settings
    private Timer timer;
    private int initialDelay = 8;
    private int delay = initialDelay;
    
    // Paddle Properties
    private int playerX = 310;
    private int playerWidth = 100;
    private int initialPlayerWidth = 100;
    
    // Ball Position and Velocity
    private int ballposX = 120;
    private int ballposY = 350;
    private int ballXdir = -1;
    private int ballYdir = -2;
    
    private MapGenerator map;
    private List<PowerUp> powerUps;
    private SoundUtils sound;

    public Gameplay() {
        // Start at Level 1
        initLevel(level);
        
        powerUps = new ArrayList<>();
        sound = new SoundUtils();
        
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        timer = new Timer(delay, this);
        timer.start();
    }

    // Setup map based on level
    private void initLevel(int lvl) {
        int rows = Math.min(6, 2 + lvl); // Increase rows with level (cap at 6)
        int cols = 7;
        map = new MapGenerator(rows, cols, lvl);
        totalBricks = map.actualBrickCount; // Get accurate count based on pattern
    }

    public void paint(Graphics g) {
        // 1. Background
        g.setColor(Color.black);
        g.fillRect(1, 1, 692, 592);

        // 2. Draw the Bricks
        map.draw((Graphics2D)g);

        // 3. Draw Power-Ups
        for (PowerUp p : powerUps) {
            p.draw(g);
        }

        // 4. Borders
        g.setColor(Color.yellow);
        g.fillRect(0, 0, 3, 592);
        g.fillRect(0, 0, 692, 3);
        g.fillRect(691, 0, 3, 592);

        // 5. Score & Level Display
        g.setColor(Color.white);
        g.setFont(new Font("serif", Font.BOLD, 25));
        g.drawString("Score: " + score, 540, 30);
        g.drawString("Level: " + level, 30, 30);

        // 6. The Paddle
        g.setColor(Color.green);
        g.fillRect(playerX, 550, playerWidth, 8);

        // 7. The Ball
        g.setColor(Color.yellow);
        g.fillOval(ballposX, ballposY, 20, 20);

        // 8. PAUSE MENU
        if (paused && play) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("serif", Font.BOLD, 60));
            g.drawString("PAUSED", 220, 300);
            g.setFont(new Font("serif", Font.BOLD, 20));
            g.drawString("Press P to Resume", 260, 350);
        }

        // 9. GAME OVER (LOSS)
        if (ballposY > 570) {
            play = false;
            ballXdir = 0;
            ballYdir = 0;
            
            g.setColor(Color.RED);
            g.setFont(new Font("serif", Font.BOLD, 30));
            g.drawString("Game Over, Final Score: " + score, 190, 300);
            g.setFont(new Font("serif", Font.BOLD, 20));
            g.drawString("Press Enter to Restart", 230, 350);
        }
        
        // 10. LEVEL COMPLETE (Instead of End Game Victory)
        if(totalBricks <= 0 && !play && levelFinished) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("serif", Font.BOLD, 30));
            g.drawString("Level " + level + " Complete!", 220, 300);
            
            g.setFont(new Font("serif", Font.BOLD, 20));
            g.drawString("Press Enter for Next Level", 230, 350);
        }

        g.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        timer.start();
        
        if (play && !paused) {
            
            // Movement Logic
            if (moveRight) {
                if (playerX >= 690 - playerWidth) playerX = 690 - playerWidth;
                else playerX += 5;
            }
            if (moveLeft) {
                if (playerX < 10) playerX = 10;
                else playerX -= 5;
            }

            // Power Up Logic
            List<PowerUp> toRemove = new ArrayList<>();
            for (PowerUp p : powerUps) {
                p.y += 2; 
                if (new Rectangle(p.x, p.y, 15, 15).intersects(new Rectangle(playerX, 550, playerWidth, 8))) {
                    playerWidth = Math.min(250, playerWidth + 20);
                    sound.playTone(1500, 150); 
                    toRemove.add(p);
                } else if (p.y > 600) {
                    toRemove.add(p);
                }
            }
            powerUps.removeAll(toRemove);

            // Ball & Paddle Collision
            if (new Rectangle(ballposX, ballposY, 20, 20).intersects(new Rectangle(playerX, 550, playerWidth, 8))) {
                ballYdir = -ballYdir;
                sound.playTone(400, 100);
                if (delay > 2) { 
                    delay--; 
                    timer.setDelay(delay);
                }
            }

            // Brick Collision
            A: for (int i = 0; i < map.map.length; i++) {
                for (int j = 0; j < map[0].length; j++) {
                    if (map.map[i][j] > 0) {
                        int brickX = j * map.brickWidth + 80;
                        int brickY = i * map.brickHeight + 50;
                        int brickWidth = map.brickWidth;
                        int brickHeight = map.brickHeight;

                        Rectangle rect = new Rectangle(brickX, brickY, brickWidth, brickHeight);
                        Rectangle ballRect = new Rectangle(ballposX, ballposY, 20, 20);
                        
                        if (ballRect.intersects(rect)) {
                            map.setBrickValue(0, i, j);
                            totalBricks--;
                            score += 5;
                            sound.playTone(800, 100);

                            if (Math.random() < 0.20) {
                                powerUps.add(new PowerUp(brickX + (brickWidth/2), brickY + (brickHeight/2)));
                            }

                            if (ballposX + 19 <= rect.x || ballposX + 1 >= rect.x + rect.width) {
                                ballXdir = -ballXdir;
                            } else {
                                ballYdir = -ballYdir;
                            }
                            
                            // LEVEL UP CHECK
                            if (totalBricks <= 0) {
                                play = false;
                                levelFinished = true;
                                ballXdir = 0;
                                ballYdir = 0;
                                sound.playTone(1000, 500); // Victory sound
                            }
                            
                            break A;
                        }
                    }
                }
            }

            ballposX += ballXdir;
            ballposY += ballYdir;

            if (ballposX < 0) ballXdir = -ballXdir;
            if (ballposY < 0) ballYdir = -ballYdir;
            if (ballposX > 670) ballXdir = -ballXdir;
        }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_P) {
            if (play && !levelFinished) {
                paused = !paused;
                repaint();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            moveRight = true;
            if (!play && !levelFinished && ballposY < 570) play = true; 
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            moveLeft = true;
            if (!play && !levelFinished && ballposY < 570) play = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            // Case 1: Game Over (Reset to Level 1)
            if (!play && ballposY > 570) {
                level = 1;
                score = 0;
                playerWidth = initialPlayerWidth;
                initialDelay = 8;
                resetGameVars();
                initLevel(level);
                repaint();
            }
            // Case 2: Level Completed (Go to Next Level)
            else if (!play && levelFinished) {
                level++;
                levelFinished = false;
                
                // Increase base speed for the next level
                // (Decrease delay, minimum 2ms)
                if (initialDelay > 2) initialDelay--;
                
                playerWidth = initialPlayerWidth; // Reset paddle width
                resetGameVars();
                initLevel(level); // Generate new map pattern
                repaint();
            }
        }
    }
    
    private void resetGameVars() {
        play = true;
        ballposX = 120;
        ballposY = 350;
        ballXdir = -1;
        ballYdir = -2;
        playerX = 310;
        powerUps.clear();
        
        delay = initialDelay;
        timer.setDelay(delay);
        paused = false;
        moveLeft = false;
        moveRight = false;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight = false;
        if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft = false;
    }
}

class MapGenerator {
    public int map[][];
    public int brickWidth;
    public int brickHeight;
    public int actualBrickCount = 0;

    public MapGenerator(int row, int col, int level) {
        map = new int[row][col];
        
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                
                // PATTERN GENERATION
                if (level % 3 == 0) { 
                    // Checkerboard
                    if ((i + j) % 2 == 0) {
                        map[i][j] = 1;
                        actualBrickCount++;
                    } else {
                        map[i][j] = 0;
                    }
                } else if (level % 3 == 2) {
                    // Stripes (skip even columns on odd rows, etc)
                    if (i % 2 == 0) {
                         map[i][j] = 1;
                         actualBrickCount++;
                    } else {
                        if (j % 2 == 0) {
                             map[i][j] = 1;
                             actualBrickCount++;
                        } else {
                            map[i][j] = 0;
                        }
                    }
                } else {
                    // Solid Block (Standard)
                    map[i][j] = 1;
                    actualBrickCount++;
                }
            }
        }
        brickWidth = 540 / col;
        brickHeight = 150 / row;
    }

    public void draw(Graphics2D g) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] > 0) {
                    // Dynamic color based on row
                    if (i % 2 == 0) g.setColor(new Color(240, 240, 240));
                    else g.setColor(new Color(200, 200, 200));
                    
                    g.fillRect(j * brickWidth + 80, i * brickHeight + 50, brickWidth, brickHeight);
                    g.setStroke(new BasicStroke(3));
                    g.setColor(Color.black);
                    g.drawRect(j * brickWidth + 80, i * brickHeight + 50, brickWidth, brickHeight);
                }
            }
        }
    }

    public void setBrickValue(int value, int row, int col) {
        map[row][col] = value;
    }
}

class SoundUtils {
    public void playTone(int hz, int msecs) {
        new Thread(() -> {
            try {
                float sampleRate = 8000F;
                AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();
                byte[] buf = new byte[1];
                for (int i = 0; i < msecs * 8; i++) {
                    double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                    buf[0] = (byte) (Math.sin(angle) * 127.0);
                    sourceDataLine.write(buf, 0, 1);
                }
                sourceDataLine.drain();
                sourceDataLine.close();
            } catch (Exception e) {}
        }).start();
    }
}

class PowerUp {
    public int x, y;
    public PowerUp(int x, int y) { this.x = x; this.y = y; }
    public void draw(Graphics g) { g.setColor(Color.CYAN); g.fillRect(x, y, 15, 15); }
}