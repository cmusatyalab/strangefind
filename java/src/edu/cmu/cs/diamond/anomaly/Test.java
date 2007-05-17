package edu.cmu.cs.diamond.anomaly;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import edu.cmu.cs.diamond.opendiamond.*;

public class Test {
    public static void main(String[] args) {
        // get scopes
        List<Scope> scopes = ScopeSource.getPredefinedScopeList();
        for (Scope scope : scopes) {
            System.out.println(scope);
        }

        // use first scope
        Scope scope = scopes.get(0);

        // set up the filters
        Filter rgb = null;
        Filter circles = null;
        try {
            FilterCode c = new FilterCode(new FileInputStream(
                    "/opt/diamond/lib/fil_rgb.a"));
            rgb = new Filter("RGB", c, "f_eval_img2rgb",
                    "f_init_img2rgb", "f_fini_img2rgb", 1, new String[0],
                    new String[0], 400);
            System.out.println(rgb);

            c = new FilterCode(new FileInputStream("fil_circle.so"));
            circles = new Filter("circles", c, "f_eval_circles",
                    "f_init_circles", "f_fini_circles", 0,
                    new String[] { "RGB" }, new String[] { "-1", "-1", "0.4",
                            "1" }, 400);
            System.out.println(circles);

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
        searchlet.setApplicationDependencies(new String[] { "RGB" });
        search.setSearchlet(searchlet);

        Result r;
        for (int ii = 0; ii < 1; ii++) {
            // begin search
            search.startSearch();

            // read some results
            int count = 0;
            while ((r = search.getNextResult()) != null && count < 10) {
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

            // else, try the other one
            data = r.getValue("_rgb_image.rgbimage");
            byte tmp[] = r.getValue("_cols.int");
            int w = Util.extractInt(tmp);
            tmp = r.getValue("_rows.int");
            int h = Util.extractInt(tmp);

            System.out.println(w + "x" + h);

            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = (y * w + x) * 4;
                    // System.out.println(x);
                    // System.out.println(y);
                    int val = (data[i] & 0xFF) << 16
                            | (data[i + 1] & 0xFF) << 8 | (data[i + 2] & 0xFF);
                    img.setRGB(x, y, val);
                }
            }

            JFrame j = new JFrame();
            j.setLocationByPlatform(true);
            j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            j.getContentPane().add(new JButton(new ImageIcon(img)));
            j.pack();
            j.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}