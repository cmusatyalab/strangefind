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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.jdesktop.swingx.graphics.GraphicsUtilities;

import edu.cmu.cs.diamond.opendiamond.Search;
import edu.cmu.cs.diamond.opendiamond.Util;

public class ResultViewer extends JButton implements ActionListener {
    private static final int PREFERRED_HEIGHT = 200;

    private static final int PREFERRED_WIDTH = 200;

    private volatile AnnotatedResult result;

    private volatile Search search;

    private volatile Icon thumbnail;

    public ResultViewer() {
        super();

        setHorizontalTextPosition(CENTER);
        setVerticalTextPosition(BOTTOM);

        Dimension d = new Dimension(getPreferredWidth(), getPreferredHeight());
        setMinimumSize(d);
        setPreferredSize(d);
        setMaximumSize(d);

        setEnabled(false);

        addActionListener(this);
    }

    public void setResult(AnnotatedResult r, Search s) {
        result = r;
        search = s;

        if (result == null) {
            thumbnail = null;
            return;
        }

        BufferedImage img = getImageForThumbnail();

        if (img == null) {
            img = new BufferedImage(getPreferredWidth(), getPreferredHeight(),
                    BufferedImage.TYPE_INT_RGB);
        }

        Insets in = getInsets();

        int w = img.getWidth();
        int h = img.getHeight();

        // calculate 2 lines
        FontMetrics metrics = getFontMetrics(getFont());
        int labelHeight = metrics.getHeight() * 2;

        System.out.println(labelHeight);

        double scale = Util.getScaleForResize(w, h, getPreferredWidth()
                - in.left - in.right, (getPreferredHeight() - in.top
                - in.bottom - labelHeight));
        BufferedImage newImg;

        newImg = Util.scaleImage(img, scale);

        Graphics2D g = newImg.createGraphics();
        result.decorate(g, scale);
        g.dispose();

        thumbnail = new ImageIcon(newImg);
    }

    private BufferedImage getImageForThumbnail() {
        BufferedImage img = null;

        // first try thumbnail (with ImageIO)
        try {
            img = ImageIO.read(new ByteArrayInputStream(result
                    .getValue("thumbnail.jpeg")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (img != null) {
            return img;
        }

        // next, fallback
        BufferedImage imgs[] = getImgs();
        if (imgs.length > 0) {
            img = imgs[0];
        }

        return img;
    }

    public void commitResult() {
        if (result == null) {
            setToolTipText(null);
            setText(null);
            setIcon(null);
            setEnabled(false);
        } else {
            setToolTipText(result.getTooltipAnnotation());
            setText(result.getOneLineAnnotation());
            setIcon(thumbnail);
            setEnabled(true);
        }
    }

    private BufferedImage[] getImgs() {
        // XXX this is messy and needs to be modularized
        BufferedImage img = null;

        // first try data (with ImageIO)
        try {
            byte data[] = result.getData();
            if (data.length == 0) {
                // refetch
                search
                        .reevaluateResult(result.getResult(),
                                new HashSet<String>(Arrays
                                        .asList(new String[] { "" })));
            }
            img = ImageIO.read(new ByteArrayInputStream(result.getData()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (img != null) {
            possiblyNormalize(img);
            return new BufferedImage[] { GraphicsUtilities
                    .toCompatibleImage(img) };
        }

        // then try loading from image server
        try {
            System.out.println("loading from image host");
            // load
            BufferedImage serverImgs[] = result.getImagesByHTTP();
            if (serverImgs != null) {
                BufferedImage result[] = new BufferedImage[serverImgs.length];
                for (int i = 0; i < serverImgs.length; i++) {
                    result[i] = GraphicsUtilities
                            .toCompatibleImage(serverImgs[i]);
                }
                return result;
            }
        } catch (NullPointerException e) {
            // guess we don't have this either
        }

        // everything failed
        return new BufferedImage[0];
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
        new VerySimpleImageViewer(result, getImgs()).setVisible(true);
    }

    public static int getPreferredWidth() {
        return PREFERRED_WIDTH;
    }

    public static int getPreferredHeight() {
        return PREFERRED_HEIGHT;
    }
}
