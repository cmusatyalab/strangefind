package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class VerySimpleImageViewer extends JFrame {
    public class ChannelSelector extends JPanel {
        public ChannelSelector() {
            String labels[] = new String[imgs.length];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = "Image " + (i + 1);
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

    final private JLabel image;

    final private AnnotatedResult result;

    public VerySimpleImageViewer(AnnotatedResult result, BufferedImage imgs[]) {
        this.result = result;
        this.imgs = imgs;
        image = new JLabel();
        JScrollPane jsp = new JScrollPane(image);
        jsp.getVerticalScrollBar().setUnitIncrement(40);
        jsp.getHorizontalScrollBar().setUnitIncrement(40);
        add(jsp);

        setImage(result, imgs[0]);

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
        image.setIcon(new ImageIcon(newImage));

        validate();
    }
}
