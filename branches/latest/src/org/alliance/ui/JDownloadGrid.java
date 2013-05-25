package org.alliance.ui;

import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.comm.filetransfers.Download;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-15
 * Time: 16:49:43
 */
public class JDownloadGrid extends JComponent {

    private int BLOCK_WIDTH = 8, BLOCK_HEIGHT = 8;
    private Download download;

    public JDownloadGrid() {
    }

    public void setDownload(Download download) {
        this.download = download;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 40);
    }

    @Override
    public synchronized void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (download == null) {
            return;
        }

        int gridWidth = getWidth() / BLOCK_WIDTH;
        int gridHeight = getHeight() / BLOCK_HEIGHT;
        int nBlocks = gridWidth * gridHeight;
        if (nBlocks == 0) {
            return;
        }
        int bytesPerBlock = (int) (download.getFd() == null ? 0 : download.getFd().getSize() / nBlocks);

        long fileOffset = 0;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                double d = getPercentCompleteForRange(fileOffset, fileOffset + bytesPerBlock);
                g.setColor(new Color(interpolate(0x5a, 0x98, d), interpolate(0x8a, 0xbd, d), interpolate(0xbd, 0xe6, d)));
                g.fillRect(x * BLOCK_WIDTH, y * BLOCK_HEIGHT, BLOCK_WIDTH - 1, BLOCK_HEIGHT - 1);
                fileOffset += bytesPerBlock;
            }
        }
    }

    private int interpolate(int a, int b, double v) {
        int c = (int) (a + (b - a) * v);
        if (c < 0) {
            c = 0;
        }
        if (c >= 256) {
            c = 255;
        }
        return c;
    }

    private double getPercentCompleteForRange(long from, long to) {
        if (download.isComplete()) {
            return 1;
        }
        BlockFile bf;
        bf = download.getStorage().getCachedBlockFile(download.getRoot());
        if (bf == null) {
            return 0;
        }

        int fromBlock = (int) (from / BLOCK_SIZE);
        int toBlock = (int) (to / BLOCK_SIZE);
        int bytesComplete = 0;
        for (int i = fromBlock; i <= toBlock; i++) {
            int c = bf.getBytesCompleteForBlock(i);
            int f = (int) (from - i * BLOCK_SIZE);
            if (f < 0) {
                f = 0;
            }
            c -= f;
            if (c < 0) {
                continue;
            }
            if (to < i * BLOCK_SIZE + c + f) {
                c = (int) (to - i * BLOCK_SIZE);
                c -= f;
            }
            if (c < 0) {
                continue;
            }
            bytesComplete += c;
        }

        return (0. + bytesComplete) / (to - from);
    }
}
