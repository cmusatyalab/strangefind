package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class VerySimpleImageViewer extends JFrame {
    public VerySimpleImageViewer(AnnotatedResult result, BufferedImage imgs[]) {
        BufferedImage img = imgs[0];
        Graphics2D g = img.createGraphics();
        result.decorate(g, 1.0);
        g.dispose();

        JLabel p = new JLabel(new ImageIcon(img));
        JScrollPane jsp = new JScrollPane(p);
        jsp.getVerticalScrollBar().setUnitIncrement(40);
        jsp.getHorizontalScrollBar().setUnitIncrement(40);
        add(jsp);
        add(new JLabel(result.getAnnotation()), BorderLayout.SOUTH);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
    }
}
