package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
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

        BufferedImage imgs[] = getImg();
        BufferedImage img = imgs[0];
        if (imgs.length > 1) {
            img = imgs[1];  // TODO(agoode) fix this to show all images, not hack for xml well data
        }
        Insets in = getInsets();

        int w = img.getWidth();
        int h = img.getHeight();
        double scale = Util.getScaleForResize(w, h, PREFERRED_WIDTH - in.left
                - in.right, PREFERRED_HEIGHT - in.top - in.bottom);
        BufferedImage newImg;

        newImg = Util.scaleImage(img, scale);

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

    private BufferedImage[] getImg() {
        // XXX this is messy and needs to be modularized
        BufferedImage img = null;

        // first try ImageIO
        try {
            img = ImageIO.read(new ByteArrayInputStream(result.getData()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (img != null) {
            possiblyNormalize(img);
            return new BufferedImage[] { GraphicsUtilities
                    .toCompatibleImage(img) };
        }

        // XXX then try loading from kohinoor
        try {
            System.out.println("loading from kohinoor");
            // load
            BufferedImage kohinoorImgs[] = result.getKohinoorImages();
            if (kohinoorImgs != null) {
                BufferedImage result[] = new BufferedImage[kohinoorImgs.length];
                for (int i = 0; i < kohinoorImgs.length; i++) {
                    result[i] = GraphicsUtilities
                            .toCompatibleImage(kohinoorImgs[i]);
                }
                return result;
            }
        } catch (NullPointerException e) {
            // guess we don't have this either
        }

        // everything failed, try manually
        System.out.println("ImageIO failed, falling back to rgbimage");
        byte data[] = result.getValue("_rgb_image.rgbimage");
        byte tmp[] = result.getValue("_cols.int");

        if (data == null || tmp == null) {
            return new BufferedImage[0];
        }

        int w = Util.extractInt(tmp);
        tmp = result.getValue("_rows.int");

        if (tmp == null) {
            return new BufferedImage[0];
        }

        int h = Util.extractInt(tmp);

        System.out.println(w + "x" + h);

        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        if (data != null) {
            // XXX slow?
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
        return new BufferedImage[] { GraphicsUtilities.toCompatibleImage(img) };
    }

    private void possiblyNormalize(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_USHORT_GRAY) {
            // XXX better test above (sample models?)
            normalize(img);
        }
    }

    private void normalize(BufferedImage img) {
        System.out.println("Normalising");

        // XXX: hah
        RescaleOp r = new RescaleOp(32, 0, null);
        r.filter(img, img);
    }

    public void actionPerformed(ActionEvent e) {
        BufferedImage img = getImg()[0];  // TODO(agoode)
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
