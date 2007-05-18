package edu.cmu.cs.diamond.anomaly;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import edu.cmu.cs.diamond.opendiamond.*;

public class Test {
    private static final String[] LABELS = { "circle-count",
            "circle-area-fraction", "circle-area-m0", "circle-area-m1",
            "circle-area-m2", "circle-area-m3", "circle-eccentricity-m0",
            "circle-eccentricity-m1", "circle-eccentricity-m2",
            "circle-eccentricity-m3" };

    public static void main(String[] args) {
        // get scopes
        List<Scope> scopes = ScopeSource.getPredefinedScopeList();
        for (Scope scope : scopes) {
            System.out.println(scope);
        }

        // use first scope
        Scope scope = scopes.get(3);

        // set up the filters
        Filter rgb = null;
        Filter circles = null;
        Filter anom = null;
        try {
            FilterCode c = new FilterCode(new FileInputStream(
                    "/opt/diamond/lib/fil_rgb.a"));
            rgb = new Filter("RGB", c, "f_eval_img2rgb", "f_init_img2rgb",
                    "f_fini_img2rgb", 1, new String[0], new String[0], 400);
            System.out.println(rgb);

            c = new FilterCode(new FileInputStream("fil_circle.so"));
            circles = new Filter("circles", c, "f_eval_circles",
                    "f_init_circles", "f_fini_circles", 0,
                    new String[] { "RGB" }, new String[] { "-1", "-1", "0.4",
                            "1" }, 400);
            System.out.println(circles);

            String anomArgs[] = new String[LABELS.length + 2];
            anomArgs[0] = "0";
            anomArgs[1] = "5";
            System.arraycopy(LABELS, 0, anomArgs, 2, LABELS.length);
            c = new FilterCode(new FileInputStream("fil_anomaly.so"));
            anom = new Filter("anomaly", c, "f_eval_afilter", "f_init_afilter",
                    "f_fini_afilter", 100, new String[] { "circles" },
                    anomArgs, 400);
            System.out.println(anom);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // init diamond
        Search search = Search.getSearch();
        search.setScope(scope);

        // make a new searchlet
        Searchlet searchlet = new Searchlet();
        searchlet.addFilter(rgb);
        searchlet.addFilter(circles);
        searchlet.addFilter(anom);
        searchlet.setApplicationDependencies(new String[] { "RGB" });
        search.setSearchlet(searchlet);

        Result r;
        for (int ii = 0; ii < 1; ii++) {
            // begin search
            search.startSearch();

            // read some results
            int count = 0;
            while ((r = search.getNextResult()) != null && count < 10000) {
                processResult(r);

                count++;
            }

            search.stopSearch();
        }
    }

    private static void processResult(Result r) {
        System.out.println(r);

        byte data[] = r.getData();

        try {
            // try reading the data
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            BufferedImage img = ImageIO.read(in);

            img = possiblyShrinkImage(img);

            int count = (int) Util.extractDouble(r.getValue("circle-count"));
            double areaFrac = Util.extractDouble(r
                    .getValue("circle-area-fraction"));
            double aM0 = Util.extractDouble(r.getValue("circle-area-m0"));
            double aM1 = Util.extractDouble(r.getValue("circle-area-m1"));
            double aM2 = Util.extractDouble(r.getValue("circle-area-m2"));
            double aM3 = Util.extractDouble(r.getValue("circle-area-m3"));

            double eM0 = Util.extractDouble(r
                    .getValue("circle-eccentricity-m0"));
            double eM1 = Util.extractDouble(r
                    .getValue("circle-eccentricity-m1"));
            double eM2 = Util.extractDouble(r
                    .getValue("circle-eccentricity-m2"));
            double eM3 = Util.extractDouble(r
                    .getValue("circle-eccentricity-m3"));

            System.out.println("count: " + count);
            System.out.println("aFrac: " + areaFrac);
            System.out.println("aM0:   " + aM0);
            System.out.println("aM1:   " + aM1);
            System.out.println("aM2:   " + aM2);
            System.out.println("aM3:   " + aM3);
            System.out.println("eM0:   " + eM0);
            System.out.println("eM1:   " + eM1);
            System.out.println("eM2:   " + eM2);
            System.out.println("eM3:   " + eM3);
            System.out.println();

            String key = LABELS[Util.extractInt(r
                    .getValue("anomalous-value.int"))];
            String anomStr = "Anomalous value "
                    + key
                    + ": "
                    + Util.extractDouble(r.getValue(key))
                    + "\nobject count: "
                    + Util.extractInt(r.getValue("anomalous-value-count.int"))
                    + "\nmean: "
                    + Util.extractDouble(r
                            .getValue("anomalous-value-mean.double"))
                    + "\nstddev: "
                    + Util.extractDouble(r
                            .getValue("anomalous-value-stddev.double"));
            System.out.println(anomStr);

            JFrame j = new JFrame();
            j.setLocationByPlatform(true);
            j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            j.getContentPane().add(new JLabel(new ImageIcon(img)));
            j.getContentPane().add(new JLabel(anomStr), BorderLayout.SOUTH);
            j.pack();
            j.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage possiblyShrinkImage(BufferedImage img) {
        int h = img.getHeight(null);
        int w = img.getWidth(null);
        int maxSize = 400;

        double scale = 1.0;
        if (h > w) {
            if (h > maxSize) {
                scale = (double) maxSize / h;
            }
        } else {
            if (w > maxSize) {
                scale = (double) maxSize / h;
            }
        }

        if (scale == 1.0) {
            return img;
        } else {
            BufferedImage newI = new BufferedImage((int) (w * scale),
                    (int) (h * scale), img.getType());

            Graphics2D g = newI.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.scale(scale, scale);
            g.drawImage(img, 0, 0, null);
            g.dispose();
            
            return newI;
        }
    }
}