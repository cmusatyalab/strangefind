package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.jdesktop.swingx.graphics.GraphicsUtilities;

import edu.cmu.cs.diamond.opendiamond.Util;

public class ResultViewer extends JButton implements ActionListener {
    private static final int PREFERRED_HEIGHT = 180;

    private static final int PREFERRED_WIDTH = 240;

    private volatile AnnotatedResult result;

    private volatile Icon thumbnail;

    public ResultViewer() {
        super();

        Dimension d = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        setMinimumSize(d);
        setPreferredSize(d);
        setMaximumSize(d);

        setEnabled(false);

        addActionListener(this);
    }

    public void setResult(AnnotatedResult r) {
        result = r;

        if (result == null) {
            thumbnail = null;
            return;
        }

        BufferedImage img = getImg();
        Insets in = getInsets();

        int w = img.getWidth();
        int h = img.getHeight();
        double scale = Util.getScaleForResize(w, h, PREFERRED_WIDTH - in.left
                - in.right, PREFERRED_HEIGHT - in.top - in.bottom);
        BufferedImage newImg;

        final int sW = (int) (w * scale);
        final int sH = (int) (h * scale);

        if (scale < 1.0) {
            newImg = GraphicsUtilities.createThumbnail(img, sW, sH);
        } else {
            newImg = Util.scaleImage(img, scale);
        }
        Graphics2D g = newImg.createGraphics();
        result.decorate(g, scale);
        g.dispose();

        thumbnail = new ImageIcon(newImg);
    }

    public void commitResult() {
        if (result == null) {
            setToolTipText(null);
            setIcon(null);
            setEnabled(false);
        } else {
            setToolTipText(result.getTooltipAnnotation());
            setIcon(thumbnail);
            setEnabled(true);
        }
    }

    private BufferedImage getImg() {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(result.getData()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (img != null) {
            return GraphicsUtilities.toCompatibleImage(img);
        }

        // ImageIO failed, try manually
        byte data[] = result.getValue("_rgb_image.rgbimage");
        byte tmp[] = result.getValue("_cols.int");
        int w = Util.extractInt(tmp);
        tmp = result.getValue("_rows.int");
        int h = Util.extractInt(tmp);

        System.out.println(w + "x" + h);

        img = GraphicsUtilities.createCompatibleImage(w, h);

        if (data != null) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = (y * w + x) * 4;
                    // System.out.println(x);
                    // System.out.println(y);
                    int val = (data[i] & 0xFF) << 16
                            | (data[i + 1] & 0xFF) << 8 | (data[i + 2] & 0xFF);
                    img.setRGB(x, y, val);
                }
            }
        }
        return img;
    }

    public void actionPerformed(ActionEvent e) {
        BufferedImage img = getImg();
        Graphics2D g = img.createGraphics();
        result.decorate(g, 1.0);
        g.dispose();

        JLabel p = new JLabel(new ImageIcon(img));
        JScrollPane jsp = new JScrollPane(p);
        JFrame f = new JFrame();
        jsp.getVerticalScrollBar().setUnitIncrement(40);
        jsp.getHorizontalScrollBar().setUnitIncrement(40);
        f.add(jsp);
        f.add(new JLabel(result.getAnnotation()), BorderLayout.SOUTH);

        f.pack();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setLocationByPlatform(true);
        f.setVisible(true);
    }
}
