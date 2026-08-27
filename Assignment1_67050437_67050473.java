import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Assignment1_67050437_67050473 extends JPanel {

    // ---------- Canvas ----------
    static final int W = 600, H = 600;

    // ---------- Timing (milliseconds) ----------
    static final int FPS = 60;
    static final int FRAME_DELAY = 1000 / FPS;

    static final int T_INSERT_START   = 0;
    static final int T_INSERT_END     = 2500;   // hand pushes disc into slot
    static final int T_LOADING_END    = 3500;   // player "reads" disc, light blinks
    static final int T_TV_CUT_END     = 3900;   // quick white-flash transition to TV
    static final int T_TV_STATIC_END  = 5400;   // old CRT "just switched on" static/snow, ~1.5s
    static final int T_TV_BLACK_END   = 6000;   // static settles to black before the logo appears
    static final int HOLD_AFTER_CORNER_MS = 1400; // freeze time once a perfect corner hit lands

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
    int rangeX, rangeY;          // travel distance before hitting an edge, in each axis
    int stepX = 3, stepY = 3;    // pixels moved per frame (equal speed = classic 45-degree DVD motion; chosen so the logo bounces ~7-8 times off the walls before landing exactly in a corner -- see initBounceGeometry)
    int posX, posY;              // integer offset within [0, rangeX] / [0, rangeY]
    int dirX = 1, dirY = 1;      // +1 or -1
    double logoX = 90, logoY = 140; // derived screen position (kept as double for drawing); starts at top-left corner
    Color logoColor = new Color(230, 200, 40);
    List<Color> palette = List.of(
            new Color(230, 200, 40),   // yellow
            new Color(230, 70, 70),    // red
            new Color(70, 160, 230),   // blue
            new Color(90, 200, 110),   // green
            new Color(200, 90, 220)    // purple
    );
    int cornerHitFlashFrames = 0;

    // Corner-hit / freeze / restart state (bounce phase only)
    boolean cornerLocked = false;   // true once logo has landed exactly in a corner
    long cornerLockedAtMs = -1;     // wall-clock time (relative to startTime) when it locked

    // TV inner screen bounds (set once we know TV geometry)
    final int screenX = 90, screenY = 140, screenW = 420, screenH = 300;

    // Reused every frame for the TV static effect (avoids allocating an image 60x/sec).
    // Chunky low-res buffer (4px "grain") upscaled with nearest-neighbor -- looks like
    // real analog snow instead of a smooth noise gradient.
    static final int STATIC_CELL = 4;
    final BufferedImage staticImg = new BufferedImage(screenW / STATIC_CELL, screenH / STATIC_CELL, BufferedImage.TYPE_INT_RGB);
    final int[] staticPixels = new int[(screenW / STATIC_CELL) * (screenH / STATIC_CELL)];
    final Random staticRng = new Random();

    public Assignment1_67050437_67050473() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(18, 18, 22));
        initBounceGeometry();

        timer = new Timer(FRAME_DELAY, e -> {
            if (startTime < 0) startTime = System.currentTimeMillis();
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed >= T_TV_BLACK_END) {
                if (!cornerLocked) {
                    stepBounce(elapsed);
                } else if (elapsed - cornerLockedAtMs >= HOLD_AFTER_CORNER_MS) {
                    // held in the corner long enough -- restart the whole story from the top
                    resetAnimation();
                }
                if (cornerHitFlashFrames > 0) cornerHitFlashFrames--;
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
        posX = 0; posY = 0;
        dirX = 1; dirY = 1;
        logoX = screenX; logoY = screenY;
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
        if (hitX && hitY) {
            // the legendary perfect corner hit -- freeze the logo right here
            cornerHitFlashFrames = 20;
            cornerLocked = true;
            cornerLockedAtMs = elapsed;
        }
    }

    /** Moves one axis by its step and reflects at the boundaries; returns true if it bounced this frame. */
    boolean advanceAxisAndReportBounce(boolean isX) {
        int range = isX ? rangeX : rangeY;
        int step = isX ? stepX : stepY;
        int pos = isX ? posX : posY;
        int dir = isX ? dirX : dirY;

        pos += dir * step;
        boolean bounced = false;
        if (pos <= 0) { pos = 0; dir = 1; bounced = true; }
        else if (pos >= range) { pos = range; dir = -1; bounced = true; }

        if (isX) { posX = pos; dirX = dir; } else { posY = pos; dirY = dir; }
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

    // ---------------- Scene 4: old CRT static/snow (TV just switched on) ----------------
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

    // ---------------- Scene 5: static settles to black before the logo appears ----------------
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

    /** Chunky black-and-white noise, like real analog TV snow (upscaled from a low-res buffer). */
    void drawStaticNoise(Graphics2D g2, int x, int y, int w, int h) {
        for (int i = 0; i < staticPixels.length; i++) {
            int v = staticRng.nextInt(256);
            staticPixels[i] = (v << 16) | (v << 8) | v;
        }
        staticImg.setRGB(0, 0, staticImg.getWidth(), staticImg.getHeight(), staticPixels, 0, staticImg.getWidth());

        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(staticImg, x, y, w, h, null);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                oldInterp != null ? oldInterp : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // ---------------- Scene 6: TV + bouncing logo ----------------
    void drawBounceScene(Graphics2D g2, long elapsed) {
        g2.setColor(new Color(10, 10, 14));
        g2.fillRect(0, 0, W, H);

        drawTVFrame(g2);

        // screen background (dark navy, like a paused/blue screen glow)
        g2.setColor(new Color(8, 12, 30));
        g2.fill(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));

        // corner flash effect
        if (cornerHitFlashFrames > 0) {
            float alpha = cornerHitFlashFrames / 20f * 0.5f;
            g2.setColor(new Color(1f, 1f, 1f, alpha));
            g2.fill(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        }

        g2.setClip(new RoundRectangle2D.Double(screenX, screenY, screenW, screenH, 12, 12));
        drawDVDLogo(g2, logoX, logoY, logoColor);
        g2.setClip(null);

        // "MY MEMORIES" fades in once the logo has landed perfectly in a corner
        if (cornerLocked) {
            long sinceLock = elapsed - cornerLockedAtMs;
            double ft = clamp01(sinceLock / 500.0);
            drawMemoriesTitle(g2, ft);
        }
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
     * but with the slot opening cut out of the fill using constructive area geometry
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
        Color[] shineColors = {new Color(255,120,120,120), new Color(120,255,180,120), new Color(140,160,255,120)};
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
        for (Point p : hole) g2.fillRect(p.x, p.y, 2, 2);
        // fill center hole solid
        int hr = Math.max(6, radius / 8);
        for (int r = hr - 1; r > 0; r--) {
            List<Point> ring = midpointCircle(cx, cy, r);
            g2.setColor(new Color(20, 20, 24));
            for (Point p : ring) g2.fillRect(p.x, p.y, 2, 2);
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

        // little TV-play triangle icon beside it, drawn via Path2D (a "curve/line" shape)
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
        String s = "MY MEMORIES";
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

    // ================= main =================
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
}