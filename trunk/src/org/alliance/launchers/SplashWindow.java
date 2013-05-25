package org.alliance.launchers;

import org.alliance.Version;
import org.alliance.core.ResourceSingelton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;

/**
 * User: maciek
 * Date: 2004-sep-14
 * Time: 10:49:44
 */
public class SplashWindow extends Window implements Runnable, StartupProgressListener {

    private Image image;
    private BufferedImage bufferedImage;
    private String statusMessage = "";
    private Color borderColor = new Color(0f, 0f, 0f, 0.5f);
    private Font font = new Font("Dialog", 0, 10);

    public SplashWindow() throws Exception {
        super(new Frame());
        image = Toolkit.getDefaultToolkit().getImage(ResourceSingelton.getRl().getResource("gfx/splash.jpg"));

        MediaTracker mt = new MediaTracker(SplashWindow.this);
        mt.addImage(image, 0);
        mt.waitForAll();

        bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        bufferedImage.createGraphics();

        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(ss.width / 2 - image.getWidth(null) / 2, ss.height / 2 - image.getHeight(null) / 2);
        setSize(new Dimension(image.getWidth(null), image.getHeight(null)));

        setVisible(true);
        toFront();
        requestFocus();
    }

    @Override
    public void paint(Graphics frontG) {
        Graphics2D gBuffered = (Graphics2D) bufferedImage.getGraphics();
        Graphics2D gSplash = (Graphics2D) frontG;

        gBuffered.drawImage(image, 0, 0, null);
        gBuffered.setFont(font);

        gBuffered.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gBuffered.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int texty = bufferedImage.getHeight(null) - 4;
        drawBorderedStrings(gBuffered, statusMessage, 5, texty, Color.white, borderColor);
        String s = "Version " + Version.VERSION + " build (" + Version.BUILD_NUMBER + ")";
        drawBorderedStrings(gBuffered, s, bufferedImage.getWidth(null) - 5 - gBuffered.getFontMetrics().stringWidth(s), texty, Color.white, borderColor);

        gBuffered.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gBuffered.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        gSplash.drawImage(bufferedImage, 0, 0, null);
    }

    private void drawBorderedStrings(Graphics2D g, String s, int x, int y, Color color, Color bcolor) {
        //Border
        g.setColor(bcolor);
        for (int i = 1; i < 2; i++) {
            g.drawString(s, x - i, y - i);
            g.drawString(s, x, y - i);
            g.drawString(s, x + i, y - i);
            g.drawString(s, x - i, y + i);
            g.drawString(s, x, y + i);
            g.drawString(s, x + i, y + i);
            g.drawString(s, x - i, y);
            g.drawString(s, x + i, y);
        }
        //String
        g.setColor(color);
        g.drawString(s, x, y);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void run() {
        dispose();
    }

    @Override
    public void updateProgress(String message) {
        this.statusMessage = message + "...";
        if (getGraphics() != null) {
            paint(getGraphics());
        }
    }
}
