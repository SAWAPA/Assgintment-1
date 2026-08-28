import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Assignment1_67050437_67050473 extends JPanel {

    // ---------- Canvas ----------
    static final int W = 600, H = 600;

    // ---------- Timing (milliseconds) ----------
    static final int FPS = 60;
    static final int FRAME_DELAY = 1000 / FPS;

    static final int T_INSERT_START = 0;
    static final int T_INSERT_END = 2500; // hand pushes disc into slot
    static final int T_LOADING_END = 3500; // player "reads" disc, light blinks
    static final int T_TV_CUT_END = 3900; // quick white-flash transition to TV
    static final int T_TV_STATIC_END = 5400; // old CRT "just switched on" static/snow, ~1.5s
    static final int T_TV_BLACK_END = 6000; // static settles to black before the logo appears
    // Story after the DVD logo reaches a corner.
    static final int BEN_SCENE_MS = 5000;
    static final int STATIC_ONE_MS = 1000;
    static final int DORAEMON_SCENE_MS = 5000;
    static final int ULTRAMAN_SCENE_MS = 5000;
    static final int STATIC_TWO_MS = 2000;
    static final int POSTER_SCENE_MS = 6000;

    long startTime = -1;
    Timer timer;

    // ---------- Midpoint circle algorithm storage ----------
    // Each call produces a set of pixel points; we render them as small filled
    // squares (classic raster-style rendering of the algorithm's output).
    List<Point> discOutlinePoints = new ArrayList<>();

    // ---------- Bouncing logo state ----------
    // Motion uses integer step counts so a perfect double-edge (corner) hit is
    // mathematically guaranteed to occur, rather than hoping floating point
    // rounding lines two independent bounces up on the same frame.
    final int logoW = 120, logoH = 60;
    int rangeX, rangeY; // travel distance before hitting an edge, in each axis
    int stepX = 3, stepY = 3; // pixels moved per frame (equal speed = classic 45-degree DVD motion; chosen so
                              // the logo bounces ~7-8 times off the walls before landing exactly in a corner
                              // -- see initBounceGeometry)
    int posX, posY; // integer offset within [0, rangeX] / [0, rangeY]
    int dirX = 1, dirY = 1; // +1 or -1
    double logoX = 90, logoY = 140; // derived screen position (kept as double for drawing); starts at top-left
                                    // corner
    Color logoColor = new Color(230, 200, 40);
    List<Color> palette = List.of(
            new Color(230, 200, 40), // yellow
            new Color(230, 70, 70), // red
            new Color(70, 160, 230), // blue
            new Color(90, 200, 110), // green
            new Color(200, 90, 220) // purple
    );
    int cornerHitFlashFrames = 0;

    // Corner-hit / freeze / restart state (bounce phase only)
    boolean cornerLocked = false; // true once logo has landed exactly in a corner
    long cornerLockedAtMs = -1; // wall-clock time (relative to startTime) when it locked

    // TV inner screen bounds (set once we know TV geometry)
    final int screenX = 90, screenY = 140, screenW = 420, screenH = 300;

    // Chunky low-res cells for the TV static effect. The snow is drawn directly
    // as rectangles, so no external picture or image asset is displayed.
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

    /**
     * rangeX = screenW - logoW = 300, rangeY = screenH - logoH = 240.
     * With stepX=3 and stepY=3 (both divide their range evenly), each axis is
     * an exact integer "triangle wave" reflecting cleanly at 0 and range with
     * no overshoot. periodX = 2*300/3 = 200 frames, periodY = 2*240/3 = 160
     * frames. Both axes hit an extreme (0 or range) on every multiple of
     * their own period; they land on an extreme TOGETHER (a perfect corner
     * hit) at LCM(200,160) = 400 frames = 6.67s, after 7 wall bounces along
     * the way -- giving a slow, readable "bounces several times, then nails
     * the corner" motion instead of an immediate or random-feeling hit.
     */
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

    // ================= Midpoint Circle Algorithm =================
    /**
     * Classic midpoint (Bresenham-style) circle algorithm.
     * Computes the 8-way symmetric points for one octant and mirrors them.
     * Returns the full set of boundary points for a circle centered at (cx, cy)
     * with the given integer radius.
     */
    static List<Point> midpointCircle(int cx, int cy, int radius) {
        List<Point> pts = new ArrayList<>();
        int x = 0;
        int y = radius;
        int d = 1 - radius; // initial decision parameter

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

    // ================= Bounce physics =================
    /**
     * Advances the logo by one frame using exact integer arithmetic on each
     * axis independently (a "triangle wave" that reflects precisely at 0 and
     * range, never overshooting). Because the step sizes are divisors of
     * their axis range, posX and posY always land exactly on 0 or range at
     * the moment of a bounce -- so checking "both axes at an extreme on the
     * same frame" is an exact test, not an approximation.
     */
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
            // the legendary perfect corner hit -- freeze the logo right here
            cornerHitFlashFrames = 20;
            cornerLocked = true;
            cornerLockedAtMs = elapsed;

        }
    }

    /**
     * Moves one axis by its step and reflects at the boundaries; returns true if it
     * bounced this frame.
     */
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

    // ================= Paint =================
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

    // ---------------- Scene 1: hand inserts disc ----------------
    void drawInsertScene(Graphics2D g2, long elapsed) {
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);

        double t = clamp01(elapsed / (double) T_INSERT_END);
        // ease-in-out for the slide
        double eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;

        int discStartY = 140;
        // 450 = player front edge (y=380) + disc radius (70): at eased=1 the disc's
        // TOP just reaches the panel's top edge, so the visible crescent above the
        // panel shrinks smoothly to zero exactly as the disc "reaches" the slot --
        // see drawPlayerFront, which is what actually hides the rest of it.
        int discEndY = 450;
        int discX = 300;
        int discY = (int) (discStartY + (discEndY - discStartY) * eased);
        int discRadius = 70;

        drawDisc(g2, discX, discY, discRadius, eased);
        // Redraw the panel ON TOP of the disc, minus the slot opening: this is what
        // makes the disc look like it's actually going INSIDE the player instead of
        // just floating in front of it.
        drawPlayerFront(g2, 150, 380, 300, 90);
        // the disc is bigger than the panel is tall, so its bottom edge can dip past
        // the panel and over the floor -- paper over that with the floor's own color
        restoreFloorBelowPlayer(g2, 470);

        drawCaption(g2, "An old disc, sliding in...", 40);
    }

    // ---------------- Scene 2: loading / reading ----------------
    void drawLoadingScene(Graphics2D g2, long elapsed) {
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);

        // Disc is now fully behind the panel; drawPlayerFront's slot cut-out is what
        // keeps only a sliver of it visible -- no manual clip rectangle needed anymore.
        int discX = 300, discY = 450, discRadius = 70;
        drawDisc(g2, discX, discY, discRadius, 1.0);
        drawPlayerFront(g2, 150, 380, 300, 90);
        restoreFloorBelowPlayer(g2, 470);

        // blinking loading light on the player (drawn with a small circle path too)
        boolean on = ((elapsed / 250) % 2 == 0);
        g2.setColor(on ? new Color(80, 230, 120) : new Color(30, 90, 50));
        g2.fill(new Ellipse2D.Double(430, 415, 14, 14));

        drawCaption(g2, "Loading...", 40);
    }

    // ---------------- Scene 3: quick flash transition ----------------
    void drawTransition(Graphics2D g2, long elapsed) {
        double t = (elapsed - T_LOADING_END) / (double) (T_TV_CUT_END - T_LOADING_END);
        drawRoomBackground(g2);
        drawPlayerBase(g2, 150, 380, 300, 90);
        drawPlayerFront(g2, 150, 380, 300, 90);
        g2.setColor(new Color(255, 255, 255, (int) (255 * (1 - Math.abs(t - 0.5) * 2))));
        g2.fillRect(0, 0, W, H);
    }

    // ---------------- Scene 4: old CRT static/snow (TV just switched on)
    // ----------------
    void drawTVStaticScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);
        drawTVFrame(g2);

        Shape screenShape = new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12);
        g2.setClip(screenShape);

        long localMs = elapsed - T_TV_CUT_END;
        long flashMs = 180; // brief CRT power-on flash: a thin bright line expands to full height

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

            // scanlines overlay, like an interlaced CRT
            g2.setColor(new Color(0, 0, 0, 60));
            for (int sy = 0; sy < screenH; sy += 3) {
                g2.fill(new Rectangle2D.Double(screenX, screenY + sy, screenW, 1));
            }

            // a dark band slowly rolling down the screen, like a mistuned channel
            int bandY = (int) ((localMs / 3) % (screenH + 60)) - 30;
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(new Rectangle2D.Double(screenX, screenY + bandY, screenW, 24));
        }

        g2.setClip(null);
    }

    // ---------------- Scene 5: static settles to black before the logo appears
    // ----------------
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

    /** Chunky black-and-white noise, drawn directly with Java 2D rectangles. */
    void drawStaticNoise(Graphics2D g2, int x, int y, int w, int h) {
        for (int py = y; py < y + h; py += STATIC_CELL) {
            for (int px = x; px < x + w; px += STATIC_CELL) {
                int v = staticRng.nextInt(256);
                g2.setColor(new Color(v, v, v));
                g2.fillRect(px, py, STATIC_CELL, STATIC_CELL);
            }
        }
    }

    // ---------------- Scene 6: TV + bouncing logo and memory story ----------------
    void drawBounceScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);
        drawTVFrame(g2);
        long sinceCorner = cornerLocked ? elapsed - cornerLockedAtMs : 0;

        if (!cornerLocked) {
            // The DVD phase ends when it hits a corner (or after the safety timeout).
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

        // Every story frame is drawn from Java 2D primitives; no JPG is loaded.
        long t = sinceCorner;
        if (t < BEN_SCENE_MS) {
            drawFourArmsTransformation(g2, t);
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS) {
            drawStoryStatic(g2, t - BEN_SCENE_MS, "CHANNEL LOST");
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS + DORAEMON_SCENE_MS) {
            drawDoraemonGadgetScene(g2, t - BEN_SCENE_MS - STATIC_ONE_MS);
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS + DORAEMON_SCENE_MS + STATIC_ONE_MS) {
            drawStoryStatic(g2, t - BEN_SCENE_MS - STATIC_ONE_MS - DORAEMON_SCENE_MS, "CHANNEL LOST");
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS + DORAEMON_SCENE_MS + STATIC_ONE_MS + ULTRAMAN_SCENE_MS) {
            drawUltramanBeamScene(g2,
                    t - BEN_SCENE_MS - STATIC_ONE_MS - DORAEMON_SCENE_MS - STATIC_ONE_MS);
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS + DORAEMON_SCENE_MS + STATIC_ONE_MS + ULTRAMAN_SCENE_MS
                + STATIC_TWO_MS) {
            drawStoryStatic(g2,
                    t - BEN_SCENE_MS - STATIC_ONE_MS - DORAEMON_SCENE_MS - STATIC_ONE_MS - ULTRAMAN_SCENE_MS,
                    "CHANNEL LOST");
        } else if (t < BEN_SCENE_MS + STATIC_ONE_MS + DORAEMON_SCENE_MS + STATIC_ONE_MS + ULTRAMAN_SCENE_MS
                + STATIC_TWO_MS + POSTER_SCENE_MS) {
            drawSeminarPoster(g2, t - BEN_SCENE_MS - STATIC_ONE_MS - DORAEMON_SCENE_MS - STATIC_ONE_MS
                    - ULTRAMAN_SCENE_MS - STATIC_TWO_MS);
        } else {
            resetAnimation();
        }
    }

    // ================= Story scenes drawn with Java 2D =================

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

        float pulse = 0.75f + 0.25f * (float) Math.sin(elapsed * 0.02);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(message, screenX + (screenW - fm.stringWidth(message)) / 2, screenY + screenH / 2);
        g.dispose();
    }

    void drawFourArmsTransformation(Graphics2D g2, long elapsed) {
        beginStoryScreen(g2, new Color(9, 35, 70), new Color(72, 18, 74));
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));

        // Green transformation rings and sparks.
        double p = clamp01(elapsed / (double) BEN_SCENE_MS);
        for (int r = 25; r < 180; r += 28) {
            float a = (float) (0.18 + 0.18 * Math.sin(elapsed * 0.01 + r));
            g.setColor(new Color(80, 255, 85, Math.max(20, (int) (255 * a))));
            g.setStroke(new BasicStroke(3));
            g.draw(new Ellipse2D.Double(300 - r - p * 12, 295 - r * 0.65, 2 * r + p * 24, 2 * r * 0.65));
        }
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI / 6.0 + elapsed * 0.002;
            int sx = (int) (300 + Math.cos(ang) * (130 + 20 * Math.sin(elapsed * 0.006)));
            int sy = (int) (305 + Math.sin(ang) * 95);
            g.setColor(new Color(170, 255, 105, 180));
            g.fill(new Ellipse2D.Double(sx - 3, sy - 3, 6, 6));
        }

        // First Ben slaps the Omnitrix, then the alien body grows out of the
        // human silhouette. The final red form remains on screen long enough
        // to be read clearly.
        boolean fourArms = p > 0.62;
        double transform = clamp01((p - 0.20) / 0.42);
        if (p < 0.48) {
            drawBenCharacter(g, 300, 318, 0.82, false, transform, elapsed, elapsed < 1250);
        } else if (p < 0.66) {
            // Cross-fade the anatomically aligned human and alien drawings so
            // the new arms grow from the shoulders instead of popping in.
            float alienAlpha = (float) clamp01((p - 0.48) / 0.18);
            Graphics2D human = (Graphics2D) g.create();
            human.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alienAlpha));
            drawBenCharacter(human, 300, 318, 0.82, false, transform, elapsed, false);
            human.dispose();
            Graphics2D alien = (Graphics2D) g.create();
            alien.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alienAlpha));
            drawBenCharacter(alien, 300, 318, 0.82, true, transform, elapsed, false);
            alien.dispose();
        } else {
            drawBenCharacter(g, 300, 318, 0.82, true, transform, elapsed, false);
        }
        if (elapsed < 1250)
            drawOmnitrixTap(g, elapsed);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(fourArms ? "FOUR ARMS!" : "BEN 10 - HIT THE OMNITRIX", 142, 166);
        g.dispose();
    }

    void drawBenCharacter(Graphics2D g, int cx, int cy, double scale, boolean fourArms,
            double transform, long elapsed, boolean tapping) {
        int bodyW = (int) ((fourArms ? 122 : 66) * scale);
        int bodyH = (int) ((fourArms ? 120 : 105) * scale);
        int headR = (int) ((fourArms ? 41 : 35) * scale);
        // The head is anchored to the shoulders; this removes the floating-head
        // gap that was visible in the previous version.
        int headY = cy - (int) (74 * scale);
        Color skin = fourArms ? new Color(180, 62, 42) : new Color(216, 141, 99);

        // Shadow and legs.
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 75 * scale, cy + 78 * scale, 150 * scale, 18 * scale));
        g.setStroke(new BasicStroke((float) (17 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(34, 34, 42));
        g.draw(new Line2D.Double(cx - 20 * scale, cy + 58 * scale, cx - 27 * scale, cy + 115 * scale));
        g.draw(new Line2D.Double(cx + 20 * scale, cy + 58 * scale, cx + 27 * scale, cy + 115 * scale));
        g.setColor(new Color(25, 25, 25));
        g.draw(new Line2D.Double(cx - 33 * scale, cy + 116 * scale, cx - 8 * scale, cy + 116 * scale));
        g.draw(new Line2D.Double(cx + 8 * scale, cy + 116 * scale, cx + 34 * scale, cy + 116 * scale));

        // Main torso.
        // Neck and shoulder connection.
        g.setColor(skin);
        g.fill(new RoundRectangle2D.Double(cx - 15 * scale, headY + headR - 8 * scale, 30 * scale,
                28 * scale, 12, 12));
        g.setColor(fourArms ? new Color(170, 54, 39) : new Color(45, 45, 48));
        g.fill(new RoundRectangle2D.Double(cx - bodyW / 2.0, cy - 34 * scale, bodyW, bodyH, 22, 22));
        g.setColor(new Color(25, 25, 25));
        g.fill(new Rectangle2D.Double(cx - bodyW / 2.0, cy - 8 * scale, bodyW, 16 * scale));
        if (!fourArms) {
            g.setColor(new Color(65, 155, 72));
            g.fill(new RoundRectangle2D.Double(cx - bodyW / 2.0 + 5 * scale, cy - 30 * scale,
                    bodyW - 10 * scale, 24 * scale, 12, 12));
            g.setColor(Color.WHITE);
            g.fill(new Polygon(new int[] { (int) (cx - 12 * scale), cx, (int) (cx + 12 * scale) },
                    new int[] { (int) (cy - 30 * scale), (int) (cy - 9 * scale), (int) (cy - 30 * scale) }, 3));
        }

        // Upper arms and the extra Four Arms pair.
        drawCharacterArm(g, cx - 34 * scale, cy - 16 * scale, cx - 77 * scale, cy + 35 * scale,
                skin, scale);
        if (tapping) {
            // Ben's existing right arm reaches across his chest to the left
            // wrist. It is the same arm, so the tap cannot create a third arm.
            drawCharacterArm(g, cx + 34 * scale, cy - 16 * scale, cx - 77 * scale, cy + 35 * scale,
                    skin, scale);
        } else {
            drawCharacterArm(g, cx + 34 * scale, cy - 16 * scale, cx + 77 * scale, cy + 35 * scale,
                    skin, scale);
        }
        if (fourArms) {
            double wave = Math.sin(elapsed * 0.008) * 10;
            drawCharacterArm(g, cx - 30 * scale, cy + 20 * scale, cx - 86 * scale, cy + 72 * scale + wave,
                    skin, scale);
            drawCharacterArm(g, cx + 30 * scale, cy + 20 * scale, cx + 86 * scale, cy + 72 * scale - wave,
                    skin, scale);
            // Four Arms has broad shoulders, a red alien torso and a visible
            // black belt instead of Ben's shirt.
            g.setColor(new Color(225, 91, 61, 160));
            g.fill(new Ellipse2D.Double(cx - 34 * scale, cy - 27 * scale, 28 * scale, 17 * scale));
            g.fill(new Ellipse2D.Double(cx + 6 * scale, cy - 27 * scale, 28 * scale, 17 * scale));
        }

        // Head, hair, eyes and the Omnitrix.
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(cx - headR, headY - headR, 2 * headR, 2 * headR));
        Path2D hair = new Path2D.Double();
        hair.moveTo(cx - headR, headY - 8 * scale);
        hair.curveTo(cx - 28 * scale, headY - 48 * scale, cx + 8 * scale, headY - 54 * scale,
                cx + headR, headY - 22 * scale);
        hair.lineTo(cx + 26 * scale, headY - 6 * scale);
        hair.lineTo(cx + 8 * scale, headY - 19 * scale);
        hair.lineTo(cx - 8 * scale, headY - 5 * scale);
        hair.lineTo(cx - 23 * scale, headY - 17 * scale);
        hair.closePath();
        g.setColor(new Color(30, 25, 24));
        g.fill(hair);
        if (fourArms) {
            // Pointed alien ears and a heavier brow make the transformed form
            // visually different from human Ben.
            g.setColor(skin);
            g.fill(new Polygon(new int[] { (int) (cx - headR + 4 * scale), (int) (cx - headR - 13 * scale),
                    (int) (cx - headR + 12 * scale) },
                    new int[] { (int) (headY - 3 * scale), (int) (headY - 13 * scale), (int) (headY + 12 * scale) }, 3));
            g.fill(new Polygon(new int[] { (int) (cx + headR - 4 * scale), (int) (cx + headR + 13 * scale),
                    (int) (cx + headR - 12 * scale) },
                    new int[] { (int) (headY - 3 * scale), (int) (headY - 13 * scale), (int) (headY + 12 * scale) }, 3));
            g.setColor(new Color(85, 28, 25));
            g.fill(new Rectangle2D.Double(cx - 21 * scale, headY - 3 * scale, 42 * scale, 8 * scale));
            g.setColor(new Color(45, 18, 20));
            g.setStroke(new BasicStroke((float) (4 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx - 29 * scale, headY - 17 * scale, cx - 13 * scale, headY - 25 * scale));
            g.draw(new Line2D.Double(cx - 10 * scale, headY - 25 * scale, cx + 7 * scale, headY - 17 * scale));
            g.draw(new Line2D.Double(cx + 9 * scale, headY - 25 * scale, cx + 27 * scale, headY - 16 * scale));
        }
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 16 * scale, headY - 2 * scale, 8 * scale, 12 * scale));
        g.fill(new Ellipse2D.Double(cx + 8 * scale, headY - 2 * scale, 8 * scale, 12 * scale));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(cx - 13 * scale, headY + 1 * scale, 4 * scale, 7 * scale));
        g.fill(new Ellipse2D.Double(cx + 11 * scale, headY + 1 * scale, 4 * scale, 7 * scale));
        g.setStroke(new BasicStroke((float) (2 * scale)));
        if (fourArms) {
            g.setColor(new Color(75, 22, 22));
            g.draw(new Line2D.Double(cx - 9 * scale, headY + 18 * scale, cx + 9 * scale, headY + 18 * scale));
            g.fill(new Ellipse2D.Double(cx - 22 * scale, headY + 9 * scale, 5 * scale, 4 * scale));
            g.fill(new Ellipse2D.Double(cx + 17 * scale, headY + 9 * scale, 5 * scale, 4 * scale));
        } else {
            g.draw(new Arc2D.Double(cx - 10 * scale, headY + 14 * scale, 20 * scale, 11 * scale, 200, 140,
                    Arc2D.OPEN));
        }
        // Omnitrix sits on the wrist, not on the stomach.
        double watchX = fourArms ? cx - 53 * scale : cx - 77 * scale;
        double watchY = fourArms ? cy + 23 * scale : cy + 30 * scale;
        drawOmnitrix(g, watchX, watchY, 10 * scale);
    }

    void drawCharacterArm(Graphics2D g, double x1, double y1, double x2, double y2, Color skin, double scale) {
        g.setColor(skin);
        float armWidth = (skin.getGreen() < 100) ? 24f : 19f;
        g.setStroke(new BasicStroke((float) (armWidth * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.setColor(skin.brighter());
        if (skin.getGreen() < 100) {
            g.fill(new RoundRectangle2D.Double(x2 - 15 * scale, y2 - 13 * scale, 30 * scale, 25 * scale, 9, 9));
            g.setColor(new Color(111, 32, 29));
            g.setStroke(new BasicStroke((float) (2 * scale)));
            for (int i = -1; i <= 1; i++)
                g.draw(new Line2D.Double(x2 + i * 5 * scale, y2 - 7 * scale, x2 + i * 5 * scale, y2 + 6 * scale));
        } else {
            g.fill(new Ellipse2D.Double(x2 - 13 * scale, y2 - 13 * scale, 26 * scale, 26 * scale));
        }
    }

    void drawOmnitrix(Graphics2D g, double x, double y, double radius) {
        g.setColor(new Color(20, 30, 25));
        g.fill(new Ellipse2D.Double(x - radius - 3, y - radius - 3, 2 * radius + 6, 2 * radius + 6));
        g.setColor(new Color(62, 238, 76));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(x - radius * 0.52, y - radius * 0.52, radius * 1.04, radius * 1.04));
        g.setColor(new Color(170, 255, 125));
        g.fill(new Ellipse2D.Double(x - radius * 0.19, y - radius * 0.19, radius * 0.38, radius * 0.38));
    }

    void drawOmnitrixTap(Graphics2D g, long elapsed) {
        double t = clamp01(elapsed / 1250.0);
        double watchX = 237;
        double watchY = 344;
        g.setColor(new Color(125, 255, 112, 180));
        g.setStroke(new BasicStroke(3));
        g.draw(new Ellipse2D.Double(watchX - 18 - 3 * t, watchY - 18 - 3 * t,
                36 + 6 * t, 36 + 6 * t));
        if (elapsed > 250) {
            g.setColor(new Color(210, 255, 170, 210));
            g.draw(new Line2D.Double(watchX - 24, watchY - 8, watchX - 15, watchY - 2));
            g.draw(new Line2D.Double(watchX + 15, watchY - 2, watchX + 24, watchY - 8));
        }
    }

    void drawDoraemonGadgetScene(Graphics2D g2, long elapsed) {
        beginStoryScreen(g2, new Color(55, 160, 220), new Color(22, 40, 120));
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        for (int i = 0; i < 9; i++) {
            g.setColor(new Color(255, 255, 255, 170));
            int sx = screenX + 25 + (i * 47) % 365;
            int sy = screenY + 25 + (i * 31) % 160;
            g.fill(new Ellipse2D.Double(sx, sy, 4, 4));
        }

        double p = clamp01(elapsed / (double) DORAEMON_SCENE_MS);
        drawDoraemon(g, 226, 300, 0.92, p);

        // Anywhere Door comes out of the pocket and opens.
        double doorP = clamp01((p - 0.28) / 0.52);
        int doorX = 318;
        int doorY = 238 - (int) (18 * doorP);
        int doorW = (int) (76 * doorP);
        if (doorW > 2) {
            g.setColor(new Color(201, 47, 133));
            g.fill(new RoundRectangle2D.Double(doorX, doorY, doorW, 133, 10, 10));
            g.setColor(new Color(255, 153, 205));
            g.fill(new RoundRectangle2D.Double(doorX + 7, doorY + 8, Math.max(1, doorW - 14), 117, 7, 7));
            g.setColor(new Color(55, 125, 200));
            g.fill(new RoundRectangle2D.Double(doorX + 13, doorY + 15, Math.max(1, doorW - 26), 103, 5, 5));
            g.setColor(new Color(255, 210, 232));
            g.setStroke(new BasicStroke(3));
            g.draw(new RoundRectangle2D.Double(doorX + 13, doorY + 15, Math.max(1, doorW - 26), 103, 5, 5));
            g.setColor(new Color(255, 225, 75));
            g.fill(new Ellipse2D.Double(doorX + doorW - 18, doorY + 65, 8, 8));
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("DORAEMON!", 198, 166);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Anywhere Door", 334, 392);
        g.dispose();
    }

    void drawDoraemon(Graphics2D g, int cx, int cy, double scale, double p) {
        Color blue = new Color(37, 155, 224);
        Color blueEdge = new Color(22, 91, 151);
        double headR = 66 * scale;

        // Ground shadow and tail are behind the body.
        g.setColor(new Color(0, 0, 0, 75));
        g.fill(new Ellipse2D.Double(cx - 84 * scale, cy + 100 * scale, 168 * scale, 18 * scale));
        g.setColor(new Color(218, 45, 48));
        g.fill(new Ellipse2D.Double(cx + 44 * scale, cy + 54 * scale, 22 * scale, 22 * scale));

        // Blue body with a clearly separate white belly and two white feet.
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

        // Arms are attached to the body before the large head is drawn.
        g.setColor(blue);
        g.setStroke(new BasicStroke((float) (18 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 48 * scale, cy + 42 * scale, cx - 73 * scale, cy + 73 * scale));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 87 * scale, cy + 63 * scale, 25 * scale, 25 * scale));
        g.setColor(blueEdge);
        g.setStroke(new BasicStroke((float) (2 * scale)));
        g.draw(new Ellipse2D.Double(cx - 87 * scale, cy + 63 * scale, 25 * scale, 25 * scale));

        double reach = clamp01((p - 0.18) / 0.55);
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

        // The head is one large blue circle with a separate white face mask.
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

        // Large eyes, red nose, whiskers, and open smiling mouth.
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

        // Red collar, yellow bell, and the white front pocket are on the body.
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

        // The Specium beam grows from Ultraman's crossed arms.
        double beam = clamp01((p - 0.22) / 0.55);
        if (beam > 0) {
            // The beam starts exactly at the two crossed wrists drawn in
            // drawUltraman, so it can never float above the hands.
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
        // Draw the hero after the beam so the crossed wrists remain visible
        // on top of the light, exactly where the beam is emitted.
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

        // Specium pose: both forearms cross in front of the chest.
        Color armEdge = new Color(77, 87, 103);
        g.setColor(armEdge);
        g.setStroke(new BasicStroke((float) (24 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // One arm sweeps across the chest and the other bends upward from the
        // lower right. This is the recognizable crossed-wrist Specium pose.
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

    // A code-drawn approximation of the supplied seminar poster.
    // It deliberately does not read or display the original JPG file.
    void drawSeminarPoster(Graphics2D g2, long elapsed) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        g.setPaint(new GradientPaint(screenX, screenY, new Color(70, 190, 225), screenX, screenY + screenH,
                new Color(245, 165, 25)));
        g.fillRect(screenX, screenY, screenW, screenH);

        // Thai-style ornamental pattern and gold curtain curves.
        for (int x = screenX - 8; x < screenX + screenW + 18; x += 26) {
            for (int y = screenY + 5; y < screenY + screenH; y += 30) {
                g.setColor(new Color(255, 245, 170, 100));
                g.setStroke(new BasicStroke(1));
                g.draw(new Arc2D.Double(x, y, 16, 22, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(x + 5, y + 5, 7, 12, 200, 140, Arc2D.OPEN));
            }
        }
        g.setColor(new Color(255, 240, 170, 190));
        g.setStroke(new BasicStroke(9));
        g.draw(new CubicCurve2D.Double(90, 143, 155, 115, 225, 143, 275, 126));
        g.draw(new CubicCurve2D.Double(365, 126, 430, 145, 488, 112, 535, 145));

        // Buddha, the praying presenter, gifts, temple, and Naga ornaments.
        // They are layered in the same order as the reference poster.
        drawPosterBuddha(g, 375, 287, 0.86);
        drawPosterGifts(g, 286, 403);
        drawPosterNaga(g, 108, 375, false);
        drawPosterNaga(g, 514, 375, true);
        drawPosterPerson(g, 455, 170, 0.96);

        // Large text from the poster, drawn as text rather than an image.
        drawPosterText(g, "งานพิธีบุญใหญ่สัมนามีเดีย", 280, 178, 19,
                new Color(8, 74, 160), Color.WHITE, 2);
        drawPosterText(g, "มีเดียรวมใจ", 215, 222, 25,
                new Color(255, 248, 220), new Color(198, 130, 18), 3);
        drawPosterText(g, "สานสายสัมพันธ์", 215, 262, 21,
                new Color(128, 54, 15), new Color(255, 240, 160), 2);
        drawPosterText(g, "ณ ห้องมีเดีย ชั้น ๓ อาคารพระจอมเกล้า", 220, 299, 14,
                Color.WHITE, new Color(222, 110, 155), 1);
        drawPosterText(g, "เสาร์ ๒๗", 215, 338, 23,
                new Color(245, 255, 240), new Color(58, 188, 81), 2);
        drawPosterText(g, "มิถุนายน ๒๕๖๙", 215, 363, 16,
                new Color(245, 255, 240), new Color(58, 188, 81), 1);

        g.setColor(new Color(20, 170, 85));
        g.fill(new Rectangle2D.Double(screenX, screenY + screenH - 19, screenW, 19));
        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(3));
        g.draw(new CubicCurve2D.Double(screenX, screenY + screenH - 31, 190, screenY + screenH - 9,
                370, screenY + screenH - 38, screenX + screenW, screenY + screenH - 16));
        g.dispose();
        drawMemoriesTitle(g2, 1.0);
    }

    void centerText(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }

    void drawPosterText(Graphics2D g, String text, int cx, int y, int size, Color fill, Color outline,
            int outlineSize) {
        g.setFont(new Font("Leelawadee UI", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        g.setColor(outline);
        for (int dx = -outlineSize; dx <= outlineSize; dx++) {
            for (int dy = -outlineSize; dy <= outlineSize; dy++) {
                if (dx * dx + dy * dy <= outlineSize * outlineSize + 1)
                    g.drawString(text, x + dx, y + dy);
            }
        }
        g.setColor(fill);
        g.drawString(text, x, y);
    }

    void drawPosterBuddha(Graphics2D g, int cx, int cy, double s) {
        Color gold = new Color(183, 125, 24);
        g.setColor(new Color(131, 78, 13, 130));
        g.fill(new Ellipse2D.Double(cx - 63 * s, cy - 120 * s, 126 * s, 220 * s));
        g.setColor(new Color(206, 146, 30));
        g.fill(new Ellipse2D.Double(cx - 31 * s, cy - 100 * s, 62 * s, 62 * s));
        g.setColor(new Color(235, 187, 55));
        g.fill(new Ellipse2D.Double(cx - 8 * s, cy - 130 * s, 16 * s, 34 * s));
        g.setColor(gold);
        g.fill(new RoundRectangle2D.Double(cx - 52 * s, cy - 42 * s, 104 * s, 104 * s, 30, 30));
        g.setColor(new Color(211, 158, 35));
        g.fill(new Ellipse2D.Double(cx - 38 * s, cy - 78 * s, 15 * s, 24 * s));
        g.fill(new Ellipse2D.Double(cx + 23 * s, cy - 78 * s, 15 * s, 24 * s));
        g.setStroke(new BasicStroke((float) (18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 38 * s, cy - 20 * s, cx - 69 * s, cy + 73 * s));
        g.draw(new Line2D.Double(cx + 38 * s, cy - 20 * s, cx + 69 * s, cy + 73 * s));
        g.setColor(new Color(91, 54, 14));
        g.fill(new Ellipse2D.Double(cx - 14 * s, cy - 74 * s, 8 * s, 8 * s));
        g.fill(new Ellipse2D.Double(cx + 6 * s, cy - 74 * s, 8 * s, 8 * s));
        g.setStroke(new BasicStroke((float) (2 * s)));
        g.draw(new Line2D.Double(cx - 17 * s, cy - 81 * s, cx - 5 * s, cy - 84 * s));
        g.draw(new Line2D.Double(cx + 5 * s, cy - 84 * s, cx + 17 * s, cy - 81 * s));
        g.setColor(new Color(151, 96, 16));
        g.draw(new Line2D.Double(cx, cy - 70 * s, cx - 3 * s, cy - 55 * s));
        g.setStroke(new BasicStroke((float) (3 * s)));
        g.draw(new Arc2D.Double(cx - 11 * s, cy - 59 * s, 22 * s, 9 * s, 200, 140, Arc2D.OPEN));
        g.setColor(new Color(246, 201, 78));
        g.draw(new Line2D.Double(cx - 40 * s, cy - 9 * s, cx + 40 * s, cy - 9 * s));
        g.draw(new Line2D.Double(cx - 42 * s, cy + 8 * s, cx + 42 * s, cy + 8 * s));
        g.setColor(new Color(249, 202, 74));
        g.setStroke(new BasicStroke((float) (10 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 28 * s, cy + 31 * s, cx + 8 * s, cy + 40 * s));
        g.draw(new Line2D.Double(cx + 28 * s, cy + 31 * s, cx - 8 * s, cy + 40 * s));
    }

    void drawPosterPerson(Graphics2D g, int cx, int top, double s) {
        int headR = (int) (26 * s);
        int headY = top + headR;
        Color skin = new Color(224, 164, 116);
        // Gray, side-swept hair and a separate face shape match the reference
        // portrait more closely than a single flat oval.
        g.setColor(new Color(132, 133, 126));
        g.fill(new Ellipse2D.Double(cx - 30 * s, top - 5 * s, 60 * s, 32 * s));
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(cx - headR - 4 * s, top + 20 * s, 10 * s, 18 * s));
        g.fill(new Ellipse2D.Double(cx + headR - 6 * s, top + 20 * s, 10 * s, 18 * s));
        Shape personFace = new Ellipse2D.Double(cx - headR, top, 2 * headR, 2 * headR);
        g.setPaint(new GradientPaint(cx - headR, top, new Color(244, 190, 142), cx + headR,
                top + 2 * headR, new Color(205, 133, 92)));
        g.fill(personFace);
        g.setColor(new Color(145, 88, 62));
        g.setStroke(new BasicStroke((float) (2 * s)));
        g.draw(personFace);
        g.setColor(new Color(117, 118, 112));
        g.fill(new Arc2D.Double(cx - headR, top - 7 * s, 2 * headR, 36 * s, 0, 180, Arc2D.PIE));
        g.fill(new Ellipse2D.Double(cx - 27 * s, top + 11 * s, 12 * s, 19 * s));
        g.setColor(new Color(235, 235, 226, 180));
        g.setStroke(new BasicStroke((float) (3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 23 * s, top + 10 * s, cx - 7 * s, top - 1 * s));
        g.draw(new Line2D.Double(cx - 11 * s, top + 8 * s, cx + 8 * s, top - 2 * s));
        g.setColor(new Color(67, 47, 39));
        g.setStroke(new BasicStroke((float) (3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 16 * s, top + 25 * s, cx - 5 * s, top + 22 * s));
        g.draw(new Line2D.Double(cx + 5 * s, top + 22 * s, cx + 16 * s, top + 25 * s));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 14 * s, top + 28 * s, 11 * s, 8 * s));
        g.fill(new Ellipse2D.Double(cx + 3 * s, top + 28 * s, 11 * s, 8 * s));
        g.setColor(new Color(50, 35, 30));
        g.fill(new Ellipse2D.Double(cx - 10 * s, top + 29 * s, 4 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 6 * s, top + 29 * s, 4 * s, 6 * s));
        g.setColor(new Color(245, 157, 126, 80));
        g.fill(new Ellipse2D.Double(cx - 22 * s, top + 39 * s, 10 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 12 * s, top + 39 * s, 10 * s, 6 * s));
        Path2D nose = new Path2D.Double();
        nose.moveTo(cx, top + 33 * s);
        nose.curveTo(cx - 4 * s, top + 43 * s, cx - 3 * s, top + 46 * s, cx + 3 * s, top + 45 * s);
        g.setColor(new Color(196, 126, 88));
        g.draw(nose);
        g.setColor(new Color(150, 48, 45));
        g.setStroke(new BasicStroke((float) (2 * s)));
        g.draw(new Arc2D.Double(cx - 10 * s, top + 43 * s, 20 * s, 10 * s, 200, 140, Arc2D.OPEN));
        // Black polo body with a bright white center panel, as in the poster.
        g.setColor(new Color(22, 22, 22));
        g.fill(new RoundRectangle2D.Double(cx - 53 * s, top + 48 * s, 106 * s, 253 * s, 18, 18));
        g.setColor(Color.WHITE);
        Path2D shirt = new Path2D.Double();
        shirt.moveTo(cx - 31 * s, top + 54 * s);
        shirt.lineTo(cx - 20 * s, top + 43 * s);
        shirt.lineTo(cx + 20 * s, top + 43 * s);
        shirt.lineTo(cx + 32 * s, top + 54 * s);
        shirt.lineTo(cx + 37 * s, top + 250 * s);
        shirt.lineTo(cx - 38 * s, top + 250 * s);
        shirt.closePath();
        g.fill(shirt);
        g.setColor(Color.BLACK);
        g.fill(new Polygon(new int[] { (int) (cx - 22 * s), (int) (cx - 4 * s), (int) (cx - 20 * s) },
                new int[] { (int) (top + 44 * s), (int) (top + 69 * s), (int) (top + 50 * s) }, 3));
        g.fill(new Polygon(new int[] { (int) (cx + 22 * s), (int) (cx + 4 * s), (int) (cx + 20 * s) },
                new int[] { (int) (top + 44 * s), (int) (top + 69 * s), (int) (top + 50 * s) }, 3));
        g.setColor(new Color(22, 22, 22));
        g.setStroke(new BasicStroke((float) (8 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 30 * s, top + 59 * s, cx, top + 94 * s));
        g.draw(new Line2D.Double(cx + 30 * s, top + 59 * s, cx, top + 94 * s));
        g.setColor(skin);
        g.setStroke(new BasicStroke((float) (14 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 39 * s, top + 72 * s, cx - 8 * s, top + 154 * s));
        g.draw(new Line2D.Double(cx + 39 * s, top + 72 * s, cx + 8 * s, top + 154 * s));
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(cx - 15 * s, top + 143 * s, 30 * s, 28 * s));
        g.setColor(new Color(183, 113, 78));
        g.setStroke(new BasicStroke((float) (2 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= 1; i++) {
            g.draw(new Line2D.Double(cx + i * 5 * s, top + 147 * s, cx + i * 5 * s, top + 160 * s));
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke((float) (5 * s)));
        g.draw(new Ellipse2D.Double(cx + 17 * s, top + 115 * s, 25 * s, 27 * s));
        g.setColor(new Color(62, 62, 62));
        g.fill(new Ellipse2D.Double(cx + 20 * s, top + 118 * s, 19 * s, 21 * s));
        g.setColor(new Color(206, 140, 78));
        g.fill(new Ellipse2D.Double(cx + 25 * s, top + 123 * s, 9 * s, 11 * s));
        // Small shirt emblem, echoing the reference image.
        g.setColor(new Color(55, 55, 55));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (22 * s)));
        g.drawString("S", (float) (cx + 18 * s), (float) (top + 92 * s));
        g.setColor(new Color(232, 105, 41));
        g.fill(new Rectangle2D.Double(cx + 37 * s, top + 210 * s, 14 * s, 43 * s));
    }

    void drawPosterGifts(Graphics2D g, int x, int y) {
        // A small temple silhouette sits behind the gift baskets.
        g.setColor(new Color(107, 70, 38));
        g.fill(new Rectangle2D.Double(365, y - 45, 47, 45));
        g.setColor(new Color(238, 188, 52));
        g.fill(new Polygon(new int[] { 355, 388, 422 }, new int[] { y - 45, y - 73, y - 45 }, 3));
        g.setColor(new Color(255, 223, 107));
        g.fill(new Polygon(new int[] { 359, 388, 417 }, new int[] { y - 52, y - 81, y - 52 }, 3));
        g.setColor(new Color(66, 47, 34));
        g.fill(new Rectangle2D.Double(382, y - 27, 12, 27));
        g.setColor(new Color(220, 222, 211));
        g.fill(new Rectangle2D.Double(370, y - 31, 8, 31));
        g.fill(new Rectangle2D.Double(398, y - 31, 8, 31));

        // Gift baskets and recognizable small product packages.
        for (int i = 0; i < 7; i++) {
            int bx = x - 62 + i * 20;
            int by = y - (i % 3) * 12;
            g.setColor(new Color(220 - i * 15, 70 + i * 18, 45 + i * 20));
            g.fill(new Rectangle2D.Double(bx, by, 25, 25));
            g.setColor(new Color(255, 210, 65));
            g.fill(new Rectangle2D.Double(bx + 10, by, 4, 25));
            g.setColor(new Color(245, 235, 210, 180));
            g.draw(new Rectangle2D.Double(bx + 4, by + 6, 17, 8));
        }
        g.setColor(new Color(112, 73, 35));
        g.fill(new Ellipse2D.Double(x - 75, y + 4, 160, 18));
        g.setColor(new Color(222, 166, 54));
        g.setStroke(new BasicStroke(3));
        g.draw(new Arc2D.Double(x - 72, y - 8, 154, 44, 0, -180, Arc2D.OPEN));
        for (int i = 0; i < 6; i++) {
            int bx = x - 48 + i * 18;
            g.setColor(new Color(245, 245, 225));
            g.fill(new RoundRectangle2D.Double(bx, y + 18, 12, 24 - (i % 2) * 5, 3, 3));
            g.setColor(new Color(65, 140, 92));
            g.fill(new Rectangle2D.Double(bx + 2, y + 25, 8, 5));
        }
    }

    void drawPosterNaga(Graphics2D g, int x, int y, boolean mirror) {
        g.setColor(new Color(14, 78, 65, 235));
        g.setStroke(new BasicStroke(17, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double d = mirror ? -1 : 1;
        CubicCurve2D body = new CubicCurve2D.Double(x, y + 80, x + d * 35, y + 20, x + d * 32, y - 25, x, y - 70);
        g.draw(body);
        g.setColor(new Color(47, 146, 107));
        g.setStroke(new BasicStroke(4));
        g.draw(new CubicCurve2D.Double(x + d * 7, y + 75, x + d * 43, y + 20, x + d * 41, y - 25, x + d * 7, y - 72));
        for (int i = 0; i < 5; i++) {
            int sy = y + 46 - i * 25;
            g.setColor(new Color(224, 182, 46));
            g.fill(new Arc2D.Double(x - 20 + d * 2, sy, 40, 22, mirror ? 20 : 160, 160, Arc2D.CHORD));
        }
        Path2D head = new Path2D.Double();
        head.moveTo(x - 18, y - 77);
        head.curveTo(x - 22, y - 94, x - 8, y - 105, x, y - 91);
        head.curveTo(x + 8, y - 105, x + 22, y - 94, x + 18, y - 77);
        head.closePath();
        g.setColor(new Color(225, 170, 45));
        g.fill(head);
        g.setColor(new Color(242, 221, 111));
        g.fill(new Polygon(new int[] { x - 12, x - 3, x - 16 }, new int[] { y - 95, y - 114, y - 102 }, 3));
        g.fill(new Polygon(new int[] { x + 12, x + 3, x + 16 }, new int[] { y - 95, y - 114, y - 102 }, 3));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - 9, y - 83, 6, 6));
        g.fill(new Ellipse2D.Double(x + 3, y - 83, 6, 6));
        g.setColor(new Color(215, 50, 45));
        g.fill(new Arc2D.Double(x - 10, y - 73, 20, 18, 180, 180, Arc2D.PIE));
        g.setColor(Color.WHITE);
        g.fill(new Polygon(new int[] { x - 6, x - 2, x + 1 }, new int[] { y - 63, y - 54, y - 63 }, 3));
        g.fill(new Polygon(new int[] { x + 1, x + 5, x + 8 }, new int[] { y - 63, y - 54, y - 63 }, 3));
    }

    // ================= Reusable drawing pieces =================

    /**
     * The disc (140px across) is taller than the player panel (90px), so even once
     * drawPlayerFront occludes everything overlapping the panel, the very bottom of
     * the disc can still poke out below the panel's bottom edge, over the floor.
     * Repainting the floor strip right below the panel papers over that sliver --
     * cheap and correct, since that's exactly the floor's own color already there.
     */
    void restoreFloorBelowPlayer(Graphics2D g2, int playerBottomY) {
        g2.setColor(new Color(60, 48, 40));
        g2.fill(new Rectangle2D.Double(0, playerBottomY, W, H - playerBottomY));
    }

    void drawRoomBackground(Graphics2D g2) {
        // wall
        g2.setColor(new Color(40, 36, 48));
        g2.fillRect(0, 0, W, H);
        // floor
        g2.setColor(new Color(60, 48, 40));
        g2.fillRect(0, 460, W, H - 460);
        Path2D floorLine = new Path2D.Double();
        floorLine.moveTo(0, 460);
        floorLine.curveTo(150, 450, 450, 470, 600, 460);
        g2.setColor(new Color(80, 64, 54));
        g2.setStroke(new BasicStroke(3));
        g2.draw(floorLine);

        // a soft curved shelf line behind the player, using CubicCurve2D
        CubicCurve2D shelf = new CubicCurve2D.Double(60, 460, 200, 440, 400, 440, 540, 460);
        g2.setColor(new Color(90, 76, 60));
        g2.setStroke(new BasicStroke(6));
        g2.draw(shelf);
    }

    /**
     * Solid panel body + the slot rendered as a plain dark opening. Drawn BEFORE
     * the disc, so that when no disc is behind it yet, the slot just reads as an
     * empty dark slit (as before). The slot now sits near the TOP edge of the
     * panel (y + 8) instead of mid-panel, so the disc visibly enters near the
     * top of the player before disappearing behind it.
     */
    void drawPlayerBase(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(25, 25, 28));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        g2.setColor(Color.BLACK);
        g2.fill(new RoundRectangle2D.Double(x + 60, y + 8, w - 120, 16, 8, 8));
    }

    /**
     * Redraws the panel ON TOP of whatever was just painted (a disc mid-insertion),
     * but with the slot opening cut out of the fill using constructive area
     * geometry
     * (Area.subtract). Anything drawn before this call gets hidden wherever it
     * overlaps the solid panel, and stays visible only through the slot cut-out --
     * this is what makes a sliding disc look like it's genuinely going INSIDE the
     * player instead of just floating in front of it.
     * Slot cut-out matches drawPlayerBase: near the top edge of the panel (y + 8).
     */
    void drawPlayerFront(Graphics2D g2, int x, int y, int w, int h) {
        Area panel = new Area(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        Area slotHole = new Area(new RoundRectangle2D.Double(x + 60, y + 8, w - 120, 16, 8, 8));
        panel.subtract(slotHole);

        g2.setColor(new Color(25, 25, 28));
        g2.fill(panel);

        g2.setColor(new Color(60, 60, 66));
        g2.setStroke(new BasicStroke(2));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        // small buttons using lines/circles (moved down slightly so they clear the
        // slot, which now sits higher up near the panel's top edge)
        g2.setColor(new Color(90, 90, 100));
        g2.fill(new Ellipse2D.Double(x + w - 40, y + 62, 16, 16));
        g2.fill(new Ellipse2D.Double(x + w - 65, y + 62, 16, 16));

        // brand line detail (QuadCurve2D), also nudged down to sit below the slot
        QuadCurve2D detail = new QuadCurve2D.Double(x + 15, y + 66, x + 40, y + 80, x + 65, y + 66);
        g2.setColor(new Color(70, 70, 78));
        g2.setStroke(new BasicStroke(2));
        g2.draw(detail);
    }

    /** Draws the CD/DVD disc using ONLY the midpoint circle algorithm output. */
    void drawDisc(Graphics2D g2, int cx, int cy, int radius, double shine) {
        // Outer edge via midpoint circle algorithm (plotted as tiny filled squares)
        List<Point> outer = midpointCircle(cx, cy, radius);
        g2.setColor(new Color(210, 215, 225));
        for (Point p : outer) {
            g2.fillRect(p.x, p.y, 2, 2);
        }
        // Fill the disc body by drawing successively smaller midpoint circles
        // (keeps everything within "midpoint circle algorithm" rather than fillOval)
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
        // rainbow shine arcs (curves) to sell the "shiny disc" look
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
        // center hole via midpoint circle algorithm too
        List<Point> hole = midpointCircle(cx, cy, Math.max(6, radius / 8));
        g2.setColor(new Color(20, 20, 24));
        for (Point p : hole)
            g2.fillRect(p.x, p.y, 2, 2);
        // fill center hole solid
        int hr = Math.max(6, radius / 8);
        for (int r = hr - 1; r > 0; r--) {
            List<Point> ring = midpointCircle(cx, cy, r);
            g2.setColor(new Color(20, 20, 24));
            for (Point p : ring)
                g2.fillRect(p.x, p.y, 2, 2);
        }
    }

    void drawTVFrame(Graphics2D g2) {
        // outer TV body
        g2.setColor(new Color(35, 32, 30));
        g2.fill(new RoundRectangle2D.Double(50, 100, 500, 380, 20, 20));
        g2.setColor(new Color(60, 55, 50));
        g2.setStroke(new BasicStroke(3));
        g2.draw(new RoundRectangle2D.Double(50, 100, 500, 380, 20, 20));

        // stand (lines)
        g2.setStroke(new BasicStroke(6));
        g2.setColor(new Color(35, 32, 30));
        g2.draw(new Line2D.Double(220, 480, 180, 540));
        g2.draw(new Line2D.Double(380, 480, 420, 540));
        g2.draw(new Line2D.Double(150, 540, 450, 540));

        // small speaker grille dots (lines)
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(70, 65, 60));
        for (int i = 0; i < 6; i++) {
            g2.draw(new Line2D.Double(65, 460 + i * 6, 80, 460 + i * 6));
        }
    }

    /** Classic bouncing "DVD" wordmark, built from Path2D letter shapes. */
    void drawDVDLogo(Graphics2D g2, double x, double y, Color c) {
        g2.setColor(c);
        Font old = g2.getFont();
        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        g2.drawString("DVD", (float) x + 8, (float) y + 30);

        // little TV-play triangle icon beside it, drawn via Path2D (a "curve/line"
        // shape)
        Path2D tri = new Path2D.Double();
        tri.moveTo(x + logoW - 26, y + 10);
        tri.lineTo(x + logoW - 26, y + 36);
        tri.lineTo(x + logoW - 6, y + 23);
        tri.closePath();
        g2.fill(tri);

        // underline curve
        QuadCurve2D underline = new QuadCurve2D.Double(x, y + 40, x + logoW / 2.0, y + 46, x + logoW, y + 40);
        g2.setStroke(new BasicStroke(2));
        g2.draw(underline);
        g2.setFont(old);
    }

    void drawMemoriesTitle(Graphics2D g2, double alpha) {
        Graphics2D gg = (Graphics2D) g2.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        gg.setColor(Color.WHITE);
        gg.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fm = gg.getFontMetrics();
        String s = "MY MEMORY";
        int tw = fm.stringWidth(s);
        gg.drawString(s, (W - tw) / 2f, 90);
        gg.dispose();
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
