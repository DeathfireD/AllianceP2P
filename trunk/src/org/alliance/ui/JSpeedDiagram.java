package org.alliance.ui;

import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Bastvera
 */
public class JSpeedDiagram extends JPanel implements Runnable {

    private int maxValue;
    private int panelHeight;
    private int panelWidth;
    private ArrayList<Integer> valuesDownload = new ArrayList<Integer>();
    private ArrayList<Integer> valuesUpload = new ArrayList<Integer>();
    private ArrayList<Integer> yPositionDownload = new ArrayList<Integer>();
    private ArrayList<Integer> yPositionUpload = new ArrayList<Integer>();
    private ArrayList<Integer> xTimeScale = new ArrayList<Integer>();
    private final UISubsystem ui;
    private static final int HEIGTH_OFFSET = 3;
    private static final int TIME_SCALE_DIVIDER = 20;
    private static final Color TRANSPARENT_GREY = new Color(0.5f, 0.5f, 0.5f, 0.5f);

    JSpeedDiagram(UISubsystem ui) {
        this.ui = ui;
        this.setBackground(Color.BLACK);
        SubstanceThemeHelper.setColorization(this, new Double(1.0));
    }

    @Override
    public void run() {
        boolean alive = true;
        while (alive) {
            try {
                if (getSize().width != 0 && getSize().height != 0) {
                    panelHeight = getSize().height;
                    panelWidth = getSize().width;

                    maxValue = 2;

                    int valueDownload = (int) (ui.getCore().getNetworkManager().getBandwidthIn().getCPS() / 1024);
                    int valueUpload = (int) (ui.getCore().getNetworkManager().getBandwidthOut().getCPS() / 1024);

                    valuesDownload.add(valueDownload);
                    valuesUpload.add(valueUpload);

                    //Get max value
                    int valueChecked;
                    for (int i = 0; i < valuesDownload.size(); i++) {
                        valueChecked = valuesDownload.get(i);
                        if (maxValue < valueChecked) {
                            maxValue = valueChecked;
                        }
                        valueChecked = valuesUpload.get(i);
                        if (maxValue < valueChecked) {
                            maxValue = valueChecked;
                        }
                    }

                    //Rescale Height
                    yPositionDownload.add(0);
                    yPositionUpload.add(0);
                    double percent;
                    int position;
                    for (int i = 0; i < yPositionDownload.size(); i++) {
                        percent = (double) valuesDownload.get(i) / (double) maxValue;
                        position = (int) ((double) panelHeight * percent);
                        if (position < HEIGTH_OFFSET) {
                            position = HEIGTH_OFFSET;
                        }
                        yPositionDownload.set(i, position);

                        percent = (double) valuesUpload.get(i) / (double) maxValue;
                        position = (int) ((double) panelHeight * percent);
                        if (position < HEIGTH_OFFSET) {
                            position = HEIGTH_OFFSET;
                        }
                        yPositionUpload.set(i, position);
                    }

                    //Rescale Width
                    xTimeScale.add(0);
                    int xDivided = panelWidth / TIME_SCALE_DIVIDER;
                    for (int i = 0; i < xTimeScale.size(); i++) {
                        xTimeScale.set(i, xDivided * i);
                    }


                    //Move Diagram
                    if (xTimeScale.size() > TIME_SCALE_DIVIDER + 1) {
                        xTimeScale.remove(TIME_SCALE_DIVIDER + 1);
                        xTimeScale.set(xTimeScale.size() - 1, getSize().width);
                        yPositionDownload.remove(0);
                        yPositionUpload.remove(0);
                        valuesDownload.remove(0);
                        valuesUpload.remove(0);
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            repaint();
                        }
                    });
                }
                Thread.sleep(3000);

            } catch (InterruptedException ex) {
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(new Font("Dialog", Font.TRUETYPE_FONT, 10));

        g2d.setColor(TRANSPARENT_GREY);
        int div = 0;
        while (div * 10 < panelWidth) {
            div++;
            g2d.drawLine(div * 10, 0, div * 10, panelHeight);
        }
        div = 0;
        while (div * 10 < panelHeight) {
            div++;
            g2d.drawLine(0, div * 10, panelWidth, div * 10);
        }

        if (xTimeScale.size() > 1) {
            for (int i = 0; i < xTimeScale.size() - 1; i++) {
                g2d.setColor(Color.GREEN);
                g2d.drawLine(xTimeScale.get(i), panelHeight - yPositionDownload.get(i), xTimeScale.get(i + 1), panelHeight - yPositionDownload.get(i + 1));
                g2d.setColor(Color.RED);
                g2d.drawLine(xTimeScale.get(i), panelHeight - yPositionUpload.get(i), xTimeScale.get(i + 1), panelHeight - yPositionUpload.get(i + 1));
            }
            g2d.setColor(Color.WHITE);
            g2d.drawString(Integer.toString(maxValue) + " Kb/s", 2, 10);
            g2d.drawString("0 Kb/s", 3, panelHeight - HEIGTH_OFFSET);
        }
        g2d.setColor(Color.GRAY);
        g2d.drawLine(0, 0, panelWidth, 0);
        g2d.drawLine(0, panelHeight, panelWidth, panelHeight);
        g2d.drawLine(0, 0, 0, panelHeight);
        g2d.drawLine(panelWidth, 0, panelWidth, panelHeight);
    }
}
