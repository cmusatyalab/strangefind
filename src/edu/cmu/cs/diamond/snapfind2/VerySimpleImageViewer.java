/*
 *  SnapFind 2, the Java-based Diamond shell
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  SnapFind 2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  SnapFind 2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SnapFind 2. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking SnapFind 2 statically or dynamically with other modules is
 *  making a combined work based on SnapFind 2. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 * 
 *  In addition, as a special exception, the copyright holders of
 *  SnapFind 2 give you permission to combine SnapFind 2 with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for SnapFind 2 and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of SnapFind 2 are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

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

        if (imgs.length > 0) {
            this.imgs = new BufferedImage[imgs.length];
            System.arraycopy(imgs, 0, this.imgs, 0, imgs.length);
        } else {
            this.imgs = new BufferedImage[1];
            this.imgs[0] = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        }

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

        setImage(result, this.imgs[0]);

        image.setPreferredSize(new Dimension(this.imgs[0].getWidth(), this.imgs[0]
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
