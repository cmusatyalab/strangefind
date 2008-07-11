package edu.cmu.cs.diamond.snapfind2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
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

    private BufferedImage img1;

    private BufferedImage img2;

    private BufferedImage img3;

    public AnnotatedResult(Result r, String annotation,
            String tooltipAnnotation, Decorator decorator) {
        theResult = r;
        this.annotation = annotation;
        this.tooltipAnnotation = tooltipAnnotation;
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

                if (img1 != null && img2 != null && img3 != null) {
                    int maxValue = Math.max(Math.max(getMaxValue(img1),
                            getMaxValue(img2)), getMaxValue(img3));

                    DataBufferUShort b1 = (DataBufferUShort) img1.getRaster()
                            .getDataBuffer();
                    DataBufferUShort b2 = (DataBufferUShort) img2.getRaster()
                            .getDataBuffer();
                    DataBufferUShort b3 = (DataBufferUShort) img3.getRaster()
                            .getDataBuffer();

                    DataBuffer b = new DataBufferUShort(new short[][] {
                            b1.getData(), b2.getData(), b3.getData() }, b1
                            .getSize());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new BufferedImage[] { img1, img2, img3 };
    }

    private URI createKohinoorURI(String image1) throws URISyntaxException {
        String imagePath = image1.replace('\\', '/').substring(1);
        URI uri = new URI("http", "kohinoor.diamond.cs.cmu.edu", imagePath,
                null);
        return uri;
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
}
