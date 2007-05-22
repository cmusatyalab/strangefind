package edu.cmu.cs.diamond.snapfind2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Util;

public class ResultViewer extends JButton implements ActionListener {
    private static final int PREFERRED_HEIGHT = 180;

    private static final int PREFERRED_WIDTH = 240;

    private Result result;

    private Icon thumbnail;

    public ResultViewer() {
        Dimension d = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        setMinimumSize(d);
        setPreferredSize(d);
        setMaximumSize(d);
        
        setEnabled(false);

        addActionListener(this);
    }

    public void setResult(Result r) {
        result = r;

        if (result == null) {
            thumbnail = null;
            setEnabled(false);
            return;
        }

        BufferedImage img = getImg();
        thumbnail = new ImageIcon(Util.possiblyShrinkImage(img,
                PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setEnabled(true);
    }

    public void commitResult() {
        setIcon(thumbnail);
    }
    
    private BufferedImage getImg() {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(result.getData()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (img != null) {
            return img;
        }

        // ImageIO failed, try manually
        byte data[] = result.getValue("_rgb_image.rgbimage");
        byte tmp[] = result.getValue("_cols.int");
        int w = Util.extractInt(tmp);
        tmp = result.getValue("_rows.int");
        int h = Util.extractInt(tmp);

        System.out.println(w + "x" + h);

        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = (y * w + x) * 4;
                // System.out.println(x);
                // System.out.println(y);
                int val = (data[i] & 0xFF) << 16 | (data[i + 1] & 0xFF) << 8
                        | (data[i + 2] & 0xFF);
                img.setRGB(x, y, val);
            }
        }
        return img;
    }

    public void actionPerformed(ActionEvent e) {
        if (result == null) {
            return;
        }

        JLabel p = new JLabel(new ImageIcon(getImg()));
        JFrame f = new JFrame();
        f.add(p);
        f.pack();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setLocationByPlatform(true);
        f.setVisible(true);
    }
}
