import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Assignment1_67050437_67050473 extends JPanel {

    static final int W = 600, H = 600;

    static final int FPS = 60;
    static final int FRAME_DELAY = 1000 / FPS;

    static final int T_INSERT_START = 0;
    static final int T_INSERT_END = 2500;
    static final int T_LOADING_END = 3500;
    static final int T_TV_CUT_END = 3900;
    static final int T_TV_STATIC_END = 5400;
    static final int T_TV_BLACK_END = 6000;

    static final int BEN_SCENE_MS = 5000;
    static final int STATIC_ONE_MS = 1000;
    static final int DORAEMON_SCENE_MS = 5000;
    static final int ULTRAMAN_SCENE_MS = 5000;
    static final int STATIC_TWO_MS = 1500;
    static final int WAKEUP_SCENE_1_MS = 3000;
    static final int WAKEUP_SCENE_2_MS = 3000;
    static final int CG_SCENE_MS = 4000;

    long startTime = -1;
    Timer timer;

    List<Point> discOutlinePoints = new ArrayList<>();

    final int logoW = 120, logoH = 60;
    int rangeX, rangeY;
    int stepX = 1, stepY = 1;
    int posX, posY;
    int dirX = 1, dirY = 1;
    double logoX = 90, logoY = 140;
    Color logoColor = new Color(230, 200, 40);
    List<Color> palette = List.of(
            new Color(230, 200, 40),
            new Color(230, 70, 70),
            new Color(70, 160, 230),
            new Color(90, 200, 110),
            new Color(200, 90, 220));
    int cornerHitFlashFrames = 0;

    boolean cornerLocked = false;
    long cornerLockedAtMs = -1;

    final int screenX = 90, screenY = 140, screenW = 420, screenH = 300;

    static final int STATIC_CELL = 4;
    final Random staticRng = new Random();

    public static void main(String[] args) {
        JFrame frame = new JFrame("MY MEMORIES - Assignment 1");
        Assignment1_67050437_67050473 panel = new Assignment1_67050437_67050473();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public Assignment1_67050437_67050473() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(18, 18, 22));
        initBounceGeometry();

        timer = new Timer(FRAME_DELAY, e -> {
            if (startTime < 0)
                startTime = System.currentTimeMillis();
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed >= T_TV_BLACK_END) {
                if (!cornerLocked) {
                    stepBounce(elapsed);
                }
                if (cornerHitFlashFrames > 0)
                    cornerHitFlashFrames--;
            }
            repaint();
        });
        timer.start();
    }

    void initBounceGeometry() {
        rangeX = screenW - logoW;
        rangeY = screenH - logoH;
    }

    void resetAnimation() {
        startTime = System.currentTimeMillis();
        posX = staticRng.nextInt(rangeX + 1);
        posY = staticRng.nextInt(rangeY + 1);
        dirX = staticRng.nextBoolean() ? 1 : -1;
        dirY = staticRng.nextBoolean() ? 1 : -1;
        logoX = screenX + posX;
        logoY = screenY + posY;
        logoColor = palette.get(0);
        cornerLocked = false;
        cornerLockedAtMs = -1;
        cornerHitFlashFrames = 0;
    }

    static List<Point> midpointCircle(int cx, int cy, int radius) {
        List<Point> pts = new ArrayList<>();
        int x = 0;
        int y = radius;
        int d = 1 - radius;

        addCirclePoints(pts, cx, cy, x, y);
        while (x < y) {
            x++;
            if (d < 0) {
                d += 2 * x + 1;
            } else {
                y--;
                d += 2 * (x - y) + 1;
            }
            addCirclePoints(pts, cx, cy, x, y);
        }
        return pts;
    }

    static void addCirclePoints(List<Point> pts, int cx, int cy, int x, int y) {
        pts.add(new Point(cx + x, cy + y));
        pts.add(new Point(cx - x, cy + y));
        pts.add(new Point(cx + x, cy - y));
        pts.add(new Point(cx - x, cy - y));
        pts.add(new Point(cx + y, cy + x));
        pts.add(new Point(cx - y, cy + x));
        pts.add(new Point(cx + y, cy - x));
        pts.add(new Point(cx - y, cy - x));
    }

    void stepBounce(long elapsed) {
        boolean hitX = advanceAxisAndReportBounce(true);
        boolean hitY = advanceAxisAndReportBounce(false);

        logoX = screenX + posX;
        logoY = screenY + posY;

        if (hitX || hitY) {
            int idx = (palette.indexOf(logoColor) + 1) % palette.size();
            logoColor = palette.get(idx);
        }
        if (hitX && hitY || elapsed - T_TV_BLACK_END >= 15000) {
            cornerHitFlashFrames = 20;
            cornerLocked = true;
            cornerLockedAtMs = elapsed;
        }
    }

    boolean advanceAxisAndReportBounce(boolean isX) {
        int range = isX ? rangeX : rangeY;
        int step = isX ? stepX : stepY;
        int pos = isX ? posX : posY;
        int dir = isX ? dirX : dirY;

        pos += dir * step;
        boolean bounced = false;
        if (pos <= 0) {
            pos = 0;
            dir = 1;
            bounced = true;
        } else if (pos >= range) {
            pos = range;
            dir = -1;
            bounced = true;
        }

        if (isX) {
            posX = pos;
            dirX = dir;
        } else {
            posY = pos;
            dirY = dir;
        }
        return bounced;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long elapsed = (startTime < 0) ? 0 : System.currentTimeMillis() - startTime;

        if (elapsed < T_INSERT_END) {
            drawInsertScene(g2, elapsed);
        } else if (elapsed < T_LOADING_END) {
            drawLoadingScene(g2, elapsed);
        } else if (elapsed < T_TV_CUT_END) {
            drawTransition(g2, elapsed);
        } else if (elapsed < T_TV_STATIC_END) {
            drawTVStaticScene(g2, elapsed);
        } else if (elapsed < T_TV_BLACK_END) {
            drawTVBlackScene(g2, elapsed);
        } else {
            drawBounceScene(g2, elapsed);
        }
    }

    void drawInsertScene(Graphics2D g2, long elapsed) {
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);

        double t = clamp01(elapsed / (double) T_INSERT_END);
        double eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;

        int discStartY = 140;
        int discEndY = 450;
        int discX = 300;
        int discY = (int) (discStartY + (discEndY - discStartY) * eased);
        int discRadius = 70;

        drawDisc(g2, discX, discY, discRadius, eased);
        drawPlayerFront(g2, 150, 380, 300, 90);
        restoreFloorBelowPlayer(g2, 470);

        drawCaption(g2, "An old disc, sliding in...", 40);
    }

    void drawLoadingScene(Graphics2D g2, long elapsed) {
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);

        int discX = 300, discY = 450, discRadius = 70;
        drawDisc(g2, discX, discY, discRadius, 1.0);
        drawPlayerFront(g2, 150, 380, 300, 90);
        restoreFloorBelowPlayer(g2, 470);

        boolean on = ((elapsed / 250) % 2 == 0);
        g2.setColor(on ? new Color(80, 230, 120) : new Color(30, 90, 50));
        g2.fill(new Ellipse2D.Double(430, 415, 14, 14));

        drawCaption(g2, "Loading...", 40);
    }

    void drawTransition(Graphics2D g2, long elapsed) {
        double t = (elapsed - T_LOADING_END) / (double) (T_TV_CUT_END - T_LOADING_END);
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);
        drawPlayerFront(g2, 150, 380, 300, 90);
        g2.setColor(new Color(255, 255, 255, (int) (255 * (1 - Math.abs(t - 0.5) * 2))));
        g2.fillRect(0, 0, W, H);
    }

    void drawTVStaticScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);
        drawTVFrame(g2);

        Shape screenShape = new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12);
        g2.setClip(screenShape);

        long localMs = elapsed - T_TV_CUT_END;
        long flashMs = 180;

        if (localMs < flashMs) {
            double ft = localMs / (double) flashMs;
            g2.setColor(Color.BLACK);
            g2.fill(screenShape);
            double lineH = 4 + ft * screenH;
            double lineY = screenY + (screenH - lineH) / 2.0;
            g2.setColor(new Color(235, 235, 245, (int) (255 * (1 - ft * 0.25))));
            g2.fill(new Rectangle2D.Double(screenX, lineY, screenW, lineH));
        } else {
            drawStaticNoise(g2, screenX, screenY, screenW, screenH);

            g2.setColor(new Color(0, 0, 0, 60));
            for (int sy = 0; sy < screenH; sy += 3) {
                g2.fill(new Rectangle2D.Double(screenX, screenY + sy, screenW, 1));
            }

            int bandY = (int) ((localMs / 3) % (screenH + 60)) - 30;
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(new Rectangle2D.Double(screenX, screenY + bandY, screenW, 24));
        }
        g2.setClip(null);
    }

    void drawTVBlackScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);
        drawTVFrame(g2);

        Shape screenShape = new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12);
        g2.setClip(screenShape);

        double t = clamp01((elapsed - T_TV_STATIC_END) / (double) (T_TV_BLACK_END - T_TV_STATIC_END));
        if (t < 0.5) {
            drawStaticNoise(g2, screenX, screenY, screenW, screenH);
            g2.setColor(new Color(0, 0, 0, (int) (255 * (t / 0.5))));
            g2.fill(screenShape);
        } else {
            g2.setColor(Color.BLACK);
            g2.fill(screenShape);
        }
        g2.setClip(null);
    }

    void drawStaticNoise(Graphics2D g2, int x, int y, int w, int h) {
        for (int py = y; py < y + h; py += STATIC_CELL) {
            for (int px = x; px < x + w; px += STATIC_CELL) {
                int v = staticRng.nextInt(256);
                g2.setColor(new Color(v, v, v));
                g2.fillRect(px, py, STATIC_CELL, STATIC_CELL);
            }
        }
    }

    void drawBounceScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);
        drawTVFrame(g2);
        long sinceCorner = cornerLocked ? elapsed - cornerLockedAtMs : 0;

        if (!cornerLocked) {
            g2.setColor(new Color(8, 12, 30));
            g2.fill(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
            if (cornerHitFlashFrames > 0) {
                float alpha = cornerHitFlashFrames / 20f * 0.5f;
                g2.setColor(new Color(1f, 1f, 1f, alpha));
                g2.fill(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
            }
            g2.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
            drawDVDLogo(g2, logoX, logoY, logoColor);
            g2.setClip(null);
            return;
        }

        long t = sinceCorner;
        long t0 = BEN_SCENE_MS;
        long t1 = t0 + STATIC_ONE_MS;
        long t2 = t1 + DORAEMON_SCENE_MS;
        long t3 = t2 + STATIC_ONE_MS;
        long t4 = t3 + ULTRAMAN_SCENE_MS;
        long t5 = t4 + STATIC_TWO_MS;
        long t6 = t5 + WAKEUP_SCENE_1_MS;
        long t7 = t6 + WAKEUP_SCENE_2_MS;
        long t8 = t7 + CG_SCENE_MS;

        if (t < t0) {
            drawFourArmsTransformation(g2, t);
        } else if (t < t1) {
            drawStoryStatic(g2, t - t0, "CHANNEL LOST");
        } else if (t < t2) {
            drawDoraemonGadgetScene(g2, t - t1);
        } else if (t < t3) {
            drawStoryStatic(g2, t - t2, "CHANNEL LOST");
        } else if (t < t4) {
            drawUltramanBeamScene(g2, t - t3);
        } else if (t < t5) {
            drawStoryStatic(g2, t - t4, "");
        } else if (t < t6) {
            drawWakeUpScene1(g2, t - t5);
        } else if (t < t7) {
            drawWakeUpScene2(g2, t - t6);
        } else if (t < t8) {
            drawCGScene(g2, t - t7);
        } else {
            drawCGScene(g2, CG_SCENE_MS);
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    void beginStoryScreen(Graphics2D g2, Color top, Color bottom) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        GradientPaint gradient = new GradientPaint(screenX, screenY, top, screenX, screenY + screenH, bottom);
        g.setPaint(gradient);
        g.fillRect(screenX, screenY, screenW, screenH);
        g.dispose();
    }

    void drawStoryStatic(Graphics2D g2, long elapsed, String message) {
        beginStoryScreen(g2, new Color(30, 30, 36), new Color(4, 4, 8));
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        drawStaticNoise(g, screenX, screenY, screenW, screenH);
        g.setColor(new Color(0, 0, 0, 70));
        for (int y = screenY; y < screenY + screenH; y += 4)
            g.fillRect(screenX, y, screenW, 1);

        if (message != null && !message.isEmpty()) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(elapsed * 0.02);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(message, screenX + (screenW - fm.stringWidth(message)) / 2, screenY + screenH / 2);
        }
        g.dispose();
    }

    void drawWakeUpScene1(Graphics2D g2, long elapsed) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        g.setColor(Color.BLACK);
        g.fillRect(screenX, screenY, screenW, screenH);

        double fade = clamp01(elapsed / 1500.0);
        g.setColor(new Color(255, 255, 255, (int) (255 * fade)));
        g.setFont(new Font("Serif", Font.ITALIC, 24));

        String line = "Your memories have come to an end";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(line, screenX + (screenW - fm.stringWidth(line)) / 2, screenY + screenH / 2 + 10);
        g.dispose();
    }

    void drawWakeUpScene2(Graphics2D g2, long elapsed) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        g.setColor(Color.BLACK);
        g.fillRect(screenX, screenY, screenW, screenH);

        double fade = clamp01(elapsed / 1500.0);
        g.setColor(new Color(255, 255, 255, (int) (255 * fade)));
        g.setFont(new Font("Serif", Font.ITALIC, 24));

        String line = "It is time to wake up.";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(line, screenX + (screenW - fm.stringWidth(line)) / 2, screenY + screenH / 2 + 10);
        g.dispose();
    }

    void drawCGScene(Graphics2D g2, long elapsed) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        g.setColor(Color.BLACK);
        g.fillRect(screenX, screenY, screenW, screenH);

        double fade = clamp01(elapsed / 1500.0);
        g.setColor(new Color(255, 255, 255, (int) (255 * fade)));

        String text1 = "This is the real world";
        String text2 = "Computer Graphics";

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(text1, screenX + (screenW - fm1.stringWidth(text1)) / 2, screenY + screenH / 2 - 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(text2, screenX + (screenW - fm2.stringWidth(text2)) / 2, screenY + screenH / 2 + 30);

        g.dispose();
    }

    void drawFourArmsTransformation(Graphics2D g2, long elapsed) {
        beginStoryScreen(g2, new Color(10, 25, 15), new Color(5, 10, 5));
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));

        double p = clamp01(elapsed / (double) BEN_SCENE_MS);
        int cx = 300;
        int cy = 300;
        double scale = 1.0;

        if (p < 0.45) {
            drawNewBen10(g, cx, cy + 20, scale);
            if (p > 0.3) {
                float flash = (float) ((p - 0.3) / 0.15);
                g.setColor(new Color(100, 255, 100, (int) (150 * flash)));
                int glowR = (int) (150 * flash);
                g.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);
            }
        } else if (p < 0.55) {
            g.setColor(new Color(150, 255, 150));
            g.fillRect(screenX, screenY, screenW, screenH);
            g.setColor(new Color(50, 255, 50, 200));
            g.setStroke(new BasicStroke(15));
            int ringSize = (int) (400 * ((p - 0.45) / 0.1));
            g.drawOval(cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize);
        } else {
            drawNewFourArms(g, cx, cy + 10, scale * 1.15);
            if (p < 0.65) {
                float flash = (float) (1.0 - (p - 0.55) / 0.1);
                g.setColor(new Color(150, 255, 150, (int) (255 * flash)));
                g.fillRect(screenX, screenY, screenW, screenH);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(p > 0.5 ? "FOUR ARMS!" : "IT'S HERO TIME!", 142, 166);
        g.dispose();
    }

    void drawNewBen10(Graphics2D g, int cx, int cy, double s) {
        Color skin = new Color(240, 190, 150);
        Color shirtGreen = new Color(130, 200, 50);
        Color pantsBrown = new Color(160, 95, 45);

        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 50 * s, cy + 130 * s, 100 * s, 15 * s));

        g.setColor(pantsBrown);
        g.fillPolygon(new int[] { (int) (cx - 15 * s), (int) (cx - 40 * s), (int) (cx - 10 * s), (int) (cx - 5 * s) },
                new int[] { (int) (cy + 40 * s), (int) (cy + 130 * s), (int) (cy + 130 * s), (int) (cy + 40 * s) }, 4);
        g.fillPolygon(new int[] { (int) (cx + 15 * s), (int) (cx + 40 * s), (int) (cx + 10 * s), (int) (cx + 5 * s) },
                new int[] { (int) (cy + 40 * s), (int) (cy + 130 * s), (int) (cy + 130 * s), (int) (cy + 40 * s) }, 4);

        g.setColor(new Color(140, 80, 35));
        g.fillOval((int) (cx - 45 * s), (int) (cy + 80 * s), (int) (15 * s), (int) (30 * s));
        g.fillOval((int) (cx + 30 * s), (int) (cy + 80 * s), (int) (15 * s), (int) (30 * s));

        g.setColor(Color.WHITE);
        g.fillArc((int) (cx - 50 * s), (int) (cy + 120 * s), (int) (45 * s), (int) (20 * s), 0, 180);
        g.fillArc((int) (cx + 5 * s), (int) (cy + 120 * s), (int) (45 * s), (int) (20 * s), 0, 180);
        g.setColor(shirtGreen);
        g.fillArc((int) (cx - 50 * s), (int) (cy + 123 * s), (int) (15 * s), (int) (15 * s), 90, 180);
        g.fillArc((int) (cx + 35 * s), (int) (cy + 123 * s), (int) (15 * s), (int) (15 * s), -90, 180);

        g.setColor(Color.BLACK);
        g.fillRoundRect((int) (cx - 20 * s), (int) (cy - 30 * s), (int) (40 * s), (int) (75 * s), 10, 10);

        g.setColor(shirtGreen);
        g.fillRect((int) (cx - 22 * s), (int) (cy - 30 * s), (int) (12 * s), (int) (30 * s));
        g.fillRect((int) (cx + 10 * s), (int) (cy - 30 * s), (int) (12 * s), (int) (30 * s));

        g.setColor(Color.WHITE);
        g.fillOval((int) (cx - 8 * s), (int) (cy - 20 * s), (int) (16 * s), (int) (16 * s));
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (10 * s)));
        g.drawString("10", (int) (cx - 6 * s), (int) (cy - 9 * s));

        g.setColor(Color.WHITE);
        g.fillPolygon(new int[] { (int) (cx - 12 * s), cx, (int) (cx + 12 * s) },
                new int[] { (int) (cy - 30 * s), (int) (cy - 20 * s), (int) (cy - 30 * s) }, 3);

        g.setColor(skin);
        g.fillRect((int) (cx - 5 * s), (int) (cy - 40 * s), (int) (10 * s), (int) (15 * s));
        g.fillOval((int) (cx - 18 * s), (int) (cy - 75 * s), (int) (36 * s), (int) (40 * s));

        g.setColor(Color.WHITE);
        g.fillOval((int) (cx - 12 * s), (int) (cy - 62 * s), (int) (10 * s), (int) (7 * s));
        g.fillOval((int) (cx + 2 * s), (int) (cy - 62 * s), (int) (10 * s), (int) (7 * s));
        g.setColor(shirtGreen);
        g.fillOval((int) (cx - 9 * s), (int) (cy - 61 * s), (int) (4 * s), (int) (4 * s));
        g.fillOval((int) (cx + 5 * s), (int) (cy - 61 * s), (int) (4 * s), (int) (4 * s));

        g.setColor(new Color(150, 100, 70));
        g.drawArc((int) (cx - 5 * s), (int) (cy - 52 * s), (int) (15 * s), (int) (5 * s), 180, 90);

        g.setColor(new Color(100, 50, 20));
        Path2D hair = new Path2D.Double();
        hair.moveTo(cx - 20 * s, cy - 60 * s);
        hair.lineTo(cx - 25 * s, cy - 75 * s);
        hair.lineTo(cx - 10 * s, cy - 85 * s);
        hair.lineTo(cx, cy - 80 * s);
        hair.lineTo(cx + 15 * s, cy - 82 * s);
        hair.lineTo(cx + 22 * s, cy - 65 * s);
        hair.lineTo(cx + 20 * s, cy - 55 * s);
        hair.lineTo(cx + 10 * s, cy - 70 * s);
        hair.lineTo(cx - 10 * s, cy - 75 * s);
        hair.closePath();
        g.fill(hair);

        g.setStroke(new BasicStroke((float) (12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(skin);
        g.draw(new Line2D.Double(cx - 18 * s, cy - 25 * s, cx - 40 * s, cy - 45 * s));
        g.draw(new Line2D.Double(cx - 40 * s, cy - 45 * s, cx - 35 * s, cy - 75 * s));
        g.fillOval((int) (cx - 40 * s), (int) (cy - 85 * s), (int) (12 * s), (int) (15 * s));

        g.draw(new Line2D.Double(cx + 18 * s, cy - 25 * s, cx + 35 * s, cy - 15 * s));
        g.draw(new Line2D.Double(cx + 35 * s, cy - 15 * s, cx - 10 * s, cy - 5 * s));

        g.setColor(Color.BLACK);
        g.fillRoundRect((int) (cx - 18 * s), (int) (cy - 12 * s), (int) (16 * s), (int) (16 * s), 5, 5);
        g.setColor(shirtGreen);
        g.fillOval((int) (cx - 15 * s), (int) (cy - 9 * s), (int) (10 * s), (int) (10 * s));
    }

    void drawNewFourArms(Graphics2D g, int cx, int cy, double s) {
        Color skinRed = new Color(210, 40, 30);
        Color pantsBlack = new Color(20, 20, 20);

        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 70 * s, cy + 130 * s, 140 * s, 20 * s));

        g.setColor(pantsBlack);
        g.fillPolygon(new int[] { (int) (cx - 25 * s), (int) (cx - 70 * s), (int) (cx - 50 * s), (int) (cx - 10 * s) },
                new int[] { (int) (cy + 40 * s), (int) (cy + 110 * s), (int) (cy + 110 * s), (int) (cy + 50 * s) }, 4);
        g.fillPolygon(new int[] { (int) (cx + 25 * s), (int) (cx + 70 * s), (int) (cx + 50 * s), (int) (cx + 10 * s) },
                new int[] { (int) (cy + 40 * s), (int) (cy + 110 * s), (int) (cy + 110 * s), (int) (cy + 50 * s) }, 4);

        g.setColor(skinRed);
        g.fillPolygon(new int[] { (int) (cx - 70 * s), (int) (cx - 90 * s), (int) (cx - 40 * s), (int) (cx - 40 * s) },
                new int[] { (int) (cy + 100 * s), (int) (cy + 125 * s), (int) (cy + 125 * s), (int) (cy + 110 * s) },
                4);
        g.fillPolygon(new int[] { (int) (cx + 70 * s), (int) (cx + 90 * s), (int) (cx + 40 * s), (int) (cx + 40 * s) },
                new int[] { (int) (cy + 100 * s), (int) (cy + 125 * s), (int) (cy + 125 * s), (int) (cy + 110 * s) },
                4);

        g.setColor(pantsBlack);
        g.fillOval((int) (cx - 20 * s), (int) (cy + 35 * s), (int) (40 * s), (int) (25 * s));
        g.setColor(Color.WHITE);
        g.fillOval((int) (cx - 12 * s), (int) (cy + 40 * s), (int) (24 * s), (int) (24 * s));
        g.setColor(Color.BLACK);
        g.fillPolygon(new int[] { (int) (cx - 8 * s), cx, (int) (cx + 8 * s), (int) (cx + 5 * s), (int) (cx - 5 * s) },
                new int[] { (int) (cy + 45 * s), (int) (cy + 50 * s), (int) (cy + 45 * s), (int) (cy + 58 * s),
                        (int) (cy + 58 * s) },
                5);
        g.setColor(new Color(130, 200, 50));
        g.fillOval((int) (cx - 4 * s), (int) (cy + 48 * s), (int) (8 * s), (int) (8 * s));

        g.setColor(Color.WHITE);
        g.fillPolygon(new int[] { (int) (cx - 45 * s), (int) (cx - 30 * s), (int) (cx + 30 * s), (int) (cx + 45 * s) },
                new int[] { (int) (cy - 40 * s), (int) (cy + 40 * s), (int) (cy + 40 * s), (int) (cy - 40 * s) }, 4);

        g.setColor(pantsBlack);
        g.fillRect((int) (cx - 15 * s), (int) (cy - 40 * s), (int) (30 * s), (int) (85 * s));
        g.fillRect((int) (cx - 35 * s), (int) (cy - 40 * s), (int) (70 * s), (int) (15 * s));

        g.setColor(skinRed);
        g.fillRect((int) (cx - 15 * s), (int) (cy - 60 * s), (int) (30 * s), (int) (25 * s));
        g.fillOval((int) (cx - 18 * s), (int) (cy - 80 * s), (int) (36 * s), (int) (30 * s));

        g.setColor(Color.YELLOW);
        g.fillOval((int) (cx - 10 * s), (int) (cy - 72 * s), (int) (6 * s), (int) (4 * s));
        g.fillOval((int) (cx + 4 * s), (int) (cy - 72 * s), (int) (6 * s), (int) (4 * s));
        g.fillOval((int) (cx - 12 * s), (int) (cy - 65 * s), (int) (6 * s), (int) (4 * s));
        g.fillOval((int) (cx + 6 * s), (int) (cy - 65 * s), (int) (6 * s), (int) (4 * s));

        g.setColor(Color.BLACK);
        g.drawArc((int) (cx - 8 * s), (int) (cy - 60 * s), (int) (16 * s), (int) (5 * s), 0, 180);

        g.setStroke(new BasicStroke((float) (20 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.setColor(skinRed);
        g.draw(new Line2D.Double(cx + 45 * s, cy - 35 * s, cx + 85 * s, cy - 60 * s));
        g.draw(new Line2D.Double(cx + 85 * s, cy - 60 * s, cx + 70 * s, cy - 100 * s));
        g.fillOval((int) (cx + 60 * s), (int) (cy - 115 * s), (int) (20 * s), (int) (20 * s));

        g.draw(new Line2D.Double(cx + 40 * s, cy - 5 * s, cx + 80 * s, cy + 25 * s));
        g.draw(new Line2D.Double(cx + 80 * s, cy + 25 * s, cx + 95 * s, cy - 15 * s));
        g.fillOval((int) (cx + 85 * s), (int) (cy - 30 * s), (int) (20 * s), (int) (20 * s));

        g.draw(new Line2D.Double(cx - 45 * s, cy - 35 * s, cx - 85 * s, cy - 60 * s));
        g.draw(new Line2D.Double(cx - 85 * s, cy - 60 * s, cx - 70 * s, cy - 100 * s));
        g.fillOval((int) (cx - 80 * s), (int) (cy - 115 * s), (int) (20 * s), (int) (20 * s));

        g.draw(new Line2D.Double(cx - 40 * s, cy - 5 * s, cx - 80 * s, cy + 25 * s));
        g.draw(new Line2D.Double(cx - 80 * s, cy + 25 * s, cx - 95 * s, cy - 15 * s));
        g.fillOval((int) (cx - 105 * s), (int) (cy - 30 * s), (int) (20 * s), (int) (20 * s));

        g.setStroke(new BasicStroke((float) (6 * s)));
        g.setColor(Color.BLACK);
        g.draw(new Line2D.Double(cx + 60 * s, cy - 47 * s, cx + 75 * s, cy - 38 * s));
        g.draw(new Line2D.Double(cx - 60 * s, cy - 47 * s, cx - 75 * s, cy - 38 * s));
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke((float) (18 * s)));
        g.draw(new Line2D.Double(cx + 75 * s, cy - 80 * s, cx + 72 * s, cy - 90 * s));
        g.draw(new Line2D.Double(cx - 75 * s, cy - 80 * s, cx - 72 * s, cy - 90 * s));
        g.draw(new Line2D.Double(cx + 85 * s, cy + 5 * s, cx + 90 * s, cy - 5 * s));
        g.draw(new Line2D.Double(cx - 85 * s, cy + 5 * s, cx - 90 * s, cy - 5 * s));
    }

    void drawDoraemonGadgetScene(Graphics2D g2, long elapsed) {
        double p = clamp01(elapsed / (double) DORAEMON_SCENE_MS);
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));

        if (p < 0.45) {
            drawNobitaRoom(g);
            int doorX = 350;
            int doorY = 390;
            double doorScale = 0;
            double doorOpen = 0;

            int dorX = 180;
            double dorScale = 0.8;
            int dorY = 390 - (int) (100 * dorScale);

            if (p < 0.15) {
                doorScale = (p / 0.15) * 1.1;
                drawAnywhereDoorBack(g, doorX, doorY, doorScale, doorOpen, false);
                drawAnywhereDoorFront(g, doorX, doorY, doorScale, doorOpen);
                drawDoraemon(g, dorX, dorY, dorScale, p / 0.15);
            } else if (p < 0.25) {
                doorScale = 1.1;
                drawAnywhereDoorBack(g, doorX, doorY, doorScale, doorOpen, false);
                drawAnywhereDoorFront(g, doorX, doorY, doorScale, doorOpen);
                drawDoraemon(g, dorX, dorY, dorScale, 0);
            } else {
                doorScale = 1.1;
                double walk = (p - 0.25) / 0.20;
                doorOpen = Math.min(1.0, walk * 4);

                dorX = 180 + (int) ((doorX - 180) * walk);
                dorScale = 0.8 * (1.0 - 0.5 * walk);
                dorY = doorY - (int) (100 * dorScale);

                drawAnywhereDoorBack(g, doorX, doorY, doorScale, doorOpen, true);
                if (walk < 0.9) {
                    drawDoraemon(g, dorX, dorY, dorScale, 0);
                }
                drawAnywhereDoorFront(g, doorX, doorY, doorScale, doorOpen);
            }
        } else if (p < 0.5) {
            float flash = 1.0f;
            if (p > 0.48)
                flash = (float) (1.0 - (p - 0.48) / 0.02);
            int alpha = Math.max(0, Math.min(255, (int) (255 * flash)));
            g.setColor(new Color(255, 255, 255, alpha));
            g.fillRect(screenX, screenY, screenW, screenH);
        } else {
            drawNatureScene(g);
            int doorX = 250;
            int doorY = 360;
            double doorScale = 1.2;
            double doorOpen = 1.0;

            double emerge = (p - 0.5) / 0.25;
            if (emerge > 1.0)
                emerge = 1.0;

            if (p > 0.85) {
                doorOpen = Math.max(0.0, 1.0 - (p - 0.85) / 0.15);
            }

            drawAnywhereDoorBack(g, doorX, doorY, doorScale, doorOpen, false);
            drawAnywhereDoorFront(g, doorX, doorY, doorScale, doorOpen);

            if (emerge > 0.05) {
                double dorScale = 0.4 + (0.6 * emerge);
                int dorBaseY = doorY + (int) (50 * emerge);
                int dorX = doorX - (int) (30 * emerge);
                int dorY = dorBaseY - (int) (100 * dorScale);
                drawDoraemon(g, dorX, dorY, dorScale, 0);
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("ANYWHERE DOOR!", 210, 166);
        }
        g.dispose();
    }

    void drawNobitaRoom(Graphics2D g) {
        g.setColor(new Color(245, 235, 210));
        g.fillRect(screenX, screenY, screenW, screenH);

        g.setColor(new Color(150, 180, 120));
        g.fillRect(screenX, screenY + 160, screenW, screenH - 160);

        g.setColor(new Color(110, 140, 80));
        g.setStroke(new BasicStroke(4));
        g.drawLine(screenX, screenY + 220, screenX + screenW, screenY + 220);
        g.drawLine(screenX + 160, screenY + 160, screenX + 160, screenY + 220);
        g.drawLine(screenX + 280, screenY + 220, screenX + 280, screenY + screenH);

        g.setColor(new Color(173, 216, 230));
        g.fillRect(screenX + 40, screenY + 40, 120, 100);
        g.setColor(new Color(200, 170, 130));
        g.setStroke(new BasicStroke(6));
        g.drawRect(screenX + 40, screenY + 40, 120, 100);
        g.drawLine(screenX + 100, screenY + 40, screenX + 100, screenY + 140);
        g.drawLine(screenX + 40, screenY + 90, screenX + 160, screenY + 90);

        g.setColor(new Color(139, 69, 19));
        g.fillRect(screenX + 300, screenY + 100, 120, 15);
        g.fillRect(screenX + 310, screenY + 115, 40, 60);
        g.fillRect(screenX + 370, screenY + 115, 40, 60);
    }

    void drawNatureScene(Graphics2D g) {
        g.setPaint(new GradientPaint(screenX, screenY, new Color(135, 206, 235), screenX, screenY + 200,
                new Color(200, 240, 255)));
        g.fillRect(screenX, screenY, screenW, screenH);

        g.setColor(new Color(255, 220, 50));
        g.fillOval(screenX + 320, screenY + 30, 60, 60);

        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(screenX + 60, screenY + 40, 80, 40);
        g.fillOval(screenX + 100, screenY + 30, 90, 50);
        g.fillOval(screenX + 150, screenY + 40, 70, 40);

        g.setColor(new Color(120, 200, 80));
        g.fillArc(screenX - 80, screenY + 130, 350, 200, 0, 180);
        g.setColor(new Color(100, 180, 70));
        g.fillArc(screenX + 120, screenY + 150, 380, 200, 0, 180);

        g.setColor(new Color(80, 160, 50));
        g.fillRect(screenX, screenY + 220, screenW, screenH - 220);
    }

    void drawAnywhereDoorBack(Graphics2D g, int x, int y, double scale, double openFactor, boolean destinationNature) {
        int w = (int) (110 * scale);
        int h = (int) (160 * scale);
        int dx = x - w / 2;
        int dy = y - h;

        if (openFactor > 0) {
            if (destinationNature) {
                g.setColor(new Color(135, 206, 235));
                g.fillRect(dx + 10, dy + 10, w - 20, h - 20);
                g.setColor(new Color(80, 160, 50));
                g.fillRect(dx + 10, dy + h / 2 + 20, w - 20, h / 2 - 30);
            } else {
                g.setColor(new Color(30, 20, 40));
                g.fillRect(dx + 10, dy + 10, w - 20, h - 20);
            }
        }
    }

    void drawAnywhereDoorFront(Graphics2D g, int x, int y, double scale, double openFactor) {
        int w = (int) (110 * scale);
        int h = (int) (160 * scale);
        int dx = x - w / 2;
        int dy = y - h;

        g.setColor(new Color(255, 105, 180));

        g.fillRect(dx, dy, 10, h);
        g.fillRect(dx + w - 10, dy, 10, h);
        g.fillRect(dx, dy, w, 10);

        g.fillRect(dx, dy + h - 10, w, 10);

        if (openFactor < 1.0) {
            int closedW = (int) ((w - 20) * (1.0 - openFactor));
            if (closedW > 0) {
                g.setColor(new Color(255, 182, 193));
                g.fillRect(dx + 10, dy + 10, closedW, h - 20);
                g.setColor(Color.DARK_GRAY);
                g.setStroke(new BasicStroke(1));
                g.drawRect(dx + 10, dy + 10, closedW, h - 20);
            }
        }
        if (openFactor > 0) {
            int openW = (int) ((w - 20) * openFactor * 0.8);
            g.setColor(new Color(255, 150, 180));
            g.fillPolygon(
                    new int[] { dx + w - 10, dx + w - 10 + openW, dx + w - 10 + openW, dx + w - 10 },
                    new int[] { dy + 10, dy + 25, dy + h - 35, dy + h - 10 },
                    4);
            g.setColor(new Color(200, 80, 130));
            g.drawPolygon(
                    new int[] { dx + w - 10, dx + w - 10 + openW, dx + w - 10 + openW, dx + w - 10 },
                    new int[] { dy + 10, dy + 25, dy + h - 35, dy + h - 10 },
                    4);
        }
    }

    void drawDoraemon(Graphics2D g, int cx, int cy, double scale, double p) {
        Color blue = new Color(37, 155, 224);
        Color blueEdge = new Color(22, 91, 151);
        double headR = 66 * scale;

        g.setColor(new Color(0, 0, 0, 75));
        g.fill(new Ellipse2D.Double(cx - 84 * scale, cy + 100 * scale, 168 * scale, 18 * scale));
        g.setColor(new Color(218, 45, 48));
        g.fill(new Ellipse2D.Double(cx + 44 * scale, cy + 54 * scale, 22 * scale, 22 * scale));

        g.setColor(blue);
        g.fill(new RoundRectangle2D.Double(cx - 53 * scale, cy + 22 * scale, 106 * scale, 92 * scale, 34, 34));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (3 * scale)));
        g.draw(new RoundRectangle2D.Double(cx - 53 * scale, cy + 22 * scale, 106 * scale, 92 * scale, 34, 34));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 43 * scale, cy + 54 * scale, 86 * scale, 57 * scale));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 49 * scale, cy + 93 * scale, 54 * scale, 27 * scale));
        g.fill(new Ellipse2D.Double(cx - 5 * scale, cy + 93 * scale, 54 * scale, 27 * scale));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (3 * scale)));
        g.draw(new Ellipse2D.Double(cx - 49 * scale, cy + 93 * scale, 54 * scale, 27 * scale));
        g.draw(new Ellipse2D.Double(cx - 5 * scale, cy + 93 * scale, 54 * scale, 27 * scale));

        g.setColor(blue);
        g.setStroke(new BasicStroke((float) (18 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 48 * scale, cy + 42 * scale, cx - 73 * scale, cy + 73 * scale));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 87 * scale, cy + 63 * scale, 25 * scale, 25 * scale));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Ellipse2D.Double(cx - 87 * scale, cy + 63 * scale, 25 * scale, 25 * scale));

        double reach = (p > 0) ? clamp01(p) : 0;
        g.setColor(blue);
        g.setStroke(new BasicStroke((float) (18 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx + 48 * scale, cy + 42 * scale, cx + (78 + 34 * reach) * scale,
                cy + (53 - 17 * reach) * scale));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx + (68 + 34 * reach) * scale, cy + (41 - 17 * reach) * scale,
                25 * scale, 25 * scale));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Ellipse2D.Double(cx + (68 + 34 * reach) * scale, cy + (41 - 17 * reach) * scale,
                25 * scale, 25 * scale));

        g.setColor(blue);
        g.fill(new Ellipse2D.Double(cx - headR, cy - headR, 2 * headR, 2 * headR));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (3 * scale)));
        g.draw(new Ellipse2D.Double(cx - headR, cy - headR, 2 * headR, 2 * headR));
        Path2D face = new Path2D.Double();
        face.moveTo(cx - 49 * scale, cy - 11 * scale);
        face.curveTo(cx - 55 * scale, cy + 22 * scale, cx - 39 * scale, cy + 60 * scale, cx,
                cy + 65 * scale);
        face.curveTo(cx + 39 * scale, cy + 60 * scale, cx + 55 * scale, cy + 22 * scale, cx + 49 * scale,
                cy - 11 * scale);
        face.curveTo(cx + 28 * scale, cy - 27 * scale, cx - 28 * scale, cy - 27 * scale, cx - 49 * scale,
                cy - 11 * scale);
        face.closePath();
        g.setColor(Color.WHITE);
        g.fill(face);
        g.setColor(new Color(35, 35, 35));
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(face);

        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 29 * scale, cy - 56 * scale, 27 * scale, 38 * scale));
        g.fill(new Ellipse2D.Double(cx + 2 * scale, cy - 56 * scale, 27 * scale, 38 * scale));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(cx - 17 * scale, cy - 43 * scale, 8 * scale, 18 * scale));
        g.fill(new Ellipse2D.Double(cx + 9 * scale, cy - 43 * scale, 8 * scale, 18 * scale));
        g.setColor(new Color(225, 29, 39));
        g.fill(new Ellipse2D.Double(cx - 12 * scale, cy - 20 * scale, 24 * scale, 24 * scale));
        g.setColor(new Color(255, 150, 150));
        g.fill(new Ellipse2D.Double(cx - 5 * scale, cy - 15 * scale, 7 * scale, 7 * scale));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Line2D.Double(cx, cy + 3 * scale, cx, cy + 27 * scale));
        Path2D mouth = new Path2D.Double();
        mouth.moveTo(cx - 33 * scale, cy + 22 * scale);
        mouth.curveTo(cx - 18 * scale, cy + 42 * scale, cx + 18 * scale, cy + 42 * scale, cx + 33 * scale,
                cy + 22 * scale);
        mouth.curveTo(cx + 17 * scale, cy + 31 * scale, cx - 17 * scale, cy + 31 * scale, cx - 33 * scale,
                cy + 22 * scale);
        mouth.closePath();
        g.setColor(new Color(225, 19, 32));
        g.fill(mouth);
        g.setColor(new Color(255, 142, 96));
        g.fill(new Ellipse2D.Double(cx - 14 * scale, cy + 29 * scale, 28 * scale, 11 * scale));

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke((float) (2 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 27 * scale, cy + 2 * scale, cx - 59 * scale, cy - 5 * scale));
        g.draw(new Line2D.Double(cx - 27 * scale, cy + 13 * scale, cx - 61 * scale, cy + 13 * scale));
        g.draw(new Line2D.Double(cx - 27 * scale, cy + 24 * scale, cx - 57 * scale, cy + 31 * scale));
        g.draw(new Line2D.Double(cx + 27 * scale, cy + 2 * scale, cx + 59 * scale, cy - 5 * scale));
        g.draw(new Line2D.Double(cx + 27 * scale, cy + 13 * scale, cx + 61 * scale, cy + 13 * scale));
        g.draw(new Line2D.Double(cx + 27 * scale, cy + 24 * scale, cx + 57 * scale, cy + 31 * scale));

        g.setColor(new Color(220, 35, 45));
        g.fill(new RoundRectangle2D.Double(cx - 46 * scale, cy + 46 * scale, 92 * scale, 12 * scale, 6, 6));
        g.setColor(Color.YELLOW);
        g.fill(new Ellipse2D.Double(cx - 12 * scale, cy + 48 * scale, 24 * scale, 21 * scale));
        g.setColor(new Color(174, 130, 24));
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Arc2D.Double(cx - 10 * scale, cy + 52 * scale, 20 * scale, 14 * scale, 200, 140, Arc2D.OPEN));
        g.setColor(new Color(25, 94, 150));
        g.setStroke(new BasicStroke((float) (3 * scale)));
        g.draw(new Arc2D.Double(cx - 35 * scale, cy + 67 * scale, 70 * scale, 46 * scale, 0, -180, Arc2D.OPEN));
    }

    void drawUltramanBeamScene(Graphics2D g2, long elapsed) {
        beginStoryScreen(g2, new Color(8, 14, 45), new Color(75, 10, 25));
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        for (int i = 0; i < 15; i++) {
            g.setColor(new Color(255, 255, 220, 180));
            g.fill(new Ellipse2D.Double(screenX + 14 + (i * 53) % 390, screenY + 15 + (i * 37) % 135, 3, 3));
        }
        double p = clamp01(elapsed / (double) ULTRAMAN_SCENE_MS);
        int ultraCx = 250;
        int ultraCy = 328;
        double ultraScale = 0.86;

        double beam = clamp01((p - 0.22) / 0.55);
        if (beam > 0) {
            int bx = ultraCx + (int) (43 * ultraScale);
            int by = ultraCy - (int) (3 * ultraScale);
            int tip = bx + (int) (220 * beam);
            Path2D beamShape = new Path2D.Double();
            beamShape.moveTo(bx, by - 13);
            beamShape.lineTo(tip, by - 52 * beam);
            beamShape.lineTo(tip, by + 52 * beam);
            beamShape.lineTo(bx, by + 13);
            beamShape.closePath();
            g.setColor(new Color(75, 210, 255, 100));
            g.fill(beamShape);
            g.setColor(new Color(215, 250, 255, 220));
            g.setStroke(new BasicStroke(5));
            g.draw(new Line2D.Double(bx, by, tip, by));
        }
        drawUltraman(g, ultraCx, ultraCy, ultraScale);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("ULTRAMAN - SPECIUM BEAM", 143, 166);
        g.dispose();
    }

    void drawUltraman(Graphics2D g, int cx, int cy, double scale) {
        Color silver = new Color(180, 190, 205);
        Color red = new Color(180, 35, 45);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 68 * scale, cy + 74 * scale, 136 * scale, 15 * scale));
        g.setStroke(new BasicStroke((float) (19 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(silver);
        g.draw(new Line2D.Double(cx - 19 * scale, cy + 45 * scale, cx - 25 * scale, cy + 112 * scale));
        g.draw(new Line2D.Double(cx + 19 * scale, cy + 45 * scale, cx + 25 * scale, cy + 112 * scale));
        g.setColor(red);
        g.draw(new Line2D.Double(cx - 28 * scale, cy + 98 * scale, cx - 9 * scale, cy + 115 * scale));
        g.draw(new Line2D.Double(cx + 28 * scale, cy + 98 * scale, cx + 9 * scale, cy + 115 * scale));

        Path2D torso = new Path2D.Double();
        torso.moveTo(cx - 34 * scale, cy - 42 * scale);
        torso.lineTo(cx + 34 * scale, cy - 42 * scale);
        torso.lineTo(cx + 43 * scale, cy + 53 * scale);
        torso.lineTo(cx, cy + 72 * scale);
        torso.lineTo(cx - 43 * scale, cy + 53 * scale);
        torso.closePath();
        g.setColor(silver);
        g.fill(torso);
        Path2D redPanel = new Path2D.Double();
        redPanel.moveTo(cx - 31 * scale, cy - 37 * scale);
        redPanel.lineTo(cx - 7 * scale, cy - 13 * scale);
        redPanel.lineTo(cx - 12 * scale, cy + 59 * scale);
        redPanel.lineTo(cx - 38 * scale, cy + 47 * scale);
        redPanel.closePath();
        g.setColor(red);
        g.fill(redPanel);
        redPanel = new Path2D.Double();
        redPanel.moveTo(cx + 31 * scale, cy - 37 * scale);
        redPanel.lineTo(cx + 7 * scale, cy - 13 * scale);
        redPanel.lineTo(cx + 12 * scale, cy + 59 * scale);
        redPanel.lineTo(cx + 38 * scale, cy + 47 * scale);
        redPanel.closePath();
        g.fill(redPanel);
        g.setColor(new Color(235, 240, 245));
        g.setStroke(new BasicStroke((float) (5 * scale)));
        g.draw(new Line2D.Double(cx, cy - 13 * scale, cx, cy + 58 * scale));

        Color armEdge = new Color(77, 87, 103);
        g.setColor(armEdge);
        g.setStroke(new BasicStroke((float) (24 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 31 * scale, cy - 21 * scale, cx + 46 * scale, cy - 5 * scale));
        Path2D raisedArmEdge = new Path2D.Double();
        raisedArmEdge.moveTo(cx + 34 * scale, cy + 39 * scale);
        raisedArmEdge.lineTo(cx + 57 * scale, cy + 28 * scale);
        raisedArmEdge.lineTo(cx + 46 * scale, cy - 5 * scale);
        g.draw(raisedArmEdge);
        g.setColor(silver);
        g.setStroke(new BasicStroke((float) (17 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 31 * scale, cy - 21 * scale, cx + 46 * scale, cy - 5 * scale));
        Path2D raisedArm = new Path2D.Double();
        raisedArm.moveTo(cx + 34 * scale, cy + 39 * scale);
        raisedArm.lineTo(cx + 57 * scale, cy + 28 * scale);
        raisedArm.lineTo(cx + 46 * scale, cy - 5 * scale);
        g.draw(raisedArm);
        g.setColor(red);
        g.fill(new Ellipse2D.Double(cx + 37 * scale, cy - 14 * scale, 21 * scale, 19 * scale));
        g.setColor(new Color(224, 231, 238));
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Line2D.Double(cx + 42 * scale, cy - 10 * scale, cx + 53 * scale, cy - 6 * scale));

        Path2D head = new Path2D.Double();
        head.moveTo(cx, cy - 126 * scale);
        head.curveTo(cx - 18 * scale, cy - 112 * scale, cx - 42 * scale, cy - 92 * scale, cx - 34 * scale,
                cy - 54 * scale);
        head.lineTo(cx + 34 * scale, cy - 54 * scale);
        head.curveTo(cx + 42 * scale, cy - 92 * scale, cx + 18 * scale, cy - 112 * scale, cx,
                cy - 126 * scale);
        head.closePath();
        g.setColor(silver);
        g.fill(head);
        g.setColor(red);
        g.fill(new Polygon(new int[] { (int) (cx - 34 * scale), (int) (cx - 24 * scale), (int) (cx - 29 * scale) },
                new int[] { (int) (cy - 84 * scale), (int) (cy - 55 * scale), (int) (cy - 54 * scale) }, 3));
        g.fill(new Polygon(new int[] { (int) (cx + 34 * scale), (int) (cx + 24 * scale), (int) (cx + 29 * scale) },
                new int[] { (int) (cy - 84 * scale), (int) (cy - 55 * scale), (int) (cy - 54 * scale) }, 3));
        Path2D eye = new Path2D.Double();
        eye.moveTo(cx - 26 * scale, cy - 84 * scale);
        eye.curveTo(cx - 20 * scale, cy - 94 * scale, cx - 10 * scale, cy - 92 * scale, cx - 8 * scale,
                cy - 82 * scale);
        eye.curveTo(cx - 15 * scale, cy - 78 * scale, cx - 21 * scale, cy - 78 * scale, cx - 26 * scale,
                cy - 84 * scale);
        eye.closePath();
        g.setColor(new Color(215, 250, 255));
        g.fill(eye);
        eye = new Path2D.Double();
        eye.moveTo(cx + 26 * scale, cy - 84 * scale);
        eye.curveTo(cx + 20 * scale, cy - 94 * scale, cx + 10 * scale, cy - 92 * scale, cx + 8 * scale,
                cy - 82 * scale);
        eye.curveTo(cx + 15 * scale, cy - 78 * scale, cx + 21 * scale, cy - 78 * scale, cx + 26 * scale,
                cy - 84 * scale);
        eye.closePath();
        g.fill(eye);
        g.setColor(new Color(70, 180, 225));
        g.fill(new Polygon(new int[] { cx, (int) (cx - 7 * scale), (int) (cx + 7 * scale) },
                new int[] { (int) (cy - 122 * scale), (int) (cy - 106 * scale), (int) (cy - 106 * scale) }, 3));
        g.setColor(new Color(65, 85, 105));
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Line2D.Double(cx - 16 * scale, cy - 62 * scale, cx + 16 * scale, cy - 62 * scale));
        g.setColor(new Color(40, 180, 235));
        g.fill(new Ellipse2D.Double(cx - 9 * scale, cy + 30 * scale, 18 * scale, 18 * scale));
    }

    void restoreFloorBelowPlayer(Graphics2D g2, int playerBottomY) {
        g2.setColor(new Color(60, 48, 40));
        g2.fill(new Rectangle2D.Double(0, playerBottomY, W, H - playerBottomY));
    }

    void drawRoomBackground(Graphics2D g2) {
        g2.setColor(new Color(40, 36, 48));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(60, 48, 40));
        g2.fillRect(0, 460, W, H - 460);
        Path2D floorLine = new Path2D.Double();
        floorLine.moveTo(0, 460);
        floorLine.curveTo(150, 450, 450, 470, 600, 460);
        g2.setColor(new Color(80, 64, 54));
        g2.setStroke(new BasicStroke(3));
        g2.draw(floorLine);

        CubicCurve2D shelf = new CubicCurve2D.Double(60, 460, 200, 440, 400, 440, 540, 460);
        g2.setColor(new Color(90, 76, 60));
        g2.setStroke(new BasicStroke(6));
        g2.draw(shelf);
    }

    void drawPlayerBase(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(25, 25, 28));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        g2.setColor(Color.BLACK);
        g2.fill(new RoundRectangle2D.Double(x + 60, y + 8, w - 120, 16, 8, 8));
    }

    void drawPlayerFront(Graphics2D g2, int x, int y, int w, int h) {
        Area panel = new Area(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        Area slotHole = new Area(new RoundRectangle2D.Double(x + 60, y + 8, w - 120, 16, 8, 8));
        panel.subtract(slotHole);

        g2.setColor(new Color(25, 25, 28));
        g2.fill(panel);

        g2.setColor(new Color(60, 60, 66));
        g2.setStroke(new BasicStroke(2));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        g2.setColor(new Color(90, 90, 100));
        g2.fill(new Ellipse2D.Double(x + w - 40, y + 62, 16, 16));
        g2.fill(new Ellipse2D.Double(x + w - 65, y + 62, 16, 16));

        QuadCurve2D detail = new QuadCurve2D.Double(x + 15, y + 66, x + 40, y + 80, x + 65, y + 66);
        g2.setColor(new Color(70, 70, 78));
        g2.setStroke(new BasicStroke(2));
        g2.draw(detail);
    }

    void drawDisc(Graphics2D g2, int cx, int cy, int radius, double shine) {
        List<Point> outer = midpointCircle(cx, cy, radius);
        g2.setColor(new Color(210, 215, 225));
        for (Point p : outer) {
            g2.fillRect(p.x, p.y, 2, 2);
        }
        for (int r = radius - 1; r > 0; r -= 1) {
            List<Point> ring = midpointCircle(cx, cy, r);
            float f = r / (float) radius;
            g2.setColor(new Color(
                    (int) (180 + 40 * f) % 256,
                    (int) (190 + 30 * f) % 256,
                    235));
            for (Point p : ring) {
                g2.fillRect(p.x, p.y, 2, 2);
            }
        }
        g2.setStroke(new BasicStroke(2));
        Color[] shineColors = { new Color(255, 120, 120, 120), new Color(120, 255, 180, 120),
                new Color(140, 160, 255, 120) };
        for (int i = 0; i < 3; i++) {
            QuadCurve2D shineArc = new QuadCurve2D.Double(
                    cx - radius * 0.5, cy - radius * 0.3 + i * 10,
                    cx, cy - radius * 0.8 + i * 10,
                    cx + radius * 0.5, cy - radius * 0.3 + i * 10);
            g2.setColor(shineColors[i]);
            g2.draw(shineArc);
        }
        List<Point> hole = midpointCircle(cx, cy, Math.max(6, radius / 8));
        g2.setColor(new Color(20, 20, 24));
        for (Point p : hole)
            g2.fillRect(p.x, p.y, 2, 2);
        int hr = Math.max(6, radius / 8);
        for (int r = hr - 1; r > 0; r--) {
            List<Point> ring = midpointCircle(cx, cy, r);
            g2.setColor(new Color(20, 20, 24));
            for (Point p : ring)
                g2.fillRect(p.x, p.y, 2, 2);
        }
    }

    void drawTVFrame(Graphics2D g2) {
        g2.setColor(new Color(35, 32, 30));
        g2.fill(new RoundRectangle2D.Double(50, 100, 500, 380, 20, 20));
        g2.setColor(new Color(60, 55, 50));
        g2.setStroke(new BasicStroke(3));
        g2.draw(new RoundRectangle2D.Double(50, 100, 500, 380, 20, 20));

        g2.setStroke(new BasicStroke(6));
        g2.setColor(new Color(35, 32, 30));
        g2.draw(new Line2D.Double(220, 480, 180, 540));
        g2.draw(new Line2D.Double(380, 480, 420, 540));
        g2.draw(new Line2D.Double(150, 540, 450, 540));

        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(70, 65, 60));
        for (int i = 0; i < 6; i++) {
            g2.draw(new Line2D.Double(65, 460 + i * 6, 80, 460 + i * 6));
        }
    }

    void drawDVDLogo(Graphics2D g2, double x, double y, Color c) {
        g2.setColor(c);
        Font old = g2.getFont();
        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        g2.drawString("DVD", (float) x + 8, (float) y + 30);

        Path2D tri = new Path2D.Double();
        tri.moveTo(x + logoW - 26, y + 10);
        tri.lineTo(x + logoW - 26, y + 36);
        tri.lineTo(x + logoW - 6, y + 23);
        tri.closePath();
        g2.fill(tri);

        QuadCurve2D underline = new QuadCurve2D.Double(x, y + 40, x + logoW / 2.0, y + 46, x + logoW, y + 40);
        g2.setStroke(new BasicStroke(2));
        g2.draw(underline);
        g2.setFont(old);
    }

    void drawCaption(Graphics2D g2, String text, int y) {
        g2.setColor(new Color(220, 220, 220));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        g2.drawString(text, (W - tw) / 2, y);
    }

    static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}