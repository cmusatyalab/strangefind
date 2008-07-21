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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.imageio.ImageIO;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Util;

public class AnnotatedResult extends Result {
    final private Result theResult;

    final private String annotation;

    final private Decorator decorator;

    final private String tooltipAnnotation;
    
    final private String oneLineAnnotation;

    private BufferedImage img1;

    private BufferedImage img2;

    private BufferedImage img3;

    private BufferedImage combinedImage;

    public AnnotatedResult(Result r, String annotation, String oneLineAnnotation,
            String tooltipAnnotation, Decorator decorator) {
        theResult = r;
        this.annotation = annotation;
        this.tooltipAnnotation = tooltipAnnotation;
        this.oneLineAnnotation = oneLineAnnotation;
        this.decorator = decorator;
    }

    @Override
    public byte[] getData() {
        return theResult.getData();
    }

    @Override
    public List<String> getKeys() {
        return theResult.getKeys();
    }

    @Override
    public byte[] getValue(String key) {
        return theResult.getValue(key);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void decorate(Graphics2D g, double scale) {
        if (decorator != null) {
            decorator.decorate(this, g, scale);
        }
    }

    public String getTooltipAnnotation() {
        return tooltipAnnotation;
    }

    @Override
    public String getServerName() {
        return theResult.getServerName();
    }

    @Override
    public String getObjectName() {
        return theResult.getObjectName();
    }

    public BufferedImage[] getKohinoorImages() {
        String image1 = Util.extractString(getValue("image-1"));
        String image2 = Util.extractString(getValue("image-2"));
        String image3 = Util.extractString(getValue("image-3"));

        System.out.println(image1);
        System.out.println(image2);
        System.out.println(image3);

        if (img1 == null || img2 == null || img3 == null) {
            try {
                URI uri1 = createKohinoorURI(image1);
                URI uri2 = createKohinoorURI(image2);
                URI uri3 = createKohinoorURI(image3);

                img1 = ImageIO.read(uri1.toURL());
                img2 = ImageIO.read(uri2.toURL());
                img3 = ImageIO.read(uri3.toURL());

                combinedImage = combineImage(img1, img2, img3);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new BufferedImage[] { combinedImage, img1, img2, img3 };
    }

    static private BufferedImage combineImage(BufferedImage img1,
            BufferedImage img2, BufferedImage img3) {
        DataBufferUShort b1 = (DataBufferUShort) img1.getRaster()
                .getDataBuffer();
        DataBufferUShort b2 = (DataBufferUShort) img2.getRaster()
                .getDataBuffer();
        DataBufferUShort b3 = (DataBufferUShort) img3.getRaster()
                .getDataBuffer();

        short[] data1 = b1.getData();
        short[] data2 = b2.getData();
        short[] data3 = b3.getData();

        final int w = img1.getWidth();
        final int h = img1.getHeight();

        BufferedImage result = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        // XXX slow?
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int val = ((data2[i] >> 8) & 0xFF) << 16
                        | ((data1[i] >> 8) & 0xFF) << 8
                        | ((data3[i] >> 8) & 0xFF);
                result.setRGB(x, y, val);
            }
        }

        return result;
    }

    static private URI createKohinoorURI(String image1)
            throws URISyntaxException {
        String imagePath = image1.replace('\\', '/').substring(1);
        URI uri = new URI("http", "kohinoor.diamond.cs.cmu.edu", imagePath,
                null);
        return uri;
    }

    public String getOneLineAnnotation() {
        return oneLineAnnotation;
    }
}
