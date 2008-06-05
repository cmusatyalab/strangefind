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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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

    private BufferedImage getImg() {
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
            return GraphicsUtilities.toCompatibleImage(img);
        }

        // XXX then try loading from kohinoor
        try {
            System.out.println("loading from kohinoor");
            String image1 = Util.extractString(result.getValue("image-1"));
            String image2 = Util.extractString(result.getValue("image-2"));
            String image3 = Util.extractString(result.getValue("image-3"));

            System.out.println(image1);
            System.out.println(image2);
            System.out.println(image3);

            // load
            img = makeImageFromKohinoor(image1, image2, image3);
            if (img != null) {
                return GraphicsUtilities.toCompatibleImage(img);
            }
        } catch (NullPointerException e) {
            // guess we don't have this either
        }

        // everything failed, try manually
        System.out.println("ImageIO failed, falling back to rgbimage");
        byte data[] = result.getValue("_rgb_image.rgbimage");
        byte tmp[] = result.getValue("_cols.int");

        if (data == null || tmp == null) {
            return null;
        }

        int w = Util.extractInt(tmp);
        tmp = result.getValue("_rows.int");

        if (tmp == null) {
            return null;
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
        return GraphicsUtilities.toCompatibleImage(img);
    }

    private BufferedImage makeImageFromKohinoor(String image1, String image2,
            String image3) {
        try {
            URI uri1 = createKohinoorURI(image1);
            URI uri2 = createKohinoorURI(image2);
            URI uri3 = createKohinoorURI(image3);

            BufferedImage img1 = ImageIO.read(uri1.toURL());
            BufferedImage img2 = ImageIO.read(uri2.toURL());
            BufferedImage img3 = ImageIO.read(uri3.toURL());

            if (img1 != null && img2 != null && img3 != null) {
                int maxValue = Math.max(Math.max(getMaxValue(img1), getMaxValue(img2)), getMaxValue(img3));
                
                DataBufferUShort b1 = (DataBufferUShort) img1.getRaster()
                        .getDataBuffer();
                DataBufferUShort b2 = (DataBufferUShort) img2.getRaster()
                        .getDataBuffer();
                DataBufferUShort b3 = (DataBufferUShort) img3.getRaster()
                        .getDataBuffer();

                DataBuffer b = new DataBufferUShort(new short[][] { b1.getData(),
                        b2.getData(), b3.getData() }, b1.getSize());

                // TODO
                return img2;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getMaxValue(BufferedImage img) {
        DataBuffer db = img.getRaster().getDataBuffer();
        int max = 0;
        for (int i = 0; i < db.getSize(); i++) {
            max = Math.max(max, db.getElem(i));
        }
        System.out.println("max: " + max);
        return max;
    }

    private URI createKohinoorURI(String image1) throws URISyntaxException {
        String imagePath = image1.replace('\\', '/').substring(1);
        URI uri = new URI("http", "kohinoor.diamond.cs.cmu.edu", imagePath,
                null);
        return uri;
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
