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

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.jdesktop.swingx.graphics.GraphicsUtilities;

import edu.cmu.cs.diamond.opendiamond.Util;

public class ResultViewer extends JButton implements ActionListener {
    private static final int PREFERRED_HEIGHT = 200;

    private static final int PREFERRED_WIDTH = 200;

    private volatile AnnotatedResult result;

    private volatile Icon thumbnail;

    public ResultViewer() {
        super();

        setHorizontalTextPosition(CENTER);
        setVerticalTextPosition(BOTTOM);

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

        BufferedImage imgs[] = getImgs();
        BufferedImage img = imgs[0];

        // for xml well data
        if (imgs.length > 1) {
            img = imgs[0]; // suspect
        }
        Insets in = getInsets();

        int w = img.getWidth();
        int h = img.getHeight();

        // calculate 2 lines
        FontMetrics metrics = getFontMetrics(getFont());
        int labelHeight = metrics.getHeight() * 2;
        
        System.out.println(labelHeight);

        double scale = Util.getScaleForResize(w, h, PREFERRED_WIDTH - in.left
                - in.right,
                (PREFERRED_HEIGHT - in.top - in.bottom - labelHeight));
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
        // XXX slow?
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
        new VerySimpleImageViewer(result, getImgs()).setVisible(true);
    }
}
