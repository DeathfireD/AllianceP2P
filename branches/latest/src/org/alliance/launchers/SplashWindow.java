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

/**
 * User: maciek
 * Date: 2004-sep-14
 * Time: 10:49:44
 */
public class SplashWindow extends Window implements Runnable, StartupProgressListener {

    private Image image;
    private String statusMessage = "";
    private int progressPercent = -1;

    public SplashWindow() throws Exception {
        super(new Frame());
        image = Toolkit.getDefaultToolkit().getImage(ResourceSingelton.getRl().getResource("gfx/splash.jpg"));
        MediaTracker mt = new MediaTracker(SplashWindow.this);
        mt.addImage(image, 0);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
        }
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();

        setLocation(ss.width / 2 - image.getWidth(null) / 2,
                ss.height / 2 - image.getHeight(null) / 2);
        setSize(new Dimension(image.getWidth(null), image.getHeight(null)));

        setVisible(true);
        toFront();
        requestFocus();
    }
    private int progressBarLength = 100, progressBarHeight = 8;

    @Override
    public void paint(Graphics frontG) {
        Graphics2D g = (Graphics2D) frontG;

        g.drawImage(image, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Arial Black, Arial", 0, 10));

        g.setColor(Color.white);
        int texty = image.getHeight(null) - 10;
        g.drawString(statusMessage, 10, texty);
        String s = "Version " + Version.VERSION + " build (" + Version.BUILD_NUMBER + ")";
        g.drawString(s, image.getWidth(null) - 10 - g.getFontMetrics().stringWidth(s), texty);

        if (progressPercent >= 0) {
            int a = progressPercent * 3 / 2;
            if (a > 70) {
                a = 70;
            }
            g.setColor(new Color(255, 255, 255, a));
            progressBarLength = image.getWidth(null) - 15 * 4;
            progressBarHeight = 20;
            int x = image.getWidth(null) / 2 - progressBarLength / 2;
            int y = image.getHeight(null) - 51;
            g.drawRect(x, y, progressBarLength, progressBarHeight);
            g.fillRect(x + 2, y + 2, (progressBarLength - 3) * progressPercent / 100, progressBarHeight - 3);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        if (getGraphics() != null) {
            paint(getGraphics());
        }
    }

    public void setProgressPercent(int i) {
        progressPercent = i;
    }

    @Override
    public void run() {
        setVisible(false);
        dispose();
    }

    @Override
    public void updateProgress(String message) {
        setStatusMessage(message + "...");
    }
}
