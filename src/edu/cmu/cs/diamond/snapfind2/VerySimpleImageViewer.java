package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXImageView;

public class VerySimpleImageViewer extends JFrame {
    public class ChannelSelector extends JPanel {
        public ChannelSelector() {
            String labels[] = new String[imgs.length];
            for (int i = 0; i < labels.length; i++) {
                if (i == 0) {
                    labels[i] = "Combined Image";
                } else {
                    labels[i] = "Image " + i;
                }
            }

            setLayout(new BorderLayout());
            final JList list = new JList(labels);

            list.setSelectedIndex(0);
            list.getSelectionModel().setSelectionMode(
                    ListSelectionModel.SINGLE_SELECTION);
            list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getFirstIndex() != -1) {
                        setImage(result, imgs[list.getSelectedIndex()]);
                    }
                }
            });
            add(list);
        }
    }

    final private BufferedImage[] imgs;

    final private JXImageView image;

    final private AnnotatedResult result;

    public VerySimpleImageViewer(AnnotatedResult result, BufferedImage imgs[]) {
        this.result = result;
        this.imgs = imgs;
        image = new JXImageView();
        add(image);

        image.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int count = e.getWheelRotation();

                Action a;
                if (count < 0) {
                    a = image.getZoomInAction();
                    count = -count;
                } else {
                    a = image.getZoomOutAction();
                }

                while (count > 0) {
                    a.actionPerformed(new ActionEvent(e.getSource(), e.getID(),
                            "zoom"));
                    count--;
                }
            }
        });
        
        InputMap inputMap = new InputMap();
        inputMap.put(KeyStroke.getKeyStroke("PLUS"), "zoom in");
        inputMap.put(KeyStroke.getKeyStroke("EQUALS"), "zoom in");
        inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoom out");
        
        ActionMap actionMap = new ActionMap();
        actionMap.put("zoom in", image.getZoomInAction());
        actionMap.put("zoom out", image.getZoomOutAction());
        
        InputMap oldInputMap = image.getInputMap();
        ActionMap oldActionMap = image.getActionMap();
        inputMap.setParent(oldInputMap.getParent());
        oldInputMap.setParent(inputMap);
        actionMap.setParent(oldActionMap.getParent());
        oldActionMap.setParent(actionMap);

        
        setImage(result, imgs[0]);

        image.setPreferredSize(new Dimension(imgs[0].getWidth(), imgs[0]
                .getHeight()));

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.add(new JLabel(result.getAnnotation()));
        bottomPanel.add(new ChannelSelector());

        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
    }

    private void setImage(AnnotatedResult result, BufferedImage img) {
        BufferedImage newImage = new BufferedImage(img.getWidth(), img
                .getHeight(), img.getType());
        Graphics2D g = newImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        result.decorate(g, 1.0);
        g.dispose();

        Point2D p = image.getImageLocation();
        double scale = image.getScale();

        image.setImage(newImage);

        image.setImageLocation(p);
        image.setScale(scale);

        validate();
    }
}
